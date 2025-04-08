---
layout: post
title: Common issues
---

Below is a brief list of issues you may encounter when developing on the JVM and some methods for investigating them.

## NoClassDefFoundError

We've seen multiple instances of this error when an attempt is made to load a class which does not exist on the classpath.
There are two steps required to resolve such issues - understanding where you expect the missing class to be loaded from, and
finding out what the classpath is.

In principle there's no connection between a JAR file and the packages of .class files it contains. In practice however,
packages tend to be grouped under the `groupId` or at least the `artifact` name of the published JAR. For example, the `json-java`
JAR seen previously is published by the group `org.json` and all the classes it defines exist within the package `org.json`.
Classes in the core Clojure JAR are defined under the package `clojure`. So a good place to start for investigating a missing class
is to find the package name and search for dependencies with the same group or artifact id. Classpaths for even medium-sized projects
can be quite large, and it's not uncommon to come across unknown classes deep in the dependency tree. 

### Finding the classpath

#### Statically

Most project management tools such as Maven, Leiningen or tools.deps have tasks to output the current classpath. There are usually
multiple classpaths within a project depending on the context. A common example is adding extra dependencies or resource directories
when running tests. As we saw previously, the classpath for a Maven project can be retrieved with

    mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt

#### At runtime

Within a running application, the classpath can be fetched via the system property `java.class.path`. If you are writing the application
you can simply output it with e.g.

    System.out.println(System.getProperty("java.class.path"));

If you're investigating an application you didn't write and can't modify, you can get the properties for a running process with [jinfo](https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr013.html).
First locate the PID of the Java process - the [jps](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jps.html) command can be used to list all running Java processes

    jps -v

then get the properties of the running process

    jinfo -sysprops <PID>

Again the `java.class.path` property contains the classpath of the process.

## UnsupportedClassVersionError

This exception is thrown when trying to load a class file with a newer format than the executing JVM supports. The [JVM specification](https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html#jvms-4.1)
lists the class file versions supported by each Java version. If you encounter this error, you have two options - find a version of the class compiled for older class file format supported by your JVM, or use a
newer JVM which supports the existing class file format.

## NoSuchMethodError

This error usually occurs when an application is compiled against one version of a class, but a different version is available at runtime. Consider a simple class to display a greeting:

**version1/Greeter.java**
```java
{% include code/common_issues/version1/Greeter.java %}
```

After some time, a new version of this class is created with a new method:

**version2/Greeter.java**
```java
{% include code/common_issues/version2/Greeter.java %}
```

An application is created which uses the newer version:

**GreetApp.java**
```java
{% include code/common_issues/GreetApp.java %}
```

Compiling both class versions and the application against the newer version

```
pushd version1 && javac Greeter.java && popd
pushd version2 && javac Greeter.java && popd
javac -cp .:version2 GreetApp.java
```

Running the application with the new version on the classpath works as expected:

    java -cp .:version2 GreetApp

Running with the old version on the classpath results in an error:

```
> java -cp .:version1 GreetApp
{% include code/common_issues/nosuchmethoderror.txt %}
```

Some libraries deprecate and subsequently remove methods from classes, so it is possible that
a class newer than the one expected is loaded instead. This error is usually the result of a mismatch
between the expected and actual version of a library being loaded at runtime.

The first diagnostic step is finding out where the class is being loaded from. This can be done by supplying
the `-verbose:class` option to the JVM

    java -verbose:class ...

This outputs the location each class is loaded from. Writing this to file or filtering with `grep` should allow you
to find where the class is located on the classpath. This will usually be a JAR file, and if fetched from a Maven repository
will contain the artifact name and version number in the filename.

The next step is deciding where you expect the class to be loaded from - this might be a different library entirely, or a different
version of the same library.

The final step is to fix the classpath so the class is loaded from the expected location. This may involve excluding a transitive dependency
somewhere in your dependency list.

## ServiceLoader errors

Java [ServiceLoaders](https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/util/ServiceLoader.html) are an extensible mechanism for
locating and loading service classes at runtime. They work by first defining an interface or class e.g.

```java
package com.picosoft.messaging;

public interface MessageSourceFactory {
    ...
}
```

then by naming implementing classes in a file provided by implementation JARs. This file should exist at `META-INF/services/{service class binary name}` within the
implementation JAR file. For example, a JAR providing database sources might define the following classes:

```java
package com.picosoft.messaging.db;
public class PostgresMessageSourceFactory implements MessageSourceFactory { ... }
public class MySqlMessageSourceFactory implements MessageSourceFactory { ... }
```

These would then be listed as implementation classes within the corresponding interface service file:

**META-INF/services/com.picosoft.messaging.MessageSourceFactory**
```
com.picosoft.messaging.db.PostgresMessageSourceFactory
com.picosoft.messaging.db.MySqlMessageSourceFactory
```

When the implementation JAR is on the classpath, these classes can be located and loaded via the `ServiceLoader` interface.

Special care must be taken when building an uberjar containing service loader classes. Multiple implementation JARs will define
the same `META-INF/services/{service.class}` file. When building uberjars, the default conflict handling behaviour for
files at the same path is 'last one wins'. For service loaders, conflicting files should be concatenated together so all implementation classes are
listed in the final JAR.

## Uberjar path conflicts

The service loader issue listed above is just a specific case of a more general issue when building uberjars - conflicts between files
at the same path in multiple JARs must be resolved to produce the file in the output JAR. Use the `jar` tool to inspect files
within JARs to ensure they contain what you expect. 