# OSGi CDI Examples as an FAQ

## Registering Services

### How do I register a bean as a service?

The simplest service is a bean annotated with `@Service`.

```java
package com.acme;

@Service
class Foo {
}
```

### Under which service types are my `@Service` beans published?

Unless otherwise specified, `@Service` beans are published as follows:

1. if the bean _does not implement any interfaces_, the service type is __the bean type__.
2. if the bean _implements any interfaces_, the service types are __all implemented interfaces__.

The service above is registered with the service type `com.acme.Foo`.

```java
import com.acme.Bar;
import com.acme.Baz;

@Service
class Foo implements Bar, Baz {
}
```

The service above is registered with the service types `com.acme.Bar` and `com.acme.Baz`.

### How do I specify service types for my `@Service` beans?

There are several ways to specify which service types `@Service` beans are published.

1. One or more types my be specified by applying the `@Service` annotation to type use clauses.
2. The `@Service` annotation on the bean type may specify a set of types as an argument of the annotation.

```java
import com.acme.Bar;
import com.acme.Baz;
import com.acme.Fum;

class Foo extends @Service Fum implements @Service Bar, Baz {
}
```

The above example specifies that the bean should be published with service types `com.acme.Fum` and `com.acme.Bar`, but NOT `com.acme.Baz`.

```java
import com.acme.Bar;
import com.acme.Baz;
import com.acme.Fum;

@Service({Fum.class, Bar.class})
class Foo extends Fum implements Bar, Baz {
}
```

This example produces the same result as the previous example.

### At what `service.scope` will my `@Service` beans is registered?

Unless otherwise specified, `@Service` beans have __`singleton`__ `service.scope`.

The example in the previous section produces a service at __`singleton`__ scope.

### How do I specify a `service.scope` on my `@Service` beans?

The annotation `@ServiceInstance` is used to specify the `service.scope`.

```java
@Service
@ServiceInstance(PROTOTYPE)
class Foo {
}
```

Available values are `SINGLETON`, `BUNDLE`, and `PROTOTYPE`.

### Are there any limitations as to which beans can become services?

Beans scoped `@ApplicationScoped` or `@Dependent`, as well as beans having the stereotype `@SingleComponent` or `@FactoryComponent` can be annotated with `@Service` and become services.

```java
@Service     // Valid! @Dependent & singleton service
class Foo {
}

@Service     // Valid! @ApplicationScoped & singleton service
@ApplicationScoped
class Foo {
}

@Service     // Valid! @ComponentScoped & prototype service
@ServiceInstance(PROTOTYPE)
@SingleComponent
class Foo {
}

@Service     // ERROR! Only @ApplicationScoped, @Dependent, @SingleComponent & @FactoryComponent can be services.
@SessionScoped
class Foo {
}

@Service     // ERROR! @ApplicationScoped can only have singleton scope.
@ServiceInstance(PROTOTYPE)
@ApplicationScoped
class Foo {
}
```

### Are there any limitations to which `service.scope` `@Service` beans can have?

Beans scoped `@Dependent` (_default when no scope annotation is defined on the bean_), and beans having the stereotype `@SingleComponent` or `@FactoryComponent` can have any `service.scope`.

Beans scoped as `@ApplicationScoped` may only have `service.scope` __`singleton`__. However, such beans may directly implement `ServiceFactory` or `PrototypeServiceFactory` in order to provide service instances at __`bundle`__, or __`prototype`__ `service.scope` respectively.

### How do I add service properties to my `@Service` beans?

Service properties may be added to `@Service` beans by annotating them with annotations that are meta-annotated using `@BeanPropertyType`. These annotations are then coerced into service properties following a predefined set of coercion rules as defined in the specification.

There are a number of predefined `@BeanPropertyTypes` to handle common cases, such as `@ServiceRanking`, `@ServiceDescription`, `@ServiceVendor` and `@ExportedService`.

```java
@Service
@ServiceRanking(100)
class Foo {
}
```

In addition to these, Aries CDI provides an additional suite of __BeanPropertyTypes__ in the dependency `org.apache.aries.cdi.extra`:

```xml
<dependency>
  <groupId>org.apache.aries.cdi</groupId>
  <artifactId>org.apache.aries.cdi.extra</artifactId>
  <version>${aries-cdi.version}</version>
</dependency>
```

In all there exist _BeanPropertyTypes_ for the __Http Whiteboard__, __JAXRS Whiteboard__, __Event Admin__ and __Remote Service Admin__ specifications.

### How do I add service properties that don't exist as a predefined `BeanPropertyType`?

Creating your own BeanPropertyTypes is very simply meta-annotating a _runtime_ annotation with `@BeanPropertyType` (since CDI uses runtime annotation processing, the annotations must have runtime retention).

```java
@Retention(RUNTIME)
@BeanPropertyType
public @interface Config {
    int http_port() default 8080;
}
```

### Can I add Metatype annotations to my `BeanPropertyTypes`?

Adding metatype annotations to your BeanPropertyTypes is the recommended way of providing a schema for your configuration(s).

