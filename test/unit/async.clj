(ns unit.async
  (:use clojure.test)
  (:require [convey.async :refer [flatmap pipe <<<]]
            [clojure.core.async :as async :refer [go <!]]))

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
