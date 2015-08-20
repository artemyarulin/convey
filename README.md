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
        (<| (map (partial str "/tmp/")))
        (<| filter is-exists)
        (<| map ls)
        (<| (take 3))
        (<| (t-do println)))

;; Output: /tmp/folder1/file0
;;         /tmp/folder1/file1
;;         /tmp/folder1/file2
```

# Main features

- Sync and async(!) transducers are supported
- Error handling - whenever there is an `Exception` or `js/Error` it goes directly to the output channel, no need to handle it in your transducers
- The library is small - most of the staff are handled by core.async and transducers

# How does it work?

Main moving part is step function `<|`: It has 2 different arities:

- `[xf]` - just a simple, sync transducer like `(map inc)`, `(filter even?)`. Fell free to use statefull transducers, `comp` multiple transducers together, etc.
- `[f-xf f-v]` - A workaround to support async transducers. Transducers by default are sync and you cannot do anything about it (see [this investigaton for example](http://grokbase.com/t/gg/clojure/149nsmjpg1/transducers-and-async-operations)). The trick here is to split for example `(filter async-filter-operation)` into two separate concepts: `f-xf` - function which returns transducer, `f-x` - function which returns value. Later on we could utilize values from both of these functions in a right moment.

`<|` returns two channels - `input` and `output`. Function `convey` just builds a workflow and connects output of one to the input of another.

# Roadmap

## 1.0

- [x] Sync transducers
- [x] Async transducers
- [x] Stateful transducers
- [x] Error handling
- [x] More tests
- [x] Readme

## 1.1

- [ ] Refactor `<|` as it is f**king ugly
- [ ] Handling exceptions in steps
- [ ] Makes test compatable with ClojureScript
- [ ] Performance (compare with RxJS)
- [ ] Leaks




