---
layout: post
title: Deps
---

So far we've seen how Java files are compiled to class files and collected into JAR files which are placed on the classpath
so they can be located at program execution time. Tools like Maven are used to manage dependencies and build the required
classpath to make them all available.

The existing Java ecosystem is built around publishing JARs to binary repositories, and, like Java, Clojure code can be packaged into JAR files, 
whether it has been compiled ahead-of-time or is being distributed as source. Many Clojure libraries are published to the [Clojars](https://clojars.org/)
repository instead of Maven Central. However, since directories can be placed on the classpath, and Clojure code can be located
and evaluated directly from source, the extra packaging and publish steps are not necessary for developing Clojure libraries and programs.

The [tools.deps](https://github.com/clojure/tools.deps) library is used to declare dependencies from various sources and build the required
classpath for execution.

## Basic example

In the previous two chapters, we fetched the required Clojure JARs locally, and added them along with our source directories to the classpath
when executing the program. This can be simplified by using the Clojure CLI and deps.

First install the [Clojure CLI](https://clojure.org/guides/install_clojure) from the instructions on the Clojure site. After installation, running
the `clojure` command should start a Clojure REPL:

```
> clojure
user=> (+ 1 2)
3
```

Now we can create a basic deps project. The structure of the project is defined in a `deps.edn` file in the project root directory.

**deps.edn**
```clojure
{% include code/deps/basic/deps.edn %}
```

This declares a dependency on the main `clojure` JAR. This JAR is published to Maven Central so should be resolved as a Maven dependency.
This JAR and all of its transitive dependencies should be available on the classpath at runtime. The `src` directory should also be placed
on the classpath since that is where our application namespaces are defined. Define the main namespace within this directory:

**src/greet/main.clj**
```clojure
{% include code/deps/basic/src/greet/main.clj %}
```

The `clojure` CLI allows us to invoke `clojure.main` with the project classpath with the `-M` option:

```
> clojure -M -m greet.main everyone
Hello everyone!
```

You can display the computed classpath with

```
> clojure -Spath
src:~/.m2/repository/org/clojure/clojure/1.11.1/clojure-1.11.1.jar:...
```

As expected, it contains our `src` directory and the JAR files for the Clojure JAR and all its dependencies.

### System and user deps

`tools.deps` actually looks for multiple `deps.edn` files which are merged together to build the final project definition. These are
refered to as the `system` and `user` files. You can find the locations of all these files with `describe`:

```
> clojure -Sdescribe
{:version "1.11.1.1267"
 :config-files ["/install/dir/deps.edn", "~/.clojure/deps.edn", "deps.edn"]
```

If you open the first of these (the system project file), you will see it has default settings for the `:paths` and `:deps` properties

**/install/dir/deps.edn**
```clojure
{:paths ["src"]
 :deps {
   org.clojure/clojure {:mvn/version "1.11.1"}
 }
 ...
```

This means we could use these defaults in our project and our project `deps.edn` file could simply contain

**deps.edn**
```clojure
{}
```

## Aliases

At different points of the development process, you might want to make additional dependencies available or add more directories to the classpath.
This can be done by specifying _aliases_ within the project `deps.edn` and supplying them to the `clojure` command where required. For example, during
development, you might want to use [scope capture](https://github.com/vvvvalvalval/scope-capture) to help with debugging at the REPL. This isn't a dependency
of the main application, so shouldn't be distributed with it.

You can define a `dev` alias within the project `deps.edn` file

**deps.edn**
```clojure
{% include code/deps/aliases/deps.edn %}
```

Then supply it when starting the REPL:

```
> clojure -A:dev
user=> (require '[sc.api :as sc])
nil
user=>
```

## Other dependency sources

The Clojure JARs are published to Maven Central so are declared as Maven dependencies in the previous `deps.edn`. Deps supports other dependency
types such as Git repositories and local directories.

We've decided to improve our greeting application by adding an optional command-line flag to show enthusiasm (or not). We also decided to split the
core greeting functionality into its own library. Since it's under active development, we just want to refer to it locally for now before we're ready
to publish the first version.

We define the library first:

**lib/deps.edn**
```clojure
{% include code/deps/sources/lib/deps.edn %}
```

**lib/src/greet/core.clj**
```clojure
{% include code/deps/sources/lib/src/greet/core.clj %}
```

We decide to use [tools.cli](https://github.com/clojure/tools.cli) for the command-line parsing. We need to use a specific commit for legal reasons, so
add it as a Git dependency rather than depend on the published JAR. This results in the following structure:

**main/deps.edn**
```clojure
{% include code/deps/sources/main/deps.edn %}
```

**main/src/greet/main.clj**
```clojure
{% include code/deps/sources/main/src/greet/main.clj %}
```

This can then be run as before:

```
> clojure -M -m greet.main --excite everyone
Hello everyone!
```

If you look at the classpath for this project:

```
> clojure -Spath
src:~/.gitlibs/libs/clojure/tools.cli/23ee9655fab71cef253a51d1bce3e7b2327499a3/src/main/clojure:...:~/projects/greet/lib/src:...
```

You can see the `tools.cli` repository was cloned locally under `~/.gitlibs` and the local project source directory was placed on the classpath.

