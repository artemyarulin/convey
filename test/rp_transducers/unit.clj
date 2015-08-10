(ns rp-transducers.unit
  (:use clojure.test)
  (:require [rp-transducers.core :refer [t-do t-zip t-err]]
            [rp-transducers.async :refer [flatmap pipe <<< <?]]
            [clojure.core.async :as async :refer [go]]))

(deftest t-zip-case
  (is (= [{:a 1}] (into [] (t-zip [:a]) [1]))))

(deftest t-do-case
  (let [c (atom 0)
        f #(swap! c + %)]
    (is (= [1 2 3] (into [] (comp (t-do f) (map inc)) (range 3))))
    (is (= 3 @c))))

(deftest t-err-case
  (let [in (async/to-chan [1 2 (Exception. "Err")])
        out (async/chan)]
    (async/pipeline 1 out (comp t-err (map inc)) in)
    (go
      (is (= 2 (async/<! out)))
      (is (= 3 (async/<! out)))
      (is (instance? Exception (async/<! out))))))

(deftest t-err-unsupported-checks
  (try (into [] t-err [range 3])
       (throw (Exception. "Should be never reached"))
       (catch Exception e)))

(deftest flatmap-case
  (let [c1 (async/to-chan [10 20])
        c2 (async/to-chan [1 2 3])
        c (flatmap (async/to-chan [c1 c2]) (map inc))]
    (async/<!! (go (is (= 41 (async/<! (async/reduce + 0 c))))))))

(deftest pipe-case
  (let [c1 (async/to-chan (range 3))
        c2 (pipe c1 (map inc))
        c3 (async/into [] c2)]
    (async/<!! (go (is (= [1 2 3] (async/<! c3)))))))

(deftest <<<-callback
  (let [f (fn[v cb] (cb v))]
    (async/<!! (go (is (= true (async/<! (<<< f true))))))))

(deftest <<<-throw
  (let [f (fn[v cb] (throw (Exception. "Error")))]
    (async/<!! (go (is (instance? Exception (async/<! (<<< f true))))))))

(deftest <<<-error
  (let [f (fn[v cb] (cb (Exception. "Error")))]
    (async/<!! (go (is (instance? Exception (async/<! (<<< f true))))))))

(deftest <?-normal
  (let [f (fn[v cb] (cb v))]
    (async/<!! (go  (is (= true (<? (<<< f true))))))))

(deftest <?-normal-with-alt
  (let [f (fn[v cb] (cb v))]
    (async/<!! (go (is (= true (<? (<<< f true) false)))))))

(deftest <?-error
  (let [f (fn[v cb] (throw (Exception. "Error")))]
    (async/<!! (go
      (try 
        (<? (<<< f true))
        (catch Exception e 
          (is (instance? Exception e))))))))

(deftest <?-err-with-alt
  (let [f (fn[v cb] (throw (Exception. "Error")))]
    (async/<!! (go (is (= true (<? (<<< f true) true)))))))

(deftest <?-err-succ
  (let [fe (fn[v cb] (cb (Exception. "Error")))
        fs (fn[v cb] (cb 42))]
    (async/<!! (go
      (is (= true (<? (<<< fe true) true false)))
      (is (= 0 (<? (<<< fe true) (constantly 0))))
      (is (= 42 (<? (<<< fs true) true)))
      (is (= true (<? (<<< fs true) false true)))
      (is (= 0 (<? (<<< fs true) false #(- 42 %))))))))

(deftest <?-nil
  (let [f (fn[v cb] (cb v))]
    (async/<!! (go
      (is (= nil (<? (<<< f nil))))
      (is (= nil (<? (<<< f (Exception. "Error")) nil)))
      (is (= nil (<? (<<< f (Exception. "Error")) nil nil)))
      (is (= nil (<? (<<< f 42) nil nil)))))))
