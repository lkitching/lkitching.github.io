---
layout: post
title: The JVM and JDK
---

Java (and Clojure) code is not executed directly. Instead, it is compiled into an architecture-independent binary format which is executed
by a Java Virtual Machine (JVM). The behaviour of the JVM is defined by the [Java Virtual Machine Specification](https://docs.oracle.com/javase/specs/jvms/se20/html/index.html).
Multiple implementations of this standard are available.

Java source files are converted to their binary representation (called .class files) by a compiler, `javac`. `javac`, along with various other tools used for Java development are included in the Java Development Kit (JDK).
Each release of the Java platform has a corresponding JDK for development.

## Java Virtual Machine

The JVM is a virtual machine which defines its own instruction set, referred to as Java bytecode. The process of locating, loading, verifying and executing this bytecode is
defined by the [JVM specification](https://docs.oracle.com/javase/specs/jvms/se20/html/index.html). After installing a Java distribution, the JVM can be invoked via the `java`
command.

## Java Development Kit

Like the JVM, the Java language is also defined by a [language specification](https://docs.oracle.com/javase/specs/jls/se20/html/index.html). Compilers are responsible for
compiling Java source files to class files according to this specification. The `javac` compiler, along with various other tools useful for development such as `javap` and
`jstack` are distributed as part of the Java Development Kit (JDK).