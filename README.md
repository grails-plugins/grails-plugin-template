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

## Adapting the Template — What to Change

When creating a new plugin from this template, or migrating an existing plugin onto it, the
following files carry plugin-specific values and must be updated:

| File                                                           | What to change                                                                                                                                                       |
|----------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `settings.gradle`                                              | `rootProject.name` plus the `plugin` and `docs` project names (the plugin project name is the published artifact ID)                                                 |
| `gradle.properties`                                            | `projectVersion`, `grailsVersion`, and `projectsToPublish` (must match the plugin project name from `settings.gradle`)                                               |
| `project.yml`                                                  | All metadata: name, title, description, org, GitHub coordinates, licence, contributors, and versions — this file drives publishing metadata, docs, and release notes |
| `plugin/build.gradle`                                          | `group`, your plugin's dependencies, and which convention plugins to apply                                                                                           |
| `plugin/src/main/groovy/.../PluginTemplateGrailsPlugin.groovy` | Replace with your own `*GrailsPlugin.groovy` descriptor                                                                                                              |
| `examples/app1/`                                               | Rename and adapt into an example app that exercises your plugin (hosts the integration/functional tests)                                                             |
| `docs/src/docs/*.adoc`                                         | Write your plugin's documentation (leave `index.tmpl` untouched — it is filled in by the version-index workflow)                                                     |
| `README.md`                                                    | Rewrite for your plugin: badges, a short description, an installation snippet, and a link to the published documentation                                             |
| `AGENTS.md`                                                    | Update the project overview, artifact names, example-app paths, and version                                                                                          |

Do **not** edit `build-logic/`, `gradle/`, `.github/workflows/`, `.github/scripts/`, `.agents/`,
`CONTRIBUTING.md`, or `LICENSE.txt` in your plugin repository — these are managed by this template
and kept up to date via the automated file sync. Finally, register your plugin in
`.github/projects.yml` **in this repository** so it receives future template updates.

Step-by-step instructions live in [TEMPLATE_README.md](TEMPLATE_README.md). For an LLM-assisted
migration of an existing plugin, point your agent at the
[enhance-plugin-with-template skill](.agents/skills/enhance-plugin-with-template/SKILL.md).

[Documentation]: https://grails-plugins.github.io/grails-plugin-template/
