(ns convey.core
  #?(:clj (:require [clojure.core.async :refer [go-loop]])
     :cljs (:require-macros [cljs.core.async.macros :refer [go-loop]]))
  (:require [#?(:clj  clojure.core.async
                :cljs cljs.core.async) :as async :refer [chan <!]]))

(defn convey [ch & args]
  (loop [prev-out ch
         steps args]
    (if (empty? steps)
      prev-out
      (let [[next-in next-out] (first steps)]
        (async/pipe prev-out next-in)
        (recur next-out (rest steps))))))

(defn <|
  ([xf]
   (let [in (chan)
         out (chan 1 xf)]
     (go-loop []
       (let [in-v (<! in)]
         (if (nil? in-v)
           (async/close! out)
           (do
             (if-not (async/put! out in-v)
               (async/close! in)
               (recur))))))
     [in out]))
  ([f-xf f-v]
   (let [in (chan)
         out (chan 1 (comp (f-xf ::ch-data)
                           (map #(or (::in-v %) %))))]
     (async/go-loop []
       (let [in-v (<! in)]
         (if (nil? in-v)
           (async/close! out)
           (let [ch-v (f-v in-v)]
             (loop []
               (when-let [ch-data (<! ch-v)]
                 (async/put! out {::in-v in-v
                                  ::ch-data ch-data})
                 (recur)))
             (recur)))))
     [in out])))
