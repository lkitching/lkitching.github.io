---
layout: post
title: Dependency management
---

We saw previously how libraries can be packaged into JAR files for easier distribution. After packaging our message library as
a JAR, our chief architect is so impressed they immediately mandate its use across all projects at our company Picosoft. It proves
so popular we're inundated with feature requests from other teams. One such request is to support reading messages from a JSON file
containing a string array e.g.

```json
["Hello", "world", "!"]
```

We're confident our existing `MessageSource` interface can support this use case, but don't want to write our own JSON parser. As conscientious
professionals, we embark on a detailed evaluation of the available options, and settle on the first Google search result - [JSON Java](https://github.com/stleary/JSON-java).

We fetch the JSON library JAR locally and begin work:

     > curl -L -o json-java.jar https://search.maven.org/remotecontent?filepath=org/json/json/20230618/json-20230618.jar

After a frenetic caffeine-fueled 10-hour coding session, we finally have our new message source implementation:

__src/libhello/JSONMessageSource.java__
```
{% include code/java_dependencies/JSONMessageSource.java %}
```

As before, we compile the library, updating the build classpath to include the dependency JAR:

    > javac -cp json-java.jar -d classes/libhello src/libhello/*.java
    > jar --create --file libhello.jar -C classes/libhello .

We write a small test application and data file for the new source

__EchoJSON.java__
```java
{% include code/java_dependencies/EchoJSON.java %}
```

__messages.json__
```json
{% include code/java_dependencies/messages.json %}
```

and run it to check the input messages are displayed as expected:

    > javac -cp json-java.jar:libhello.jar -d app EchoJSON.java
    > java -cp json-java.jar:libhello.jar:app test.EchoJSON messages.json

Satisfied with another job well done, we email the updated JAR to the chief scrum master of the originating team for the request.

Shortly after, we receive a response complaining that when running their application, they receive the following error:

```
Exception in thread "main" java.lang.NoClassDefFoundError: org/json/JSONTokener
	at enterprisey.JSONMessageSource.fromFile(JSONMessageSource.java:21)
```

This occurs because, when running their application, the other team has not placed `json-java.jar` on the classpath, and this defines
the `org.json.JSONTokener` class. This is not surprising, since given the `libhello.jar` file, there is no way to detect that it depends
on `json-java.jar` at runtime. JAR files are just an archive of class files and do not define a mechanism for declaring which other JARs
define dependency classes.

In addition, the other team has a few other requirements for consuming our library:

* They would prefer not to commit binary files to their source control repository.
* They would like to be able to identify the library version from the JAR file name.
* They need to be able to identify all dependency JARs so they can fetch them and construct the correct classpath to run their application.

## Maven

[Maven](https://maven.apache.org) is a tool for managing Java projects. It defines a standard project layout and built-in tools for common
project operations such as running tests and building JAR files. It also defines the format for [binary repositories](https://maven.apache.org/repository/layout.html)
where JARs can be published and retrieved, along with their dependencies. 

### POM files

Maven projects are defined by a Project Object Model (POM) file in XML format called `pom.xml`. This defines all aspects of the project, such as
where to find source and test files, build and packaging information, and dependency definitions. All project `pom.xml` files implicitly inherit
from a '[super POM](https://maven.apache.org/ref/3.0.4/maven-model-builder/super-pom.html)' file defined by the Maven distribution. This defines
the standard layout of a Maven project.

### Dependencies

Most projects make use of external libraries, which often in turn declare dependencies of their own. As we've seen, manually managing these dependencies
and putting them on the classpath rapidly becomes impractical, even for small projects. Maven allows projects to declare which libraries they depend on,
and manages fetching these locally and constructing the required classpath. Dependencies are identified by a set of _coordinates_ which can be used to
locate them within a repository they're published to. These consist of three main components:

* Group - The entity or organisation responsible for maintaining and publishing the artifact. These are usually identified by a reversed domain name controlled by the publisher, such as `org.apache` or `com.google`.
* Artifact - The name of the artifact, such as `clojure` or `guava` 
* Version - The version of the artifact to use

Taken together, the Group, Artifact and Version (often abbreviated GAV) are usually enough to uniquely identify the dependency artifact. Non-standard dependencies can
specify other components such as a classifier or different type - see [Maven Coordinates](https://maven.apache.org/pom.html#Maven_Coordinates) in the POM reference for the full details.

### Repositories

In order to make artifacts available to others, the responsible organisation publishes them to a _repository_. Maven repositories have a [defined layout](https://cwiki.apache.org/confluence/display/MAVENOLD/Repository+Layout+-+Final)
which can be used to locate an artifact based on its coordinates. For most artifacts, the location is given by:

    /$groupId[0]/../${groupId[n]/$artifactId/$version/$artifactId-$version.$extension

where `$groupId[0]..groupId[n]` are the dot-separated components of the publisher's groupId. For example artifacts for the `org.clojure` group will be published under `org/clojure`. The `$extension` component is
determined by the artifact packaging type - this is usually `jar` for Java artifacts. For example, the `data.json` project is published by the `org.clojure` group, so the JAR for version `0.2.6` would be located within a repository at:

    org/clojure/data.json/0.2.6/data.json-0.2.6.jar

#### Maven Central

Maven defines a single repository `central` in the [super POM](https://maven.apache.org/ref/3.0.4/maven-model-builder/super-pom.html). This is located at `http://repo.maven.apache.org/maven2` and it means that Maven
projects can reference dependencies published to this repository without any further configuration.

#### Local repositories

During dependency resolution, Maven fetches any required dependencies and copies them to a local repository. By default, this repository is located at `~/.m2/repository` and follows the same layout as described above.
Therefore, version `0.2.6` of the `data.json` JAR would be fetched to `~/.m2/repository/org/clojure/data.json/0.2.6/data.json-0.2.6.jar`. 

## Maven library development

Now that we understand the basics of Maven we can create a project for our library. First we create a directory for the project and set it up in the
standard Maven layout. As defined in the [super POM](https://maven.apache.org/ref/3.0.4/maven-model-builder/super-pom.html), Java source files go under
`${project.basedir}/src/main/java`.

```
libhello/
  pom.xml
  src/
    main/
      java/
        enterprisey/
          CommandLineMessageSource.java
          JSONMessageSource.java
          ...
```

Our `pom.xml` file defines the coordinates for the library itself along with its dependencies:

**pom.xml**
```xml
{% include code/java_dependencies/maven/pom.xml %}
```

Now we can compile and package the library as a JAR:

    mvn package

The super POM sets the default build directory to `${project.basedir}/target` and if we look there we can see a `libhello-1.0.0.jar` file has been
created.

This can be installed into our local repository with

    mvn install

As we would expect from the library coordinates and the default local repository location, this is installed into `~/.m2/repository/com/enterprisey/libhello/1.0.0/`.
Both the JAR and POM files are published so the dependency information is still accessible.

## Maven application development

With the library published to our local repository, we decide to convert our test application into a Maven project as well. Again we set up the directory
layout and POM files:

```
json-test/
  src/
    main/
      java/
        test/
          EchoJSON.java
  pom.xml
  messages.json
```

**pom.xml**
```xml
{% include code/java_dependencies/app/pom.xml %}
```

The application POM declares a dependency on the version of the library we want to use. As before the JAR can be built with

    mvn package

which creates the JAR `json-test-1.0.0.jar`. When we try to run the application with

    java -cp target/json-test-1.0.0.jar test.EchoJSON messages.json

we run into the same `java.lang.NoClassDefFoundError` error as before (this time for one of our library classes). This is because the
output JAR still only contains the application classes and none of the classes defined by its dependencies. One option is to get the runtime
classpath from Maven and supply that to run the application:

```
mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt
java -cp "$(cat classpath.txt):target/json-test-1.0.0.jar" test.EchoJSON messages.json
```

If we look in the `classpath.txt` file written by the `mvn` command we can see it references all transitive dependencies of the application
which Maven downloaded into our local repository:

**classpath.txt**
```
~/.m2/repository/com/enterprisey/libhello/1.0.0/libhello-1.0.0.jar:~/.m2/repository/org/json/json/20230618/json-20230618.jar
```

Rather than manually construct the runtime classpath, we can use the [exec plugin](https://www.mojohaus.org/exec-maven-plugin/usage.html)
during development:

    mvn exec:java -Dexec.mainClass=test.EchoJSON -Dexec.args="messages.json"

Another option is to build uber JARs using the [Maven shade plugin](https://maven.apache.org/plugins/maven-shade-plugin/index.html).

## Publishing

Now the library and application are working locally, it's time to share the library with our colleagues. Our chief architect is convinced
it constitutes a key competitive advantage for the company and is unwilling to unleash it on an unsuspecting public. She arranges for a private
Maven repository to be set up, and our CI process to publish there instead of the Maven Central repository. We advise any teams wishing to use
it to configure the private repository in their application POM files with the following fragment:

```xml
{% include code/java_dependencies/private_repository.xml %}
```

### Repository settings

Public repositories can be accessed anonymously, but our private repository requires authentication. Since authentication is connected
to a user instead of a project, Maven allows user settings to be configured outside of a project. By default these settings are defined within
a `~/.m2/settings.xml` file. The full format of the file is specified in the [documentation](https://maven.apache.org/settings.html), but for our private
repository, we just need to configure the credentials to use:

**~/.m2/settings.xml**
```xml
{% include code/java_dependencies/settings.xml %}
```