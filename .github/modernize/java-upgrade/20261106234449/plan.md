# Upgrade Plan: autotech-manager (20261106234449)

- **Generated**: 2026-11-06
- **HEAD Branch**: N/A (not a Git repository)
- **HEAD Commit ID**: N/A (not a Git repository)

## Available Tools

**JDKs**
- JDK 17: not available (baseline will be skipped)
- JDK 25.0.2: /home/amolina/.jdk/jdk-25.0.2 (available; target runtime)

**Build Tools**
- Maven Wrapper: 3.10.0 (available; will be used)
- Maven 3.9.15: /home/amolina/.maven/maven-3.9.15/bin (available)

## Guidelines

> Note: You can add any specific guidelines or constraints for the upgrade process here if needed, bullet points are preferred.

- Scope is limited to the Java runtime/compiler target; preserve existing framework and dependency versions unless compatibility or CVE validation requires a change.
- Changes are not version-controlled because `/home/amolina/autotech-manager` is not a Git repository.

## Options

- Working branch: appmod/java-upgrade-20261106234449 (not applicable; Git unavailable)
- Run tests before and after the upgrade: true

## Upgrade Goals

- Upgrade the Java runtime/compiler target from 17 to 25 (latest LTS).

## Technology Stack

| Technology/Dependency | Current | Min Compatible Version | Why Incompatible |
| --------------------- | ------- | ---------------------- | ---------------- |
| Java | 17 | 25 | User requested Java 25 LTS |
| Spring Boot parent | 4.1.2 | 4.1.2 | No framework change requested; parent supports modern Java targets |
| Maven Wrapper | 3.10.0 | 3.9+ recommended | Current wrapper exceeds recommended Maven baseline and is retained |
| maven-compiler-plugin | Managed by Spring Boot 4.1.2 | 3.11+ recommended | Version is parent-managed; no explicit incompatible override found |
| maven-surefire-plugin | Managed by Spring Boot 4.1.2 | 3.0+ recommended | Version is parent-managed; no explicit incompatible override found |
| Microsoft SQL Server JDBC | 13.4.0.jre11 | 13.4.0.jre11 | Driver artifact targets Java 11+ and is retained |
| OpenPDF | 1.3.30 | To be validated | No Java 25-specific incompatibility found in project metadata; exercise through compilation and tests |

## Derived Upgrades

- No framework, build-tool, or dependency upgrades are inferred. Java 25.0.2 and Maven Wrapper 3.10.0 are already available.

## Impact Analysis

### Dependency Changes

| File | Dependency | Current | Action | Target | Reason |
|------|-----------|---------|--------|--------|--------|
| pom.xml | java.version | 17 | upgrade | 25 | Set the Java compilation/runtime target requested by the user |

### Source Code Changes

No source changes identified. The compatibility scan found no use of JDK-internal packages, reflective access to JDK internals, or removed Java APIs in the project source.

### Configuration Changes

No separate runtime configuration changes identified. The only Java target is the Maven `java.version` property in `pom.xml`.

### CI/CD Changes

No CI/CD files or separate hardcoded Java runtime versions were found in this project.

### Risks & Warnings

- **Baseline unavailable**: JDK 17 is not installed, so pre-upgrade compilation and test pass rate cannot be recorded. **Mitigation**: Run compilation and the full test suite with the installed target JDK 25 after the change.
- **Third-party runtime compatibility**: OpenPDF 1.3.30 and other existing dependencies may expose Java 25 runtime issues not detectable by static scanning. **Mitigation**: Run the complete test suite and address any reproducible failures before completion.

## Upgrade Steps

- Step 1: Setup Environment
  - **Rationale**: Confirm the required target JDK and Maven wrapper are usable before making changes.
  - **Changes to Make**: No installation is required; Java 25.0.2 and Maven Wrapper 3.10.0 are already available.
  - **Verification**: `JAVA_HOME=/home/amolina/.jdk/jdk-25.0.2 /home/amolina/.jdk/jdk-25.0.2/bin/java -version`; verify Maven wrapper version. Expected: Java 25 and Maven 3.10.0.
- Step 2: Setup Baseline
  - **Rationale**: Record pre-upgrade compilation and test status where the current JDK is available.
  - **Changes to Make**: None.
  - **Verification**: Skipped because JDK 17 is not installed; baseline cannot run under the configured current target without that JDK.
- Step 3: Set Java Target to 25
  - **Rationale**: Update the Maven compilation/runtime target to the requested latest LTS.
  - **Changes to Make**: Apply the Dependency Changes item in Impact Analysis.
  - **Verification**: With JDK 25, run `./mvnw clean test-compile -q`. Expected: production and test sources compile successfully.
- Step 4: Validate and Remediate CVEs
  - **Rationale**: Check resolved direct dependencies after the Java target change and remediate any reported vulnerabilities.
  - **Changes to Make**: No dependency changes unless the CVE scan reports an available fix; then make the minimal compatible upgrade and update this plan/progress record.
  - **Verification**: Extract resolved direct dependencies, run the Java dependency CVE validation, compile after any fixes, and re-scan to confirm remediation.
- Step 5: Final Validation
  - **Rationale**: Confirm Java 25 is the configured target and that the full build and tests pass.
  - **Changes to Make**: Resolve any Java 25 compilation or test failures and ensure no temporary workarounds remain.
  - **Verification**: With JDK 25, run `./mvnw clean test-compile -q` and `./mvnw clean test -q`; expected: compilation succeeds and all tests pass. Collect coverage using `./mvnw clean verify -Djacoco.skip=false` if supported by the project.
