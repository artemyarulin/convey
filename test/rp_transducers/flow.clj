(ns rp-transducers.flow
  (:use clojure.test)
  (:require [rp-transducers.core :refer [flow convey]]
            [clojure.core.async :as async :refer [<! <!! to-chan go <!]]))

(deftest flow-base-case
  (let [async-mult #(to-chan [(* % 10)])
        async-more-70 #(to-chan [(> % 70)])
        out (flow (to-chan (range 10))
                  (convey (filter even?))
                  async-mult
                  (convey filter async-more-70))]
    (<!!
     (go (is (= [80] (<! (async/into [] out))))))))






