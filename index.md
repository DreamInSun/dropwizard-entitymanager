---
layout: default
---

# Getting Started

If you're using Maven, simply add the `dropwizard-entitymanager` dependency to your POM:

```xml
<dependency>
  <groupId>com.scottescue</groupId>
  <artifactId>dropwizard-entitymanager</artifactId>
  <version>0.9.0-1</version>
</dependency>
```

<div class="alert alert-info" role="alert"> 
  <div><strong>Note</strong></div> Dropwizard Hibernate's <strong>@UnitOfWork</strong> annotation 
  is bundled within this library for convenience.  There is no need to add a dropwizard-hibernate 
  dependency. 
</div>


# Configuration

First, your configuration class needs a `DataSourceFactory` instance:

```java
public class ExampleConfiguration extends Configuration {
    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }
}
```

Then, add an `EntityManagerBundle` instance to your application class, specifying your entity classes and how to get a 
`DataSourceFactory` from your configuration subclass:

```java
private final EntityManagerBundle<ExampleConfiguration> entityManagerBundle = 
        new EntityManagerBundle<ExampleConfiguration>(Person.class) {
    @Override
    public DataSourceFactory getDataSourceFactory(ExampleConfiguration configuration) {
        return configuration.getDataSourceFactory();
    }
};

@Override
public void initialize(Bootstrap<ExampleConfiguration> bootstrap) {
    bootstrap.addBundle(entityManagerBundle);
}

@Override
public void run(ExampleConfiguration config, Environment environment) {
    final EntityManager entityManager = entityManagerBundle.getSharedEntityManager();
    environment.jersey().register(new UserResource(entityManager));
}
```

This will create a new managed connection pool to the database, a health check for connectivity to the database, and 
a new `EntityManagerFactory` as well as a thread-safe `EntityManager` instance for you to use in your classes.


# Usage

## Container Managed PersistenceContext
The shared `EntityManager` obtained from your `EntityManagerBundle` works with the `@UnitOfWork` annotation from the 
Dropwizard Hibernate module.  The `@UnitOfWork` annotation may be applied to resource methods to create a container 
managed `PersistenceContext`.  This gives you the ability to declaratively scope transaction boundaries.  The 
annotation _must_ be present on any resource method that either directly or indirectly uses the shared `EntityManager`.

```java
@POST
@Timed
@UnitOfWork
public Response create(@Valid Person person) {
    entityManager.persist(checkNotNull(person));

    return Response.created(UriBuilder.fromResource(PersonResource.class)
            .build(person.getId()))
            .build();
}
```

This will automatically initialize the `EntityManager`, begin a transaction, call persist, commit the transaction, and 
finally close the `EntityManager`. If an exception is thrown, the transaction is rolled back.

Often you simply need to read data without requiring an actual transaction.
 
```java
@GET
@Timed
@UnitOfWork(transactional = false)
public Person findPerson(@PathParam("id") LongParam id) {
    return entityManager.find(Person.class, id.get());
}
```

This will automatically initialize the `EntityManager`, call find, and finally close the `EntityManager`.

<div class="alert alert-info" role="alert"> 
  <div><strong>Important</strong></div> The EntityManager is closed before your resource method’s return value (e.g., the Person from the database), 
which means your resource method is responsible for initializing all lazily-loaded collections, etc., 
before returning. Otherwise, you’ll get a `LazyInitializationException` thrown in your template (or null values 
produced by Jackson). 
</div>


## Application Managed PersistenceContext
There may be times when you need to have more control over the `PersistenceContext` or need to manage a new transaction.  
The `EntityManagerFactory` obtained from your `EntityManagerBundle` allows you to create and manage new 
`EntityManager` instances.  Any `EntityManager` created from the factory will have a new `PersistenceContext` 
independent of any `@UnitOfWork` context or transaction.

```java
public void create(Person person) {
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();
    try {
        transaction.begin();
        entityManager.persist(person);
        transaction.commit();
    } catch (RuntimeException e) {
        if (transaction.isActive()) {
            transaction.rollback();
        }
    } finally {
        entityManager.close();
    }
}
```

# Prepended Comments

By default, `dropwizard-entitymanager` configures Hibernate JPA to prepend a comment describing the context of all 
queries:

```sql
/* load com.example.helloworld.core.Person */
select
    person0_.id as id0_0_,
    person0_.fullName as fullName0_0_,
    person0_.jobTitle as jobTitle0_0_
from people person0_
where person0_.id=?
```

This will allow you to quickly determine the origin of any slow or misbehaving queries.  See the Database - 
autoCommentsEnabled attribute in the [Dropwizard Configuration Reference](http://www.dropwizard.io/0.9.0/docs/manual/configuration.html) 


# Support

Please file bug reports and feature requests in [GitHub issues](https://github.com/scottescue/dropwizard-entitymanager/issues).

# Credits

This module is heavily derived from Dropwizard Hibernate. Those who have contributed to Dropwizard Hibernate deserve 
much of the credit for this project. I've essentially adapted their work to create and expose the `EntityManager` and 
`EntityManagerFactory` objects.

Dropwizard is developed by Coda Hale; Yammer, Inc.; and the Dropwizard Team, licensed under the Apache 2.0 license.

# License

Copyright 2015-2016 Scott Escue

This library is licensed under the Apache License, Version 2.0. See the project's [LICENSE](https://github.com/scottescue/dropwizard-entitymanager/blob/master/LICENSE) file for the full license text.

