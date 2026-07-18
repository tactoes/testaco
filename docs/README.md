# Testaco Documentation

This directory contains comprehensive documentation for Testaco users and contributors.

## For Users

- **[Usage Guide](usage.md)** — Detailed usage examples, patterns, and best practices
- **[Schema Reference](schema.md)** — JSON schema file format and validation rules
- **[Schema Evolution](schema-evolution.md)** — Maintaining datasets as your database schema changes
- **[Debugging Failing Tests](debugging.md)** — Inspecting and comparing actual vs expected datasets
- **[Gradle Tasks Reference](gradle-tasks.md)** — Complete command reference for schema management tasks

## For Contributors

- **[Releasing Guide](releasing.md)** — Sonatype OSSRH setup and Maven Central publication process
- **[Contributing Guidelines](../CONTRIBUTING.md)** — How to contribute, PR process, code style expectations

## Getting Help

- **Issues:** Report bugs or request features on [GitHub Issues](https://github.com/tactoes/testaco/issues)
- **Discussions:** Ask questions in [GitHub Discussions](https://github.com/tactoes/testaco/discussions)
- **Security:** Report vulnerabilities using [GitHub Security Advisories](https://github.com/tactoes/testaco/security/advisories)

## Documentation Versions

Currently, documentation covers the latest development version (`main` branch). After v0.1.0 release, versioned documentation will be available at `docs.testaco.org`.

## Contributing to Docs

- Documentation changes should be submitted as pull requests
- Use Markdown for all docs; keep formatting consistent
- Include examples where helpful
- Run `./gradlew build` to ensure no broken links in rendered docs (if applicable)
