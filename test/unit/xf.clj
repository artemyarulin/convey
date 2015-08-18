(ns unit.xf
  (:use clojure.test)
  (:require [convey.xf :refer [t-do t-zip t-err]]
            [clojure.core.async :as async :refer [go <!]]))

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
