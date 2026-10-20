# Module 01 — Inversion of Control and Dependency Injection

## Learning goals

By the end, be able to explain the IoC principle, identify a dependency graph, wire components using constructor injection, choose between interfaces and implementations, and test a service without booting Spring.

## 1. The vocabulary

- **Object:** a runtime instance of a Java class.
- **Dependency:** another object a class needs to perform its job.
- **IoC (Inversion of Control):** the class does not fully control the construction and lookup of the collaborators it uses; responsibility is delegated to an external mechanism/container.
- **DI (Dependency Injection):** a technique for implementing IoC: dependencies are provided to an object through constructor arguments, setters, or factory method arguments.
- **Spring bean:** an object instantiated, assembled, and managed by a Spring IoC container.
- **Container:** the Spring machinery that reads configuration metadata, creates bean instances, resolves their dependencies, and manages their lifecycle.

IoC is the principle; DI is a technique. They are related but are not synonyms.

## 2. Why direct construction causes coupling

Consider `new DefaultGreetingProvider()` inside `GreetingService`. The service chooses the concrete implementation and controls its construction. If the implementation changes, or we want a fake for tests, the service must change too.

This lab instead depends on the `GreetingProvider` interface. `GreetingService` doesn't know which concrete implementation fulfils the contract. Spring discovers `DefaultGreetingProvider` and supplies it to the service constructor.

Dependency graph:

```text
HTTP request
    |
    v
GreetingController  --constructor injection--> GreetingService
                                                |
                                                +--constructor injection--> GreetingProvider
                                                                               ^
                                                                               |
                                                                    DefaultGreetingProvider
                                                                    (@Component implementation)
```

`@RestController` and `@Service` are component stereotypes. `@Component` marks the implementation for component scanning. When the application starts, component scanning finds these classes and the container resolves dependencies by type.

## 3. Constructor injection

```java
@Service
public class GreetingService {
    private final GreetingProvider greetingProvider;

    public GreetingService(GreetingProvider greetingProvider) {
        this.greetingProvider = Objects.requireNonNull(greetingProvider);
    }
}
```

Why prefer constructor injection for required dependencies?

1. Required dependencies are visible in the constructor signature.
2. `final` fields support an immutable dependency reference after construction.
3. A normal Java unit test can instantiate the class directly.
4. The object cannot be returned to its caller in a partially configured state if construction requires all dependencies.
5. Missing dependencies tend to fail early during context startup.

When a class has exactly one constructor, Spring can use it without `@Autowired`. The annotation is still useful in specific cases such as selecting among multiple constructors or marking injection points when needed.

## 4. Setter and field injection

Setter injection can fit optional dependencies or cases where late reconfiguration is meaningful. Required setter dependencies can leave an object invalid until the setter is called, so constructor injection is usually simpler for mandatory collaborators.

Field injection (`@Autowired private SomeService service`) hides the dependency in the class body, makes the field difficult to assign in a plain unit test, and prevents straightforward `final` dependency references. Prefer constructor injection in application services.

## 5. Interface versus implementation

Depending on an interface separates policy from implementation. `GreetingService` depends on what a provider can do, not on how it does it. Other implementations could later format greetings differently, call another service, or return localized text without changing the service's constructor contract.

Spring still needs an unambiguous candidate. If two beans implement `GreetingProvider`, injection by type may fail with a non-unique-bean error. Later, learn `@Primary`, `@Qualifier`, and profile-specific beans to resolve that situation intentionally.

## 6. @Component, @Service, @RestController and @Bean (first pass)

- `@Component`: general stereotype discovered by component scanning.
- `@Service`: service-layer stereotype; it is also a component.
- `@RestController`: controller stereotype with response-body semantics for its handler methods.
- `@Bean`: method-level declaration inside configuration, where the method's return value is managed as a bean. This is often useful for third-party classes or explicit setup.

Stereotype annotations communicate intent and support scanning; they do not themselves mean an object is globally available unless component scanning/configuration registers it in the container.

## 7. What Spring does at a high level

1. `SpringApplication.run(...)` starts Boot's application bootstrap.
2. Boot prepares the environment and application context.
3. Component scanning/configuration registers bean definitions.
4. The context creates required beans and resolves constructor arguments from candidate beans.
5. With the web starter, Boot configures a web application and embedded server.
6. The application becomes ready to handle requests.

This is a conceptual sequence, not every internal startup callback. We'll inspect auto-configuration and bean lifecycle in later learning steps.

## 8. Hands-on instructions

From this directory, with JDK 21 and Maven installed:

```powershell
java -version
mvn -version
mvn clean test
mvn spring-boot:run
```

In another PowerShell window:

```powershell
Invoke-RestMethod "http://localhost:8080/api/greeting?name=Gyanesh" | ConvertTo-Json
Invoke-RestMethod "http://localhost:8080/api/greeting" | ConvertTo-Json
```

Expected JSON message values:

```json
{"message":"Hello, Gyanesh! Welcome to Spring Boot."}
{"message":"Hello, there! Welcome to Spring Boot."}
```

Stop the app using Ctrl+C. We will add the lifecycle log probe in the lifecycle section.

## 9. Experiments — don't just read the output

1. Change the constructor of `GreetingService` to accept `DefaultGreetingProvider` rather than `GreetingProvider`. What flexibility do you lose? Return it to the interface after testing.
2. Temporarily remove `@Component` from `DefaultGreetingProvider`. Run the app. Record the exception and explain why `GreetingService` cannot be created.
3. Add a second `@Component` implementation of `GreetingProvider`. Observe the ambiguous-injection failure. Do not leave the project in that state; we'll resolve it later with `@Primary` or `@Qualifier`.
4. Run only `GreetingServiceTest`. Notice that it doesn't start a Spring context. Explain why constructor injection makes that possible.
5. Change the fake provider in a test. The service behavior is tested without changing application production code.

## 10. Completion criteria

- [ ] Explain IoC versus DI without mixing their definitions.
- [ ] Draw the application's dependency graph.
- [ ] Explain why constructor injection is preferred for required dependencies.
- [ ] Run unit and integration tests and understand the difference.
- [ ] Reproduce and explain a missing-bean error.
- [ ] Explain the roles of `@Component`, `@Service`, `@RestController`, and `@Bean`.
