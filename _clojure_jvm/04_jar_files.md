---
layout: post
title: JAR Files
---

So far we've seen how Java source files are compiled into binary class files, and how these are located at runtime when the containing directory is placed on the classpath.
This approach works for the single file application developed previously, but most applications consist of many classes and make use of libraries which themselves define classes
and interfaces of their own. Managing and publishing directories of class files would be cumbersome, but fortunately Java defines a standard for packaging related classes into a single
file called a Java ARchive, or JAR file.

## Lib Hello World

After showing our groundbreaking salutatory application to our chief architect, they are mainly positive, but have some suggestions:

* Instead of hard-coding the message, it should be supplied as input to the program
* Messages could come from anywhere, not just the command line
* Messages could be written anywhere, not just the console
* Messages could be of arbitrary size
* Other teams might want to take advantage of our work, so we should publish it as a library

After some back-and-forth, we settle on defining interfaces for sources and destinations for messages:

__src/libhello/MessageSource.java__
``` java
{% include code/jar/src/libhello/MessageSource.java %}
```

__src/libhello/MessageSink.java__
``` java
{% include code/jar/src/libhello/MessageSink.java %}
```

In addition, we define a source of messages read from the command-line, and a sink which writes messages to a `PrintStream`:

__src/libhello/CommandLineMessageSource.java__
```java
{% include code/jar/src/libhello/CommandLineMessageSource.java %}
```

__src/libhello/PrintStreamMessageSink.java__
```java
{% include code/jar/src/libhello/PrintStreamMessageSink.java %}
```

We compile these classes as usual and output the corresponding class files to the `libhello` directory:

```
javac -d classes/libhello src/libhello/*.java 
```

This outputs the class files under the `classes/libhello` directory. We can then build a JAR file from this directory:

```
jar --create --file libhello.jar -C classes/libhello .
```

this creates a `libhello.jar` file in the current directory. We can list the contents of this file with the `--list` command:

    jar --list --file libhello.jar

This shows the archive contains the `.class` files as their expected locations on the classpath, along with a `META-INF/MANIFEST.MF` file.
This file is called the _manifest_ file and is described [below](#manifest-files).

```
{% include code/jar/jar_list %}
```

Note that JAR files are also zip files, so their contents can be listed with the `unzip` command:

    unzip -l libhello.jar

Now we have build our library, we can re-write our application to use it:

__src/app/Echo.java__
```java
{% include code/jar/src/app/Echo.java %}
```

As before, we compile it with `javac`. Since the application references the classes in `libhello.jar`, we have to place it on the classpath
to make the class definitions available.

    javac -d classes/app -cp libhello.jar src/app/Echo.java

This writes the `test.Echo` class to the `classes/app` directory. Now we can run the application. Again we have to make the classes in `libhello`
available by adding the jar file to the classpath. We also add the build output directory for the app so the main `test.Echo` class can be resolved:

    java -cp libhello.jar:classes/app test.Echo Hello world '!'

As expected, the application reads each message from the command line and writes it to the console

```
{% include code/jar/app_output %}
```

Of course we should also create a JAR for the application:

    jar --create --file echo.jar -C classes/app/ .

which we can then execute by placing both the lib and app JARs on the classpath:

    java -cp libhello.jar:echo.jar test.Echo Hello world '!'

## Manifest files

When listing the contents of the JAR file above, we saw a `META-INF/MANIFEST.MF` file included. The `META-INF` directory within a JAR contains 
files used to configure aspects of the JVM. The `MANIFEST.MF` file contains various sections of key-value pairs used to describe the contents of the JAR.
The format of the `META-INF` directory and `MANIFEST.MF` files are described in detail within the [JAR file specification](https://docs.oracle.com/en/java/javase/20/docs/specs/jar/jar.html).

The `jar` tool can be used to extract the contents of the default manifest file:

    jar --extract --file echo.jar META-INF/MANIFEST.MF

this is fairly minimal by default:

```
{% include code/jar/default_manifest.mf %}
```

There are two additional manifest properties we would like to set when building the application JAR:

* `Main-Class`: This is the name of the main class the JVM should load on startup
* `Class-Path`: The classpath to configure

We can add these properties to a file to be added to the manifest when building the application JAR:

__echo-manifest.mf__
```
{% include code/jar/echo-manifest.mf %}
```

    > jar --create --file echo.jar --manifest=echo-manifest.mf -C classes/app .

Adding the main class and classpath to the application manifest means the application can be run with the `-jar` option
without needing to specify the classpath and main class manually:

    > java -jar echo.jar Hello world '!'

## 'Fat' JARs

Defining the classpath and main class within the application manifest simplifies the command required to invoke the application,
but it still requires ensuring the dependency JARs are in the expected location as defined by the `Class-Path` entry in the application
manifest. From a deployment perspective it would be easier if the application and all its dependencies were packaged in a single file.

Since a JAR file represents a single root on the classpath, multiple JAR files can be combined into one by simply extracting them all to
a single directory and re-packaging them within a new JAR file. Such JARs are usually referred to as 'fat' (or 'uber') JARs.

We can first extract our library and application JARs to a temporary directory:

    > unzip -d uber libhello.jar
    > unzip -o -d uber echo.jar

and build a new JAR with a new manifest file. Unlike the previous application manifest, this does not need to specify the classpath
since all classes are contained within the new JAR.

__uber-manifest.mf__
```
{% include code/jar/uber-manifest.mf %}
```

We can now build the new JAR:

    > jar --create --file echo-uber.jar --manifest=uber-manifest.mf -C uber .

and run it with the `-jar` option as before:

    > java -jar echo-uber.jar Hello world '!'

### File collisions

When building the uberjar above, we simply overwrote (using the `-o` option for `unzip`) any existing files within `libhello.jar`
when extracting the `echo.jar` file. This is fine for our simple application since there are no collisions, except for the default manifest
files in the two files, which should be the same. However, this simple strategy may not work for more complicated applications. It's possible
that different JAR files will contain different files at the same path which need special handling to produce the corresponding file in the
output JAR. One example is libraries which configure [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html)
classes which are configured by files in the `META-INF/services` directory.