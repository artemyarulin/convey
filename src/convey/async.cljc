(ns convey.async
  #?(:clj (:require [clojure.core.async :refer [go]])
     :cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [#?(:clj  clojure.core.async
                :cljs cljs.core.async) :as async :refer [<! chan close! put! to-chan]]))

(defn pipe                              
  "Pipes values from the channel using transducer"
  [ch xt]
  (async/pipe ch (chan 1 xt)))


(defn <<< 
  "Converts callback function to a channel. Callback can be called only once, as a channel get closed right after"
  [f & args]
  (let [c (chan)]
    (try (apply f (concat args [(fn [x]
                                  (when-not (or (nil? x) #?(:cljs (undefined? x)))
                                    (put! c x))
                                  (close! c))]))
         (catch #?(:clj Exception :cljs js/Error) e (put! c e)))
    c))

(defn flatmap
  "Takes a channel of source channels and returns a channel which
  contains all values taken from them.  The channel will close after all
  the source channels have closed."
  ([ch] (flatmap ch nil))
  ([ch xf]
   (let [out (async/chan 1 xf)]
     (go (loop [cs [ch]]
       (if-not (empty? cs)
         (let [[v c] (async/alts! cs)]
           (cond (nil? v) (recur (filterv #(not= c %) cs))
                 (= c ch) (recur (conj cs v)) 
                 :else (do (async/>! out v)
                           (recur cs))))
         (async/close! out))))
     out)))

#?(:clj
(defmacro <?
  "Waits for value from the channel and throws if it is js/Error
  Accept additionally on-error and on-success handlers (could be simple values) which would be called. 
  :undefined is used as a no-value, because nil is accepted value"
  ([ch] `(<? ~ch :undefined))
  ([ch on-err] `(<? ~ch ~on-err :undefined))
  ([ch on-err on-suc]
   `(let [err-handler# (if (fn? ~on-err) ~on-err (constantly ~on-err))
          suc-handler# (if (fn? ~on-suc) ~on-suc (constantly ~on-suc))
          res# (~'<! ~ch)
          res-is-error# (instance? js/Error res#)]
      (if res-is-error#
        (if (= :undefined ~on-err)
          (throw res#)
          (err-handler# res#))
        (if (= :undefined ~on-suc)
          res#
          (suc-handler# res#)))))))
