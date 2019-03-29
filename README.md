# Aries CDI Integration

This is an implementation of [CDI Integration Specification ](https://osgi.org/specification/osgi.enterprise/7.0.0/service.cdi.html).

## License

[Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Building From Source

The build uses maven so it should look pretty familiar to most developers.

`mvn clean install`

## Depedencies

The main artifact is the CCR (CDI Component Runtime) implementation, or _extender_ bundle:

```
<dependency>
  <groupId>org.apache.aries.cdi</groupId>
  <artifactId>org.apache.aries.cdi.extender</artifactId>
  <version>1.0.1</version>
  <scope>runtime</scope>
</dependency>
```

However all the required dependencies are available using the Aries CDI BOM:

```
<dependency>
    <groupId>org.apache.aries.cdi</groupId>
    <artifactId>org.apache.aries.cdi.bom</artifactId>
    <version>1.0.1</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

## Pre-built runtime

This repository provides an example for how to assemble an executable jar providing a complete runtime for you to just drop in your CDI bundles. It comes complete with logging, Gogo shell, Config Admin, Http Whiteboard support, and OSGi Promises.

Once you've completed a successfull build, you should be able to execute the command:

`java -jar cdi-executable/target/executable.jar`

and be presented with a gogo shell prompt ready for you to install a CDI bundle.
