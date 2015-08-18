# convey [![Circle CI](https://circleci.com/gh/artemyarulin/rp-transducers.svg?style=svg)](https://circleci.com/gh/artemyarulin/rp-transducers) 

[![Clojars Project](http://clojars.org/rp-transducers/latest-version.svg)](http://clojars.org/rp-transducers)

Reactive programming implemented using transducers. Supports Clojure and ClojureScript

``` clojure
(def folders
    (async/to-chan ["folder1" "folder2"]))
(defn is-exists [abs-path]
    (async/to-chan [(= abs-path "/tmp/folder1")]))
(defn ls [abs-path] 
    (async/to-chan [(map (partial str abs-path "/file") (range 10))]))

(convey folders
        (<| map (partial str "/tmp/"))
        (<| filter is-exists)
        (<| map ls)
        (<| take 3)
        (<| (t-do println)))

;; Output: /tmp/folder1/file0
;;         /tmp/folder1/file1
;;         /tmp/folder1/file2
```





