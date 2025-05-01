---
layout: post
title: tools.build
---

With `tools.deps` we can now build libraries and publish them to our favourite Git hosting provider and have others consume
them as dependencies without the need to package and publish them as JARs. As we've seen previously, we can do this using
the tools provided by Clojure and the JVM. However, these do not integrate easily with `tools.deps` and there are various
common tasks we'd like to automate. Clojure provides the [tools.build](https://clojure.org/guides/tools_build) library for
defining build tasks.

After showing your CTO the Clojure greet application, she's very impressed but informs you the company has recently invested heavily in spare `{` and `}` keys,
so all library development must be done in Java. The application itself can still be written in Clojure.

You restructure your project to have separate source directories for Java and Clojure:

```
greeter/
  deps.edn
  build.clj
  java/
    src/
  clojure/
    src/
```

Then define the Java library:

**java/src/com/picosoft/greet/Greeter.java**
```java
{% include code/build/java/src/com/picosoft/greet/Greeter.java -%}
```

The main Clojure namespace imports this class for the greet implementation

**clojure/src/greet/main.clj**
```clojure
{% include code/build/clojure/src/greet/main.clj -%}
```

The Clojure source location needs to be specified in the `deps.edn` file along with the `tools.cli` dependency. There is also
a `build` alias used by our build definition.

**deps.edn**
```clojure
{% include code/build/deps.edn -%}
```

Finally our `tools.build` script defines an `uber` task which compiles the Java and Clojure code and packages it into an uberjar.

**build.clj**
```clojure
{% include code/build/build.clj -%}
```

This task can be invoked with

    > clojure -T:build uber

this builds the uberjar at `target/greeter-standalone.jar` which can be run with

```
> java -jar target/greeter-standalone.jar --excite everyone
Hello everyone!
```
