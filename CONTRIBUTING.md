# Contributing to Testaco

Thanks for your interest in contributing to Testaco. Contributions are welcome via GitHub Issues and Pull Requests. Please follow these guidelines to make the process smooth.

- Issues
- Use the appropriate [issue template](https://github.com/tactoes/testaco/issues/new/choose) (bug report, feature request, or question)
- Provide a minimal reproduction and environment information
- For security vulnerabilities, use GitHub Security Advisories as described in [SECURITY.md](SECURITY.md)
- Discussions are enabled for general discussion; prefer Issues for bug reports and feature requests

- Pull Requests
  - Fork the repository (or create a branch) and open a topic branch named `feat/<short-desc>` or `fix/<short-desc>`.
  - Run the test suite before submitting: `./gradlew test`.
  - Ensure all new code includes appropriate tests and license headers (see LICENSE and code header requirements).
  - In your PR description: include motivation, a short description, references to any related issue (use `Fixes #NN` to auto-close), and the testing steps.
  - Use the PR template — complete the checklist before requesting review.

- Code style and quality
  - This project targets Kotlin on JVM; use the existing project conventions. Run `./gradlew build` to verify compilation.

- Review and merging
  - The default reviewer is the project maintainer (see CODEOWNERS).
  - Maintainters may request changes; please address them and re-request review.

- Changelog and releases
  - Follow semantic versioning (MAJOR.MINOR.PATCH)
  - Update CHANGELOG.md in your PR: add entry under `[Unreleased]` in the appropriate section (Added, Fixed, Changed, etc.)
  - Use present tense ("Add feature", not "Added feature") and reference issues where applicable ("Fixes #123")
  - When releasing, maintainers move `[Unreleased]` entries to a versioned section with the release date
  - See [CHANGELOG.md](../CHANGELOG.md) for detailed guidelines

Thank you for contributing!