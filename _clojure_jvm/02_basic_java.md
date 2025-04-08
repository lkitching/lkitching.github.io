---
layout: post
title: Java
---

Java is a statically-typed object-oriented language. Java programs are organised as a collection of classes and interfaces. Classes extend a single superclass, implement a number of interfaces, and define various fields and methods.
Interfaces can declare any number of super-interfaces, along with methods that implementing classes must define.

__Hello.java__
{% highlight java %}
{% include code/basic_java/Hello.java %}
{% endhighlight %}

This defines a class `Hello` in the package `test`. This class contains a single static `main` method which writes a short message to the console.

This file can be compiled with `javac`:

    $ javac -d . Hello.java

This outputs a `Hello.class` file in the `./test` directory. The `main` method can be invoked with:

    $ java test.Hello
    Hello world!

## Entry point

Java program execution begins from a named class specified to the Java runtime. In the previous example this was the class `test.Hello`.
The JVM loads this class and executes its `main` method, which must have the signature `public static void main(String[])` i.e. a static 
method called `main` which takes a single String array parameter. This array receives all the arguments provided to the `java` command which
do not configure the execution of the JVM itself.

## Loading classes

In order to execute the `main` method of the `test.Hello` class, the JVM must be able to locate and load the binary definition of the class.
The job of locating and loading classes is done by _class loaders_. These are implementations of the `java.lang.ClassLoader` class. The JVM defines
a default class loader, called the 'bootstrap class loader'. 

### Classpath

The JVM locates user classes by searching a list of locations defined by the 'classpath'. By default, the classpath usually just contains the current
directory the `java` command is invoked from. The full name of the class dictates the expected location of the `.class` file containing the binary representation
of the class. For a simple top-level class like `test.Hello`, this location is constructed by simply replacing the `.` characters in the class name with `/` path
separator characters, and adding a `.class` extension. So the class `test.Hello` should exist at `test/Hello.class` under one of the directories on the classpath.
Since the default classpath contains just the current directory, the only candidate location is at `./test/Hello.class`.

If you change into the `test` directory and run the `java` command again you will receive an error:

    $ cd test
    $ java test.Hello
    Error: Could not find or load main class test.Hello

This is because within the `test` directory, no `test/Hello.class` file exists so the `test.Hello` class cannot be loaded.

Within this directory, the `Hello.class` file does exist, so you might try the following:

    $ java Hello
    Error: Could not find or load main class Hello
    Caused by: java.lang.NoClassDefFoundError: test/Hello (wrong name: Hello)

In this case, the `Hello.class` file was located, however the class defined there has a different name `test.Hello` so loading fails.

Since classes are resolved via their full names relative to the classpath, you need to put the previous directory (i.e. the current parent directory)
on the classpath. This can be done with the `-classpath` option to the `java` command:

    $ java -classpath ".." test.Hello
    Hello world!

change back to the parent directory where the `Hello.java` file is defined

    $ cd ..

### System classes

The `test.Hello` class makes reference to another class `java.lang.System` which is defined by the core Java library. Since there's no `java/lang/System.class` file
under the current directory, you might wonder how it is loaded. The answer is that the bootstrap class loader has its own internal mechanism for locating core library classes.
The only requirement for class loaders is that they can resolve and load classes given their full (binary) names. There is no requirement that these exist as `.class` files on
disk. They can be loaded from the network, a database or defined dynamically depending on how the class loader is implemented. The bootstrap class loader knows where to locate
core Java classes and loads them as required.

Older JVMs (before version 9) did ship core Java classes in a Java archive. This was usually located at `jre/lib/rt.jar` within the JVM distribution. The bootstrap class loader
was additionally configured with a 'bootstrap classpath' containing this core archive. Since the advent of Java modules in Java 9, core classes are distributed in a more efficient
format.

You can tell the `java` command to log more information on the class loading process with the `-verbose` option.

    $ java -verbose:class test.Hello
    ...
    [0.006s][info][class,load] java.lang.System source: shared objects file
    ...
    [0.027s][info][class,load] test.Hello source: file:/classpath/directory/

This will usually show a lot of output as each class and their references need to be loaded as part of the program execution.