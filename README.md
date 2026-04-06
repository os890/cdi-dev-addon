# CDI-DEV Add-on: Duplicate Class Veto Extension

A CDI portable extension that detects and handles duplicate bean classes on the classpath.

## Description

When multiple JARs on the classpath contain CDI beans with the same fully-qualified class name, the CDI container may discover them more than once during deployment. This extension observes `ProcessAnnotatedType` events and:

- **Vetoes** the duplicate if it originates from the same classpath location and classloader (same class scanned twice).
- **Logs a warning** if the class exists in different JARs (different classpath locations).

## Architecture

- **`addon/`** — The CDI extension (`DuplicateClassVetoExtension`) registered via `META-INF/services/jakarta.enterprise.inject.spi.Extension`.
- **`demo-lib-a/`** and **`demo-lib-b/`** — Two small JARs each containing a `DuplicateBean` with the same FQCN, used to verify duplicate detection in tests.

## Requirements

- Java 25+
- Maven 3.6.3+
- CDI 4.1 (Jakarta EE 11)

## Usage

Add the addon as a dependency:

```xml
<dependency>
    <groupId>org.os890.cdi.addon</groupId>
    <artifactId>cdi-duplicated-class-veto</artifactId>
    <version>1.0.0</version>
</dependency>
```

The extension is automatically discovered via the Java `ServiceLoader` mechanism.

## Building

```bash
mvn clean verify
```

## Quality Plugins

The build enforces:

- **Compiler**: `-Xlint:all`, fail on warnings
- **Enforcer**: Java 25+, Maven 3.6.3+, dependency convergence, banned `javax.*` dependencies
- **Checkstyle**: No star imports, brace style, whitespace rules — applied to main and test sources
- **RAT**: Apache License 2.0 header verification on all source and config files
- **JaCoCo**: Code coverage reporting
- **Surefire**: Test execution with pinned version

## Testing

Tests use the [Dynamic CDI Test Bean Addon](https://github.com/os890/dynamic-cdi-test-bean-addon) (`@EnableTestBeans`) with OpenWebBeans SE to boot a CDI container and verify the extension's duplicate detection behavior.

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
