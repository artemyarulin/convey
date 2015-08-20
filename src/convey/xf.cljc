(ns convey.xf
  (:require [#?(:clj  clojure.core.async.impl.protocols
                :cljs cljs.core.async.impl.protocols) :refer [add! Buffer]]))

(defn t-do
  "Returns transducer which would execute function parameter as a side affect"
  [f]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (do (f input)
           (rf result input))))))

(defn t-zip
  "Returns transducer which would map key to value"
  [keys]
  (fn [rf]
    (let [idx (atom -1)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (do (swap! idx inc)
             (rf result {(nth keys @idx) input})))))))

(def t-err
  "Returns transducers which in case of error value would stop futher processing
  and send clean unprocessed error value to the output core.async buffer"
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (if (instance? #?(:clj Exception :cljs js/Error) input)
         (do
           (when-not (satisfies? Buffer result)
             (throw (#?(:clj Exception. :cljs js/Error.) "t-err works with core.async only")))
           (add! result input)
           (reduced result))
         (rf result input))))))
