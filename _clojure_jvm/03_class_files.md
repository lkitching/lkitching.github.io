---
layout: post
title: Class files
---

Java classes are compiled into a binary representation stored in `.class` files. The contents of `.class` files can be viewed with the `javap` tool shipped with the JDK.
Here's the simple Java class from the previous chapter:

__Hello.java__
{% highlight java %}
{% include code/basic_java/Hello.java %}
{% endhighlight %}

When compiled with `javac`:

    javac -d . Hello.java

it creates a `test/Hello.class` file in the current directory. Opening this file with `javap` shows a brief summary of the `test.Hello` class:

```
{% include code/basic_java/hello_summary %}
```

The `.class` file contains all the information required to load the class and execute its methods. The full contents of the file can be displayed
using the `-verbose` option:

    javap -verbose test/Hello.class

This shows much more detail:

```
{% include code/basic_java/hello_full %}
```

The format of `.class` files is described in full in the [JVM specification](https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-4.html).
They contain a sequence of sections

1. [Magic number](#magic-number)
2. [Version number](#version-number)
3. [Class properties](#class-properties)
4. [Constant pool](#constant-pool)
5. Interfaces
6. Fields
7. [Methods](#methods)
8. Attributes

Since the `test.Hello` class contains no fields and implements no interfaces they are not described here.

## Magic number

All `. class` files begin with the 4-byte constant `0xCAFEBABE`. This is not displayed by `javap` but can be seen by viewing the raw bytes.
Note `.class` files are stored in big-endian order which may differ from the architecture of your system.

    od --endian=big -x test/Hello.class

```
{% include code/basic_java/hello_dump %}
```

## Version number

The first information displayed by `javap` is the major and minor version of the class file format this file uses. This class file uses version `58.0`
which means it is only supported by versions 14 or higher of the JVM. Attempting to load this class on an older version of the JVM will result in an error e.g.

```
{% include code/basic_java/version_error %}
```

## Class properties 

The `flags`, `this_class` and `super_class` entries define the access properties and names of the defined class and its direct superclass. The `test.Hello` class does not explicitly
declare a superclass, so it is implicitly a subclass of `java.lang.Object`. 

## Constant pool

The `test.Hello` class makes various symbolic references to other code elements, such as classes and their methods. Some of these references are not explicit in the source
code. These names and their types are recorded in the constant pool.

### Binary names

The format of binary names within `.class` files differs slightly from those in `.java` files. The `.` separator used by Java is replaced within `.class` files.
For example the class `java.lang.Object` will be refered to as `java/lang/Object` within class files.

### Descriptors

Within class files, descriptors are used to define the types of fields and methods.

#### Field descriptors

JVM types are either one of the primitive types, an object type or an array type. The grammar for
field descriptors denotes one of these three possibilities.

The primitive types are `byte`, `char`, `double`, `float`, `int`, `long`, `short` and `boolean`, denoted as `B`, `C`, `D`, `F`, `I`, `J`, `S` and `Z` respectively within
the field descriptor grammar. For example, a descriptor of `J` indicates a field of the primitive `long` type.

Object types are defined with the literal `L` followed by the binary name of the class type followed by the literal `;`. For example a field of type `java.lang.Thead` has a descriptor of `Ljava/lang/Thread;`.

Array types are defined with the literal `[` followed by the element type. So an array of `java.lang.Thread` references has a descriptor of `[Ljava/lang/Thread;` and an array of primitive `int` values
has a descriptor of `[I`.

#### Method descriptors

Methods take a (possibly empty) collection of arguments and optionally return a value. Methods which do not return a value are given a return type of `void` (denoted by `V`) within method descriptors.
Methods which return a value use the grammar for field descriptors to denote the return type.

Method descriptors consist of a literal `(`, the corresponding field descriptor for each parameter type in sequence, a literal `)` followed by the return type descriptor. This can be either `V` for `void` methods
or a field descriptor for the return type.

For example the method `public String test(int i, boolean b, Object o)` has a method descriptor of `(IZLjava/lang/Object;)Ljava/lang/String;`.

## Constant pool contents

Here are the references made by the `test.Hello` class:

| Index | Name | Type | Description |
|-------|------|------|-------------|
| #2    | `java/lang/Object` | Class | This is the (implicit) superclass of the `test.Hello` class. The `super_class` property (see above) contains the index of this class reference within the constant pool. |
| #1   | `java/lang/Object::<init>` | Method | Object constructors are given the special name `<init>` within `.class` files. This is invoked by the `test.Hello` constructor (see below). |
| #8   | `java/lang/System` | Class | Class which defines the static `out` field |
| #7   | `java/lang/System::out` | Field | Reference to the `out` field of the `java.lang.System` class |
| #12 | `Ljava/io/PrintStream;` | Type reference | The declared type of the `System.out` field |
| #13 | | String | The String constant "Hello world!" |
| #16 | `java/io/PrintStream` | Class | Type defining the `println` method |
| #15 | `java/io/PrintStream::println` | Method | The `println` method used to write to the console |
| #21 | `test/Hello` | Class | The class defined by this class file. The `this_class` property contains this index into the constant pool |
| #25 | `main` | utf8 | Name of the `main` method |

## Methods

Entries in the methods table define the name, attributes (access modifiers etc.) and JVM opcodes for the methods defined by the class.

### Instruction format

JVM instructions are variable-length and consist of an `opcode`, which defines the operation, and a (possibly empty) sequence of operands. `javap` displays
each instruction in the following format:

    <index> <opcode> [ <operand1>, ..., <operandN> ] [ <comment> ]

`index` is the index of the start of the instruction in the code array for the method.
`opcode` is a mnemonic for the operation.

### Method execution

Each JVM thread defines its own private stack consisting of _frames_ for each method invocation. As a method is invoked, a new stack frame is allocated
to store space for local variables and temporary results during execution. On method exit, the frame is popped from the stack and execution returns to the calling
method.

Each method also makes use of an _operand stack_ which is manipulated by individual JVM instructions. Some instructions push values onto the operand stack, some
store values from the top of the stack into local variables, and some consume multiple items from the top of the stack and push a result.

#### Arguments

In order to invoke a method, its arguments are pushed in order onto the operand stack and the method is invoked via a method reference in the class constant pool.

Instance methods are invoked via the `invokevirtual` instruction and have a first argument which is a reference to the object the method is being invoked on (the receiver).
Static methods are invoked with `invokestatic` and only the explicit arguments are pushed prior to invocation.

Within a method, the arguments are loaded onto the operand stack with a family of opcode instructions (`aload`, `iload` etc.).

## Hello class methods

The `test.Hello` class defines two methods - an implicit constructor and the static `main` method.

### Constructor

```
public test.Hello();
    descriptor: ()V
    flags: (0x0001) ACC_PUBLIC
    Code:
      stack=1, locals=1, args_size=1
         0: aload_0
         1: invokespecial #1                  // Method java/lang/Object."<init>":()V
         4: return
      LineNumberTable:
        line 3: 0
```

The constructor declares no formal parameters and returns `void` as shown by the descriptor. It does require a single parameter however - the
reference to the new object being initialised. This is loaded onto the operand stack with the [aload_0](https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-6.html#jvms-6.5.aload_n)
instruction. The `java.lang.Object` constructor is then invoked in the same way, passing the reference just loaded as the first argument. The [invokespecial](https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-6.html#jvms-6.5.invokespecial) instruction
is similar to `invokevirtual` and `invokestatic` but is required to invoke constructors and superclass methods. The operand to the `invokespecial` instruction is an index into the constant pool
of the `test.Hello` class. The element at index `#1` is a method reference to the `java/lang/Object.<init>` method as indicated by the comment.

### main

```
public static void main(java.lang.String[]);
    descriptor: ([Ljava/lang/String;)V
    flags: (0x0009) ACC_PUBLIC, ACC_STATIC
    Code:
      stack=2, locals=1, args_size=1
         0: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
         3: ldc           #13                 // String Hello world!
         5: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
         8: return
      LineNumberTable:
        line 5: 0
        line 6: 8
```

As indicated by the descriptor, the `main` method defines a single `String[]` parameter and has a return type of `void`. 
The flags also indicate the method is `static` and `public`.

The [getstatic](https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-6.html#jvms-6.5.getstatic) instruction loads a field reference onto the operand stack. The operand of `#7` is the index of the reference to the
`System.out` field within the constant pool of the `test.Hello` class.

The String "Hello world!" is pushed onto the operand stack with the [ldc](https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-6.html#jvms-6.5.ldc) instruction. The operand of
`#13` is the index of the String within the class constant pool.

At this point the operand stack contains the arguments to the `java.io.PrintStream.<println>` method - the receiver (the contents of the `System.out` field), and the String to write. The
method is invoked with [invokevirtual](https://docs.oracle.com/javase/specs/jvms/se20/html/jvms-6.html#jvms-6.5.invokevirtual) using the method reference in the class constant pool.
