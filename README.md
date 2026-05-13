[![Maven Central](https://img.shields.io/maven-central/v/io.github.gpc/grails-plugin-template)](https://central.sonatype.com/artifact/io.github.gpc/grails-plugin-template)
[![License](https://img.shields.io/github/license/grails-plugins/grails-plugin-template)](https://www.apache.org/licenses/LICENSE-2.0)
[![CI](https://github.com/grails-plugins/grails-plugin-template/actions/workflows/ci.yml/badge.svg?event=push)](https://github.com/grails-plugins/grails-plugin-template/actions/workflows/ci.yml)

Grails Plugin Template
======================

A template repository for building Grails plugins with a standardised multi-project structure,
automated CI/CD workflows, and publishing configuration.

Use this repository as the starting point when creating a new Grails plugin under the
`grails-plugins` or `gpc` organisations. It provides:

- Multi-project Gradle build with convention plugins in `build-logic/`
- An example Grails app under `examples/app1/` for integration testing
- Automated dependency updates via Dependabot and Renovate
- GitHub Actions workflows for CI, publishing, release notes, and documentation

The user guide can be found here: 📚 [Documentation]

## Installation

Add the following dependency to the `build.gradle` file:

### Grails 7.x

```groovy
dependencies {
    implementation("io.github.gpc:grails-plugin-template:1.0.0")
}
```

[Documentation]: https://grails-plugins.github.io/grails-plugin-template/
