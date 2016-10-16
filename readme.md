Provides the `geny.Generator` data type, A Generatorerator of elements of type `A`.

`Generator` is basically the inverse of
a `scala.Iterator`: instead of the core functionality being the pull-based
`hasNext` and `next: T` methods, the core is based around the push-based
`generate` method. `generate` is basically an extra-customizable version of
`foreach`, which allows the person calling it to provide basic control-flow
instructions to the upstream Generators.

Unlike a `scala.Iterator`, subclasses of `Generator` can guarantee any clean
up logic is performed by placing it after the `generate` call is made.

Transformations on a `Generator` are lazy: calling methods like `filter`
or `map` do not evaluate the entire Generator, but instead construct a new
Generator that delegates to the original. The only methods that evaluate
the `Generator` are the "Action" methods like
`generate`/`foreach`/`find`, or the "Conversion" methods like `toArray` or
similar.

`generate` takes a function returning `Generator.Action` rather that
`Unit`. This allows a downstream Generator to provide basic control
commands to the upstream Generators: i.e. `Generator.End` to cease
enumeration of the upstream Generator. This allows it to avoid traversing and
processing elements that the downstream Generator doesn't want/need to see
anyway
