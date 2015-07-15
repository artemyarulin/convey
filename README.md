# rp-transducers
A set of additional transducers inspired by reactive programming (specifically RxJS). Support Clojure and ClojureScript

# Why

Transducers is a very powerfull concept and combined with core.async it can give you the same power as using libraries like RxJS. Standard library contains most of the transducers you may need (filter, map, mapcat, etc.) but there are couple of things that are missing:

- `t-do` - For side affects, useful for logging purposes
- `t-zip` - Same as zipmap
- `t-err` - Easy error handling

# Example

```clojure
;; Returns [0 1 2] and logs numbers to console at the same time
(into [] (t-do print) (range 3)) 

;; Returns [{:a 0} {:b 1} {:c 2}]
(into [] (t-zip [:a :b :c]) (range 3))
```

# Transducers and core.async error handling

core.async doesn't provide any solution for error handling, it is up to developer to decide how it can be resolved. You may read an article of David Nolen about it [Asynchronous Error Handling](http://swannodette.github.io/2013/08/31/asynchronous-error-handling/). He proposes one small `<?` macro which allows you to utilize `try catch`:

``` clojure
(go (try (<? do-staff-async)
    (catch js/Error e (handle-error e))))
```

It works just fine and allows you to separate your error handling from executing code, but problem arise when you attach transducers to the channel:

```clojure
(def in (async/to-chan [1 2 (js/Error. "Err")]))
(def out (async/chan))

(async/pipeline 1 out (map inc) in)

(go
 (try
     (.log js/console (<? out)) ;; 2
     (.log js/console (<? out)) ;; 3
     (.log js/console (<? out)) ;; "Error: Err1" WTF?!1
 (catch js/Error e
     (.error js/console e))) 
```

It happend because transducers are executing **before** the value would be send to the channel. Let see how it handled in RxJS:

```js
Rx.Observable.concat(Rx.Observable.fromArray([1,2]),
                     Rx.Observable.throwException(new Error("Err")))
    .map(v => v + 1)
    .subscribe(v => console.log(v),
               e => console.error(e))
```

RxJS has a concept of error which would avoid any attached process steps and goes directly to subscriber error handler (or throws if there is no such). In order to do the same we have to somehow avoid attached transducers and send `Error` value directly to the channel.

There is one solution for that - we can stop thansducers chain using `reduced` value and at the same time send a value directly to core.async buffer using standard `add!` function. And now core.async error handling with transducers is complete.