```java
@Retention(RUNTIME)
@BeanPropertyType
@ObjectClassDefinition(
    localization = "OSGI-INF/l10n/member",
    description = "%member.description",
    name = "%member.name"
    icon = @Icon(resource = "icon/member-32.png", size = 32)
)
public @interface Member {
    @AttributeDefinition(
        type = AttributeType.PASSWORD,
        description = "%member.password.description",
        name = "%member.password.name"
    )
    public String _password();

    @AttributeDefinition(
        options = {
            @Option(label = "%strategic", value = "strategic"),
            @Option(label = "%principal", value = "principal"),
            @Option(label = "%contributing", value = "contributing")
        },
        defaultValue = "contributing",
        description = "%member.membertype.description",
        name = "%member.membertype.name"
    )
    public String type();
}
```

### Can I provide services using producer methods or fields?

TODO

## Referencing Services

### How do I get a service from the registry into my bean?

The simplest form of getting a service into a bean is with the `@Reference` annotation (simple called a _reference_).

```java
@Inject
@Reference
Bar bar; // using field injection

// OR

private final Bar bar;

@Inject
public Fum(@Reference Bar bar) { // using constructor injection
    this.bar = bar;
}

// OR

private Bar bar;

@Inject
public void addBar(@Reference Bar bar) { // using method injection
    this.bar = bar;
}

```

### I want to get the service properties of the referenced service. Are there other service representations I can get in my reference that can help me?

There are a number of service representations that can be injected most of which provide a facility for getting the service properties:

* `S` - where `S` is the raw service type, this is the most basic form of reference

  ```java
  @Inject
  @Reference
  Person person;
  ```

* `ServiceReference<S>` - you can get the service properties directly from the `ServiceReference` interface

  ```java
  @Inject
  @Reference
  ServiceReference<Person> personReference;
  ```

* `Map<String, Object>` - the service properties in `Map` form. Notice in this scenario that the reference must be qualified by a service type. (_This can also be used in other scenarios to narrow the services obtained to a more specific type. However, except in the Map use case, this qualified type __must__ be a subtype of the type specified in the reference._)

  ```java
  @Inject
  @Reference(Person.class)
  Map<String, Object> personProperties;
  ```

* `Map.Entry<Map<String,Object>, S>` - a `Man.Entry` holding the service properties map as key and the service instance as the value.

  ```java
  @Inject
  @Reference
  Map.Entry<Map<String, Object>, Person> personAndProperties;
  ```

* `BeanServiceObjects<S>` - a special type mapping to `ServiceObjects` providing support for prototype scope services. Get the service properties via the `getServiceReference` method on this interface.

  ```java
  @Inject
  @Reference
  BeanServiceObjects<Person> persons;
  ```

### Can I make the reference optional?

Making a reference optional is simply using the `Optional` type around the service type.

```java
@Inject
@Reference
Optional<Person> person;

// OR

@Inject
@Reference
Optional<Map.Entry<Map<String, Object>, Person>> person;
```

### Can I get more than one service in a single reference?

A reference can have multi-cardinality by specifying a container type of `java.util.Collection<R>`, or `java.util.List<R>` where `R` is one of the types specified in the previous sections.

```java
@Inject
@Reference
Collection<Person> persons;

// OR

@Inject
@Reference
List<Map.Entry<Map<String, Object>, Person>> persons;

```

### What is the default minimum cardinality of multi-cardinality references?

Unless otherwise specified, the minimum cardinality is zero (0) making multi-cardinality references effectively _optional_ by default.

```java
@Inject
@Reference
List<Map.Entry<Map<String, Object>, Person>> persons; // min cardinality is 0, therefore this will resolve when no person services exist
```

### Can I specify a minimum cardinality when using a multi-cardinality reference?

In order to specify a minimum cardinality use the `@MinimumCardinality` annotation on a multi-cardinality reference.

```java
@Inject
@MinimumCardinality(3)
@Reference
List<Map.Entry<Map<String, Object>, Person>> persons;
```

### Can I make a multi-cardinality reference optional?

Simply do not provide a minimum cardinality.

### How do I specify a default target filter for the referenced service?

There are a number of ways to specify a target filter for references:

1. specify a service filter in the `target` property on the `@Reference` annotation.
2. specify any number of BeanPropertyTypes on the reference all of which will be AND'ed together in the order they are defined (_after having been appended to any specified `target` value_.)

```java
@Inject
@Reference(target = "(&(foo=bar)(service.vendor=Acme, Ltd.))")
Collection<Dog> dogs;

// OR

@Inject
@Reference(target = "(foo=bar)")
@ServiceVendor("Acme, Ltd.")
Collection<Dog> dogs;
```

Both of the above produce the same target filter.

### Can I track unknown service types that match a target filter?

Tracking any and all *service types* in a reference is supported providing the following criteria are met:

1. `@Reference.value` must specify the single value `Reference.Any.class`.
2. `@Reference.target` must specify a valid, non-empty filter value.
3. The reference *service type* must be `java.lang.Object`.

```java
@Inject
@Reference(value = Reference.Any.class, target = "(foo=bar)")
List<Map.Entry<Map<String, Object>, Object>> fooAreBars;
```

###### TODO

* add links on types throughout
* dynamic references
* portable extensions
* tooling on README

### 