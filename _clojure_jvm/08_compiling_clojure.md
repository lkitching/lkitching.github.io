---
layout: post
title: Compiling Clojure
---

We saw previously how Clojure source code is located on the classpath, read and evaluated. Clojure also supports defining classes which can be compiled
into `.class` files with [gen-class](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/gen-class). The following example defines
two classes with `gen-class`:

**src/greet.clj**
```clojure
{% include code/compiling_clojure/greet/src/greet.clj %}
```

This can be compiled with the [compile](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/compile) function. As described in the documentation,
the `.class` files are output to a directory defined by `*compile-path*` which must be on the classpath. The value of `*compile-path*` is `classes` by default.
Create this directory and compile the `greet` namespace with `compile`: 

```
mkdir classes
java -cp classes:src:clojure-1.11.1.jar:core.specs.alpha-0.2.62.jar:spec.alpha-0.3.218.jar clojure.main -e "(compile 'greet)"
```

This defines two classes using `gen-class` - `greeter.Greet` and `greeter.main`. You can see how these are compiled into Java classes
with `javap`:

**greet.Greeter**
```java
{% include code/compiling_clojure/Greeter_decompiled.java %}
```

**greet.main**
```java
{% include code/compiling_clojure/main_decompiled.java %}
```

The generated class methods simply delegate to Clojure functions with the appropriately-prefixed names. For example the `greet.Greeter` class defines a function prefix
of `greeter-`, so its `greet` method delegates to the function `greeter-greet` within the implementation namespace (by default the current namespace). Similarly, the `main`
method of the `greet.main` class should be defined by a `-main` function.

The `main` class can now be invoked as with any other Java main class:

```
> java -cp classes:clojure-1.11.1.jar:core.specs.alpha-0.2.62.jar:spec.alpha-0.3.218.jar greet.main everyone
Hello everyone!
```

## Namespace compilation

The `ns` macro used to define namespaces supports an optional `:gen-class` option to compile a class for the namespace.
It supports all the options of [gen-class](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/gen-class)
and by default uses the namespace name as the class name, sets `:main` to `true`, and sets the prefix to `-`. This means
a `-main` function is expected to exist which corresponds to the `main` method of the generated class.

This allows us to write straightforward Clojure code which is compiled into a Java entrypoint without having to invoke it via
`clojure.main` or use `gen-class` directly.

**src/greet/core.clj**
```clojure
{% include code/compiling_clojure/main/src/greet/core.clj %}
```

**src/greet/main.clj**
```clojure
{% include code/compiling_clojure/main/src/greet/main.clj %}
```

This can be compiled and run as before:

```
> mkdir classes
> java -cp clojure-1.11.1.jar:core.specs.alpha-0.2.62.jar:spec.alpha-0.3.218.jar:classes:src clojure.main -e "(compile 'greet.main)"
> java -cp clojure-1.11.1.jar:core.specs.alpha-0.2.62.jar:spec.alpha-0.3.218.jar:classes:src greet.main everyone
Hello everyone!
```

## Clojure uberjars

Now that we can create Java entrypoints for our Clojure applications, we can create 'fat' JARs by including the Clojure JARs along with
the application source files and any compiled classes. As before, we need to define a manifest file which specifies the main class

**manifest.mf**
```
{% include code/compiling_clojure/uberjar/manifest.mf %}
```

The fat JAR can then be built with

```
> mkdir classes uber
> unzip -d uber clojure-1.11.1.jar
> unzip -o -d uber/ core.specs.alpha-0.2.62.jar
> unzip -o -d uber/ spec.alpha-0.3.218.jar
> java -cp clojure-1.11.1.jar:core.specs.alpha-0.2.62.jar:spec.alpha-0.3.218.jar:classes:src clojure.main -e "(compile 'greet.main)"
> cp -r classes/* uber/
> cp -r src/* uber/
> jar --create --file greet-standalone.jar --manifest=manifest.mf -C uber .
```

This JAR can now be invoked with:

```
> java -jar greet-standalone.jar everyone
Hello everyone!
```

Note the caveats for correctly handling path conflicts discussed previously. 