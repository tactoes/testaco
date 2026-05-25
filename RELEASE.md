# Release process

This project uses semantic versioning. Releases are created as GitHub Releases.

Suggested process:
1. Ensure all CI checks pass on `main`.
2. Update CHANGELOG.md (or use GitHub release notes) with notable changes.
3. Tag the commit: `git tag -a vX.Y.Z -m "Release vX.Y.Z"` and push tag.
4. Draft a release on GitHub and publish.

Optionally automate with GitHub Actions for release notes and changelog generation.
