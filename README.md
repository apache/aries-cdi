# Aries CDI Integration

This is an implementation of [OSGi CDI Integration Specification ](https://osgi.org/specification/osgi.enterprise/7.0.0/service.cdi.html) (hereafter referred to simply as _OSGi CDI_).

## License

[Apache License Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Building From Source

The build uses maven so it should look pretty familiar to most developers.

`mvn clean install`

## Depedencies

The main artifact is the __CDI Component Runtime__ (__CCR__) implementation. a.k.a. the _extender_ bundle:

```xml
<dependency>
  <groupId>org.apache.aries.cdi</groupId>
  <artifactId>org.apache.aries.cdi.extender</artifactId>
  <version>${aries-cdi.version}</version>
  <scope>runtime</scope>
</dependency>
```

However all the required dependencies are available using the __Aries CDI BOM__:

```xml
<dependency>
    <groupId>org.apache.aries.cdi</groupId>
    <artifactId>org.apache.aries.cdi.bom</artifactId>
    <version>${aries-cdi.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

## Tooling

TODO

## Pre-built runtime

This repository provides an example for how to assemble an executable jar providing a complete runtime for you to just drop in your CDI bundles. It comes complete with logging, Gogo shell, Config Admin, Http Whiteboard support, and OSGi Promises.

Once you've completed a successfull build, you should be able to execute the command:

`java -jar cdi-executable/target/executable.jar`

and be presented with a gogo shell prompt ready for you to install a CDI bundle.

## Architecture Overview

The goal of OSGi CDI was to remain as true to both technologies as possible. This proved possible due to the extensive feature set provided by each technology.

### Who cares! Examples please

[Examples](examples.md)

### Actors

The main actors in the OSGi CDI architecture are:

* __CDI bundle__ - bundles which contain CDI beans __and__ opted-in to OSGi CDI (best achieved with supporting build [tooling](#tooling).)

* __CDI container__ - an instance of the CDI machinery hosting all beans inside a bundle and managing their instantiation.

* __CDI Component Runtime__ (__CCR__) - is what __Aries CDI__ implements using [the extender pattern](https://enroute.osgi.org/FAQ/400-patterns.html#extender-pattern). It awaits CDI bundles creating a _private_ CDI container for each one.

* __OSGi CDI Components__ (hereafter referred to simple as _components_) - A set of closely related CDI beans having a common _OSGi lifecycle_. A CDI bundle has __1 to N__ _components_. Again, all beans within the same component have a common OSGi lifecycle within the CDI bundle. The collective dependencies declared by all bean making up a component are treated as a single set. As such any single unsatisfied dependency of the component will prevent the entire component from activating, or upon removal, will cause the component to deactivate.

* __OSGi CDI Portable Extension__ (hereafter referred to simply as _portable extensions_) - bundles which contain portable extensions __and__ opted-in to providing those extensions in a OSGi CDI compatible way.

* __Service Registry__ - The OSGi service registry is the central actor by which all inter bundle service activity is managed. As such, CDI bundles interact with other bundles via the service registry as well.

    > _The nice thing is you can mix and match through the lingua franca of services. A bundle that is internally implemented with DS can talk to a bundle that is internally implemented with CDI (or Blueprint, etc...)_ [Neil Bartlett - Twitter](https://twitter.com/nbartlett/status/1114148717911859202)

* __Configuration Admin__ - OSGi CDI is well integrated with Configuration Admin the way that __Declarative Services__ (__DS__) is. As such, __all__ _components_ in CDI bundles are configurable via configuration admin.

### How a CDI bundle is created

When a CDI bundle is identified by CCR several steps are taken before any bean is instantiated:

1. Any _portable extensions_ identified by the bundle must be discovered and their associated `javax.enterprise.inject.spi.Extension` services must be located. The bundle's CDI container will remain inactive until all portable extension services are located. Conversely, for a bundle with an active CDI container, if an identified extension goes away the CDI container is torn down.
2. The beans of the bundle are analysed and categorised into 3 classifications:
   1. __Container component__:
      - All beans you would traditionally find in a CDI application; `ApplicationScoped`, `Dependent`, `RequestScoped`, `SessionScoped`, `ConversationScoped`, any custom scopes, etc.; all of these make up the _container component_. 
      - In fact, all beans that are not specifically `org.osgi.service.cdi.annotations.ComponentScoped` are part of the _container component_.
      - __Every__ CDI bundle has exactly __1__ _container component_. 
      - It is perfectly valid for the set of _container component_ beans to be __empty__.
   2. __Single component__: 
      - All beans using the _stereotype_ `@SingleComponent` are roots of _a single component_.
      - Any referred beans (via injection points) that are explicitly scoped `@ComponentScoped` are also part of this _single component_.
      - Each _single component_ in a bundle has an __independent__ OSGi lifecycle, with one restriction; the _container component_ __must__ be active.
      - if the _container component_ becomes unresolved, active _single components_ are deactivated.
      - A bundle may have __0 to N__ _single components_.
      - _Single components_ are directly analogous to DS components that __are not__ flipped to factory mode.
   3. __Factory component__:
      - All beans using the stereotype `@FactoryComponent` are roots of a _factory component_.
      - Any referred beans (via injection points) that are explicitly scoped `@ComponentScoped` are also part of this _factory component_.
      - Each _factory component_ in a bundle has an __independent__ OSGi lifecycle, with one restriction; the _container component_ __must__ be active.
      - if the _container component_ becomes unresolved, active _factory components_ are deactivated.
      - A bundle may have __0 to N__ _factory components_.
      - _Factory components_ are directly analogous to DS components that __are__ flipped to factory mode.
      - _Factory components_ are dependent on factory configuration instances.
3. The bundle's CDI container remains inactive while there remain unsatisfied dependencies of the _container component_. These may be services or configurations.
4. Once the ___container component___  is resolved:
   1. CDI container is created and activated.
   2. the application scope is activated (_if there are any such beans in the container component_.)
   3. services provided by the _container component_ are published to the service registry (_if any_.)
   4. The `javax.enterprise.inject.spi.BeanManager` of the CDI container is published as a service with the service property `osgi.cdi.container.id`. (_always, even if the container component is empty_.)
   5. _single components_ and _factory components_ remain inactive while there remain unsatisfied dependencies. These may be services or configurations.
   6. Once a ___single component___  is resolved:
      1. it becomes active; the exact nature of which is determined by whether the component provides a service or not, and what the service scope is.
         - if the component __does not provide a service__, the component is simply instantiated.
         - if the component __provides a singleton scoped service__, the component is instantiated and published into the registry (wrapped in a `ServiceFactory` like DS component services)
         - if the component __provides a bundle scoped service__, the component is published into the registry (wrapped in a `ServiceFactory`). Service instances are created whenever the `getService` method of the factory is called, and destroyed when the `ungetService` is called. __Note:__ The service registry is the one tracking if a bundle has already _gotten_ factory service instances.
         - if the component __provides a prototype scoped service__, the component is published into the registry (wrapped in a `PrototypeServiceFactory`). Service instances are created whenever the `getService` method of the factory is called, and destroyed when the `ungetService` is called.
      2. if any required dependency of the component goes away, any service registration is removed from the registry and all instances are destroy.
      3. Note that CDI context events whose payload is the component instance are fired at the appropriate moment for each of: 
         - `@Initialized(ComponentScoped.class)`
         - `@BeforeDestroy(ComponentScoped.class)`
         - `@Destroyed(ComponentScoped.class)`
   7. Once a ___factory component___  is resolved (_when a factory configuration instance is created in addition to all other service & configuration dependencies_):
      1. an instance becomes active; the exact nature of which is determined by whether the component provides a service or not, and what the service scope is.
         - if the component __does not provide a service__, the component is simply instantiated.
         - if the component __provides a singleton scoped service__, the component is instantiated and published into the registry (wrapped in a `ServiceFactory` like DS component services)
         - if the component __provides a bundle scoped service__, the component is published into the registry (wrapped in a `ServiceFactory`). Service instances are created whenever the `getService` method of the factory is called, and destroyed when the `ungetService` is called. __Note:__ The service registry is the one tracking if a bundle has already _gotten_ factory service instances.
         - if the component __provides a prototype scoped service__, the component is published into the registry (wrapped in a `PrototypeServiceFactory`). Service instances are created whenever the `getService` method of the factory is called, and destroyed when the `ungetService` is called.
      2. if any required dependency of the component goes away, any service registration is removed from the registry and all instances are destroy.
      3. Note that CDI context events whose payload is the component instance are fired at the appropriate moment for each of: 
         - `@Initialized(ComponentScoped.class)`
         - `@BeforeDestroy(ComponentScoped.class)`
         - `@Destroyed(ComponentScoped.class)`

Time to move onto [the examples](examples.md).