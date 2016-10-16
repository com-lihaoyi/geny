Provides the `geny.Gen` data type, A Generator of elements of type `A`.

`Gen` is basically the inverse of
a `scala.Iterator`: instead of the core functionality being the pull-based
`hasNext` and `next: T` methods, the core is based around the push-based
`generate` method. `generate` is basically an extra-customizable version of
`foreach`, which allows the person calling it to provide basic control-flow
instructions to the upstream Gens.

Unlike a `scala.Iterator`, subclasses of `Gen` can guarantee any clean
up logic is performed by placing it after the `generate` call is made.

Transformations on a `Gen` are lazy: calling methods like `filter`
or `map` do not evaluate the entire Gen, but instead construct a new
Gen that delegates to the original. The only methods that evaluate
the `Gen` are the "Action" methods like
`generate`/`foreach`/`find`, or the "Conversion" methods like `toArray` or
similar.

`generate` takes a function returning `Gen.Action` rather that
`Unit`. This allows a downstream Gen to provide basic control
commands to the upstream Gens: i.e. `Gen.End` to cease
enumeration of the upstream Gen. This allows it to avoid traversing and
processing elements that the downstream Gen doesn't want/need to see
anyway
