(ns unit.core
  (:use clojure.test)
  (:require [convey.core :refer [convey <|]]
            [convey.xf :refer [t-do]]
            [clojure.core.async :as async :refer [go <!! <! to-chan]]))

(def err (Exception. "Error"))
(deftest convey-base-case
  (let [async-mult #(to-chan [(* % 10)])
        async-more-70 #(to-chan [(> % 70)])
        out (convey (to-chan (range 10))
                    (<| (filter even?))
                    (<| map async-mult)
                    (<| filter async-more-70))]
    (<!!
     (go (is (= [80] (<! (async/into [] out))))))))


(deftest convey-base-stateful-xf
  (let [out (convey (to-chan (range 20))
                    (<| (take 1)))]
    (<!!
     (go (is (= 1 (count (<! (async/into [] out)))))))))

(deftest convey-readme-example
  (let [folders (async/to-chan ["folder1" "folder2"])
        is-exists (fn[abs-path] (async/to-chan [(= abs-path "/tmp/folder1")]))
        ls (fn[abs-path](async/to-chan (map (partial str abs-path "/file") (range 10))))
        out (convey folders
                    (<| (map (partial str "/tmp/")))
                    (<| filter is-exists)
                    (<| map ls)
                    (<| (take 3)))]
    (<!!
     (go (is (= ["/tmp/folder1/file0"
                 "/tmp/folder1/file1"
                 "/tmp/folder1/file2"]
                (<! (async/into [] out))))))))

(deftest convey-error-in-sync
  (let [out (convey (to-chan [err])
                    (<| (map *)))]
    (<!!
     (go (is (= err (<! out)))))))

(deftest convey-error-in-async
  (let [f (fn[v](to-chan ["value"]))
        out (convey (to-chan [err])
                    (<| (map f)))]
    (<!!
     (go (is (= err (<! out)))))))

(deftest convey-error-step-sync
  (let [out (convey (to-chan (range 3))
                    (<| (map (constantly err)))
                    (<| (map *))
                    (<| (take 1)))]
    (<!!
     (go (is (= [err] (<! (async/into [] out))))))))

(deftest convey-error-step-async
  (let [f-ae (fn[_] (to-chan (repeat 3 err)))
        out (convey (to-chan (range 3))
                    (<| map f-ae)
                    (<| map *)
                    (<| (take 1)))]
    (<!!
     (go (is (= [err] (<! (async/into [] out))))))))
                    
(deftest convey-empty-step-async
  (let [f #(if (< % 2)
             (to-chan [%])
             (let [c (async/chan)]
               (async/close! c)
               c))
        out (convey (to-chan (range 3))
                    (<| map f))]
    (<!!
     (go (is (= [0 1] (<! (async/into [] out))))))))
                    
