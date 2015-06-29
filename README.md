# rp-transducers
A set of additional transducers like `do`, `do-error`, `zip` inspired by RxJS

# Why

Transducers is very powerfull concept and combined with core.async it can give you the same power as using libraries like RxJS. Standard library contains most of the transducers you may need (filter, map, mapcat, etc.) but there are couple of things that are missing:

- `do` - Handy function for side affects, useful for logging purposes
- `on-error` - For handling errors
- `zip` - Same as zipmap

# Example
``` clojure
(ns cljs.user (:require [rp-transducers.core :refer [t-do t-do-error t-zip]]))

(def t-log (t-do #(.log js/console %)))

;; Returns [0 1 2] and logs numbers to console at the same time
(into [] t-log (range 3)) 

;; Returns [{:a 0} {:b 1} {:c 2}]
(into [] (t-zip [:a :b :c]) (range 3)) 

;; Returns [0 1], logs [0 1] and errors to console "Oops". 4 woudn't be processed
(into [] (comp t-log (t-on-error #(.error js/conole %))) [0 1 (js/Error. "Oops") 4])

```
