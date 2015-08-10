(ns rp-transducers.async
  (:require [#?(:clj  clojure.core.async
                :cljs cljs.core.async)
             :as async
             :refer [<! chan pipeline go close! put!]]))

(defn flatmap
  "Receiving channel of channel unpack all the values and return new channel with 
  merged all the values and applied transducer"
  [ch xf]
  (let [out (chan)
        vals (async/into [] ch)]
    (go (pipeline 1 out xf (async/merge (<! vals))))
    out))

(defn pipe
  "Pipes values from the channel using transducer"
  [ch xt]
  (let [out (chan)]
    (pipeline 1 out xt ch)
    out))

(defn <<< 
  "Converts callback function to a channel"
  [f & args]
  (let [c (chan)]
    (try (apply f (concat args [(fn [x]
                                  (if (or (nil? x) #?(:cljs (undefined? x)))
                                    (close! c)
                                    (put! c x)))]))
         (catch #?(:clj Exception :cljs js/Error) e (put! c e)))
    c))

(defmacro <?
  "Waits for value from the channel and throws if it is js/Error or Exception. 
  Accept additionally on-error and on-success handlers (could be simple values) which would be called. 
  :undefined is used as a no-value, because nil is accepted value"
  ([ch] `(<? ~ch :undefined))
  ([ch on-err] `(<? ~ch ~on-err :undefined))
  ([ch on-err on-suc]
   `(let [err-handler# (if (fn? ~on-err) ~on-err (constantly ~on-err))
          suc-handler# (if (fn? ~on-suc) ~on-suc (constantly ~on-suc))
          res# (~'async/<! ~ch)
          res-is-error# (instance? #?(:clj Exception :cljs js/Error) res#)]
      (if res-is-error#
        (if (= :undefined ~on-err)
          (throw res#)
          (err-handler# res#))
        (if (= :undefined ~on-suc)
          res#
          (suc-handler# res#))))))
