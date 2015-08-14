(ns rp-transducers.core
  #?(:clj (:require [clojure.core.async :refer [go]])
     :cljs (:require-macros [cljs.core.async.macros :refer [go]]))
  (:require [#?(:clj  clojure.core.async.impl.protocols
                :cljs cljs.core.async.impl.protocols) :refer [add! Buffer]])
  (:require [#?(:clj  clojure.core.async
                :cljs cljs.core.async) :as async :refer [<! chan pipeline close! put! to-chan]]))

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

(defn pipe
  "Pipes values from the channel using transducer"
  [ch xt]
  (let [out (chan)]
    (pipeline 1 out xt ch)
    out))

(defn flatmap
  "Takes a *channel* of source channels and returns a channel which
  contains all values taken from them.  The channel will close after all
  the source channels have closed."
  ([ch] (flatmap ch nil))
  ([ch xf]
   (let [out (async/chan 1 xf)]
     (async/go-loop [cs [ch]]
       (if-not (empty? cs)
         (let [[v c] (async/alts! cs)]
           (cond (nil? v) (recur (filterv #(not= c %) cs))
                 (= c ch) (recur (conj cs v)) 
                 :else (do (async/>! out v)
                           (recur cs))))
         (async/close! out)))
     out)))

(defn flow [ch & args]
  (loop [steps args
         from ch]
    (if (empty? steps)
      from
      (let [c (chan 1 (comp (t-err
                             (map (first steps)))))]
        (async/pipe from c)
        (recur (rest steps) (flatmap c))))))

(defn convey
  ([xf]
   (fn[v]
     (pipe (to-chan [v]) (comp t-err xf))))
  ([f-xf f-ch]
   (fn [v]
     (flow (f-ch v) 
           #(pipe (to-chan [v]) (comp t-err
                                      (f-xf (constantly %))))))))



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
