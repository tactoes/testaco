# Releasing Testaco

This guide covers Sonatype OSSRH setup and the release process for publishing to Maven Central.

## Prerequisites: Sonatype OSSRH Account Setup (One-Time)

### 1. Create Sonatype JIRA Account

1. Go to https://issues.sonatype.org/
2. Create an account (or log in if you have one)
3. Create a ticket to claim the `org.testaco` namespace:
   - **Project:** Community Licenses (OSSRH)
   - **Issue Type:** New Project
   - **Summary:** "Testaco - Kotlin database testing library"
   - **Group Id:** `org.testaco`
   - **Project URL:** `https://github.com/tactoes/testaco`
   - **SCM URL:** `https://github.com/tactoes/testaco.git`
4. Wait for approval (usually 1-2 business days; responds to your ticket)

### 2. Generate GPG Key

If you don't have a GPG key:

```bash
gpg --full-generate-key
# Select: RSA and RSA (default)
# Key size: 4096
# Expiration: 0 (no expiration) or 1 year
# Name: Your Name
# Email: your.email@example.com
# Passphrase: Strong passphrase (you'll need this)
```

List your key:
```bash
gpg --list-secret-keys --keyid-format SHORT
# Output example:
# sec   rsa4096/ABC12345 2026-07-18 [SC] [expires: 2027-07-18]
#       FINGERPRINT123456789
# uid         [ultimate] Your Name <your.email@example.com>
```

Export public key to Maven Central:
```bash
gpg --keyserver keyserver.ubuntu.com --send-keys ABC12345
# (Replace ABC12345 with your key ID from above)
```

### 3. Configure Maven Credentials

Create or edit `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>YOUR_SONATYPE_JIRA_USERNAME</username>
      <password>YOUR_SONATYPE_JIRA_PASSWORD</password>
    </server>
  </servers>
</settings>
```

Restrict permissions:
```bash
chmod 600 ~/.m2/settings.xml
```

## Release Process

### 1. Prepare Local Environment

Ensure you have:
- GPG configured and public key on keyserver
- `~/.m2/settings.xml` with Sonatype credentials
- All tests passing: `./gradlew clean test`
- Latest code pulled from `main`

### 2. Update Version and CHANGELOG

1. Update `build.gradle.kts`:
   ```kotlin
   version = "0.1.0"  // Change from 0.1.0-SNAPSHOT to 0.1.0
   ```

2. Update `CHANGELOG.md`:
   - Move all entries from `[Unreleased]` to new section `## [0.1.0] - 2026-07-18`
   - Keep `[Unreleased]` section with empty subsections
   - At bottom, add:
     ```markdown
     [Unreleased]: https://github.com/tactoes/testaco/compare/v0.1.0...HEAD
     [0.1.0]: https://github.com/tactoes/testaco/releases/tag/v0.1.0
     ```

3. Commit:
   ```bash
   git add build.gradle.kts CHANGELOG.md
   git commit -m "chore: prepare release v0.1.0"
   ```

### 3. Create and Push Git Tag

```bash
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin main
git push origin v0.1.0
```

### 4. GitHub Actions Will:
- Build the project
- Import GPG key (using secrets)
- Sign and publish to Sonatype OSSRH
- Create GitHub Release with artifacts

**Note:** GitHub Actions workflow (`release.yml`) requires these secrets configured in repository settings:
- `OSSRH_USERNAME` - Sonatype JIRA username
- `OSSRH_PASSWORD` - Sonatype JIRA password
- `GPG_PRIVATE_KEY` - Your GPG private key (export: `gpg --export-secret-keys --armor ABC12345`)
- `GPG_KEY_ID` - Your GPG key ID (e.g., ABC12345)
- `GPG_PASSPHRASE` - Your GPG passphrase

### 5. Verify Published Artifact

1. Check Sonatype OSSRH: https://oss.sonatype.org/#nexus-search;quick~testaco
2. Search Maven Central (takes 10-30 min to sync): https://search.maven.org/search?q=org.testaco
3. Verify in your project:
   ```kotlin
   dependencies {
       implementation("org.testaco:testaco-core:0.1.0")
   }
   ```

### 6. After Release: Bump to Next SNAPSHOT Version

1. Update `build.gradle.kts`:
   ```kotlin
   version = "0.2.0-SNAPSHOT"
   ```

2. Reset `CHANGELOG.md` `[Unreleased]` section:
   ```markdown
   ## [Unreleased]

   ### Added
   - N/A

   ### Fixed
   - N/A
   ```

3. Commit:
   ```bash
   git add build.gradle.kts CHANGELOG.md
   git commit -m "chore: bump version to 0.2.0-SNAPSHOT"
   git push origin main
   ```

## Troubleshooting

**Error: "Could not find artifact"**
- Verify `OSSRH_USERNAME` and `OSSRH_PASSWORD` secrets in GitHub Actions settings
- Check Sonatype JIRA ticket status

**Error: "GPG verification failed"**
- Verify `GPG_PRIVATE_KEY` secret is correctly formatted (export with `--armor`)
- Verify `GPG_KEY_ID` and `GPG_PASSPHRASE` match your key

**Error: "Group ID org.testaco not yet approved"**
- Wait for Sonatype to respond to your JIRA ticket
- Once approved, the group ID is locked to your account

## References

- [Sonatype OSSRH Guide](https://central.sonatype.org/publishing/publish-maven/)
- [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)
- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
