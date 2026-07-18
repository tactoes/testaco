# Changelog


All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial public release preparation
- Kotlin re-implementation of DbUnit for PostgreSQL test data management
- Support for timestamps with tolerance for being "almost now" added.
- Support for loading multiple, stacked files added
- Gradle test file manipulation actions added.
- First, automatically generated version; has still not been manually code-reviewed fully. The goal of the
  project is partly to see if LLMs can rewrite existing code into other languages more or less without
  manual intervention. The library needs manual code review in order to be ready for production.

### Fixed
- N/A (pre-release)

### Changed
- N/A (pre-release)

### Deprecated
- N/A (pre-release)

### Removed
- N/A (pre-release)

### Security
- N/A (pre-release)

## Guidelines for Contributors

When adding changes, update this file in the pull request:
1. Add entry under `[Unreleased]` in appropriate section (Added, Fixed, Changed, etc.)
2. Use present tense: "Add feature" not "Added feature"
3. Reference issues where applicable: "Fixes #123"
4. When releasing, move `[Unreleased]` entries to versioned section with release date
5. Keep changelog human-readable; technical details belong in commit messages

See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.
