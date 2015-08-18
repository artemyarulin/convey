(defn convey
  ([xf]
   (println "New channel")
   (let [c (chan 1 xf)]
     (fn[v] 
       (async/put! c v)
       c))))



(dump (flow (to-chan (range 5))
            (convey ls)
            (convey cat-all)))

(pipe (to-chan (range 5))
      c)



(defn flow [ch & args]
  (loop [prev-out ch
         steps args]
    (if (empty? steps)
      prev-out
      (let [[next-in next-out] (first steps)]
        (async/pipe prev-out next-in)
        (recur next-out (rest steps))))))

(defn ls [f]
  (to-chan (repeat 3 f)))

(def c (chan))
(dump (flow (to-chan (range 3))
            (convey (filter even?))
            (convey (map inc))
            (convey (map inc))
            (convey filter exists)
            (convey map ls)
            (convey (take 1))))






(defn get-files [v] (to-chan ["file1" "file2" "file3"]))
(defn exists [v] (to-chan [(not= v "file3")]))
(defn ls [v] (to-chan [".." (str "./" v)]))

(dump (flow (get-files nil)
            (convey filter exists)
            (convey map ls)
            (convey (filter (partial not= "..")))
            (convey (map #(str % ".zip")))))




(defn convey
  ([xf]
   (let [in (chan)
         out (chan 1 xf)]
     (async/go-loop []
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


;; xf
;; async
;; statefull xf


(def in (to-chan (range 10)))
(def out (convey ls))

(async/pipe in (first out))

(dump (second out))

(def f (convey (take 2)))

(go (println (<! (f 0))))

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

;; Flow base cases
(flow get-async   ;; ch
      save-async) ;; (convey (map save-async)) + (convey 

(flow get-async
      sync-transducer) ;; c

(flow get-async
      async-transducer)

(flow get-async
      statefull-sync-transducer)

(flow get-async
      statefull-async-transducer)

(defn dump2 [ch]
  (go
    (println "Dump:\n" (<! (async/into [] ch)))))

(defn dump [ch]
  (println "DUMP: ")
  (async/go-loop []
    (let [v (<! ch)]
      (if (nil? v)
        (println "end.")
        (do
          (println "  " v)
          (recur))))))

           



(filter exists?) ->

(let [f# (fn[v] v)
      (chan 1 (filter f#))
