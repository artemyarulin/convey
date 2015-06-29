# rp-transducers
A set of additional transducers like `do`, `do-error`, `zip`

# Why

Transducers is very powerfull concept and combined with core.async it can give you the same power as using libraries like RxJS. Standard library contains most of the transducers you may need (filter, map, mapcat,etc.) but there are couple of things that missing:

- `do` - Handy function for side affects, useful for logging purposes
- `do-error` - For raising errors
- `zip` - Same as zipmap

