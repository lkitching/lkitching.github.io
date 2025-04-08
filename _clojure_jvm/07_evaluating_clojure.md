---
layout: post
title: Evaluating Clojure
---

Clojure on the JVM is implemented in a combination of Java and Clojure. The core platform and compiler are written in Java,
and the base libraries are written in Clojure. Together they are (mostly) distributed in a single JAR file. Recent versions
also have additional dependencies on a couple of `spec.alpha` JARs as described on the [downloads page](https://clojure.org/releases/downloads).

Fetch the required JARs listed on the downloads page:

```
curl -LO https://repo1.maven.org/maven2/org/clojure/clojure/1.11.1/clojure-1.11.1.jar
curl -LO https://repo1.maven.org/maven2/org/clojure/spec.alpha/0.3.218/spec.alpha-0.3.218.jar
curl -LO https://repo1.maven.org/maven2/org/clojure/core.specs.alpha/0.2.62/core.specs.alpha-0.2.62.jar
```

This should leave you with `clojure-1.11.1.jar`, `spec.alpha-0.3.218.jar` and `core.specs.alpha-0.2.62.jar` in the current directory.

## clojure.main

All invocations of the `java` command require a main class to be specified as the entry point. The Clojure JAR contains such a class - `clojure.main`
which can be used as an entry point. The full set of options for `clojure.main` are [described here](https://clojure.org/reference/repl_and_main).

As a simple example we can provide a string of Clojure code to be evaluated:

```
> java -cp clojure-1.11.1.jar:core.specs.alpha-0.2.62.jar:spec.alpha-0.3.218.jar clojure.main -e '(println "Hello world!")'
Hello world!
```

### Locating Clojure files

Most Clojure code is written to files and located and evaluated by the Clojure runtime during program execution. Similar to Java class files,
Clojure source files are located on the classpath. When a namespace is loaded, the namespace name is converted to a set of relative locations on the
classpath where the namespace could be defined. The conversion process is as follows:

1. Convert any `.` characters in the namespace name to `/`s
2. Convert any `-` characters in the namespace name to `_`s
3. Add `.clj`, `.cljc` and `__init.class` to the resulting name

This means there are three candidate locations a namespace could be loaded from. Applying the process to the namespace
`org.picosoft.lib-hello.core` the candidate locations are:

1. `org/picosoft/lib_hello/core.clj`
2. `org/picosoft/lib_hello/core.cljc`
3. `org/picosoft/lib_hello/core__init.class`

The first two candidates are expected to be Clojure source files, while the last is a compiled `.class` file.

If we define the above namespace 

**src/org/picosoft/lib_hello/core.clj**
```java
{% include code/evaluating_clojure/src/org/picosoft/lib_hello/core.clj %}
```

and place it in the appropriate place on the classpath, we can invoke it via `clojure.main`
```
> java -cp clojure-1.11.1.jar:core.specs.alpha-0.2.62.jar:spec.alpha-0.3.218.jar:src clojure.main -m org.picosoft.lib-hello.core
Hello world!
```

The `-m` option takes the name of a namespace expected to contain a `-main` function to be executed.

### Packaging

Since Clojure source files are located and evaluated from the classpath similar to compiled Java `.class` files, they can be packaged
into JAR files for easier distribution. As with `.class` files this can be done with the `jar` tool:

     > jar --create --file lib-hello.jar -C src .

This packages the contents of the `src` directory into `lib-hello.jar` and this can be placed on the classpath as before:

```
> java -cp clojure-1.11.1.jar:core.specs.alpha-0.2.62.jar:spec.alpha-0.3.218.jar:lib-hello.jar clojure.main -m org.picosoft.lib-hello.core
Hello world!
```