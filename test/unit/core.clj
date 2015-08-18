(ns unit.core
  (:use clojure.test)
  (:require [convey.core :refer [convey <|]]
            [convey.xf :refer [t-do]]
            [clojure.core.async :as async :refer [go <!! <! to-chan]]))

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
                    (<| (take 3))
                    (<| (t-do println)))]
    (<!!
     (go (is (= ["/tmp/folder1/file0"
                 "/tmp/folder1/file1"
                 "/tmp/folder1/file2"]
                (<! (async/into [] out))))))))
