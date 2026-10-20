# Spring Boot Foundations

A progressive, hands-on Spring Boot project for Java backend and SDE-II/SDE-III interview preparation. Each learning module adds practical code, tests, experiments, and an interview checkpoint.

## Current status

**Module 01 — IoC and Dependency Injection (starter implementation).** This project currently demonstrates component scanning, interface-based dependency inversion, constructor injection, and unit vs integration tests. Bean scopes, lifecycle callbacks, configuration binding, auto-configuration inspection, and Actuator will be added in subsequent modules.

## Stack

- Java 21
- Spring Boot 3.5.16
- Maven
- JUnit 5 / Spring Boot Test / MockMvc

Boot 3.5 is selected for this learning project to align with the Spring Boot 3.x conventions used in the companion `java-performance-lab`. Use documentation that matches the project's version when behavior is version-specific.

## Run locally

Requirements: JDK 21 and Maven 3.9+.

```powershell
java -version
mvn -version
mvn clean test
mvn spring-boot:run
```

Call the service:

```powershell
Invoke-RestMethod "http://localhost:8080/api/greeting?name=Gyanesh" | ConvertTo-Json
```

Expected response:

```json
{"message":"Hello, Gyanesh! Welcome to Spring Boot."}
```

## Learning files

- [`MODULE_01_IOC_DI.md`](MODULE_01_IOC_DI.md): concepts, dependency graph, trade-offs, experiments, and completion criteria.
- [`INTERVIEW_QUESTIONS_MODULE_01.md`](INTERVIEW_QUESTIONS_MODULE_01.md): 20-question checkpoint and scoring rubric.

## Roadmap

1. IoC, dependency injection, bean scopes, bean creation/destruction, ApplicationContext, lifecycle callbacks.
2. Auto-configuration, conditional configuration, starters, component scanning, embedded servers, startup inspection.
3. Externalized configuration, profiles, `@ConfigurationProperties`, validation, environment-specific settings.
4. Actuator health, metrics, info, endpoint security, and production readiness.
5. Integrated startup/bean/configuration investigations, performance and operational trade-offs.

## Repository practice

This is a local learning application, not a production-ready service. Do not place real secrets in configuration files. Run changes locally and record test output before marking a checkpoint complete.
