# Using the Grails Plugin Template

This document explains how to use `grails-plugin-template` either as a starting point for a **new** Grails plugin or as
a **migration target** for an existing plugin that needs a uniform build and release process.

---

## What the Template Provides

Once a plugin is in the template format it gets:

- A consistent multi-project Gradle build (`plugin/`, `examples/`, `docs/`)
- Convention plugins in `conventions/` that handle compilation, testing, publishing, docs, and asset pipeline
- GitHub Actions for CI, snapshot publishing, multi-stage releases to Maven Central, release notes, contributor
  tracking, and version index updates
- Automated file sync: when a new template release is published, a PR is opened in every registered plugin repo to keep
  all infrastructure files up to date

---

## Starting a New Plugin

1. **Use this repo as a template** — click *Use this template* on GitHub, or clone and remove the `.git` directory.

2. **Rename the plugin subproject** in `settings.gradle`:
   ```groovy
   rootProject.name = 'my-plugin-root'

   include('plugin')
   project(':plugin').name = 'grails-my-plugin'   // artifact ID
   include('docs')
   project(':docs').name = 'grails-my-plugin-docs'
   ```

3. **Update `gradle.properties`** — see the [reference below](#gradleproperties). This includes
   `projectGroup`, your Maven publication group (e.g. `io.github.gpc`) — it applies to the root
   project and every published subproject, so it only needs setting once.

4. **Update `project.yml`** — see the [reference below](#projectyml).

5. **Update `plugin/build.gradle`**:
    - Adjust `dependencies` for your plugin's requirements.

6. **Replace the plugin descriptor** at
   `plugin/src/main/groovy/grails/plugins/template/PluginTemplateGrailsPlugin.groovy` with your own
   `*GrailsPlugin.groovy`.

7. **Register the plugin** in `.github/projects.yml` so it receives future template syncs —
   see [Registering for Sync](#registering-for-sync).

8. **Install SDK versions**: run `sdk env install` (requires [SDKMAN](https://sdkman.io)) to get the correct Java,
   Gradle, and Groovy versions from `.sdkmanrc`.

---

## Migrating an Existing Plugin

The goal is to replace the plugin's ad-hoc build scripts and workflows with the template structure while keeping all
plugin source code in place.

> **Tip:** for an LLM-assisted migration, point your agent (e.g. Claude Code) at
> [.agents/skills/enhance-plugin-with-template/SKILL.md](.agents/skills/enhance-plugin-with-template/SKILL.md) —
> a step-by-step playbook distilled from a real migration, including the pitfalls found along the way.

### Step 1 — Copy infrastructure files

Copy these files and directories wholesale from the template into your plugin repo. Do **not** edit them in your own
repo; they are managed by the template sync:

```
.github/workflows/
.github/scripts/
.github/dependabot.yml
.github/renovate.json
.github/release-drafter.yml
.github/dependency-graph/
.agents/                    # agent skills (symlink .claude -> .agents for Claude Code)
conventions/
gradle/
.editorconfig
.sdkmanrc
CONTRIBUTING.md
gradlew
gradlew.bat
LICENSE.txt
```

### Step 2 — Adopt the multi-project layout

The template expects this structure:

```
my-plugin/
├── plugin/          # Plugin source code and unit tests
│   ├── grails-app/
│   └── src/
├── examples/
│   └── app1/        # Integration / functional tests
├── docs/            # Asciidoctor documentation
├── conventions/     # (copied from template — do not edit)
├── gradle/          # (copied from template — do not edit)
├── build.gradle
├── settings.gradle
├── gradle.properties
└── project.yml
```

Move your plugin source into `plugin/` if it isn't already there.

### Step 3 — Update `settings.gradle`

Replace your existing `settings.gradle` with the template version and change the project names:

```groovy
rootProject.name = 'grails-my-plugin-root'

include('plugin')
project(':plugin').name = 'grails-my-plugin'
include('docs')
project(':docs').name = 'grails-my-plugin-docs'
```

The `examples/` directory is auto-discovered — add a `build.gradle` there for each example app.

### Step 4 — Update `gradle.properties`

See the [reference below](#gradleproperties).

### Step 5 — Create `project.yml`

See the [reference below](#projectyml). Copy the template's `project.yml` and fill in your plugin's details.

### Step 6 — Update `plugin/build.gradle`

Apply the convention plugins and set your group and dependencies:

```groovy
plugins {
    id 'config.code-coverage'
    id 'config.code-style'
    id 'config.compile'
    id 'config.grails-plugin'      // or 'config.grails-web-plugin' if the plugin ships web assets
    id 'config.project-metadata'
    id 'config.publish'
    id 'config.testing'
}

version = projectVersion
group = projectGroup   // from gradle.properties

dependencies {
    profile 'org.apache.grails.profiles:web-plugin'
    compileOnly platform("org.apache.grails:grails-bom:$grailsVersion")
    compileOnly 'org.apache.grails:grails-dependencies-starter-web'
    testImplementation platform("org.apache.grails:grails-bom:$grailsVersion")
    testImplementation 'org.apache.grails:grails-dependencies-starter-web'
    testImplementation 'org.apache.grails:grails-dependencies-test'
}
```

`config.grails-plugin` applies the Grails plugin Gradle plugin only. `config.grails-web-plugin`
composes it with asset-pipeline packaging (`packagePlugin = true`) for plugins that ship
CSS/JS/images under `grails-app/assets/` — this template's own `plugin/build.gradle` uses
`config.grails-web-plugin` for that reason.

Publishing metadata (title, description, licence, developers, GitHub slug) is configured
automatically by `config.publish` from the values in `project.yml` — do not add a manual
`GrailsPublishExtension` block.

### Step 7 — Register for sync

Add the plugin to `.github/projects.yml` in **this template repo** — see [Registering for Sync](#registering-for-sync).

---

## `project.yml`

This file is the single source of metadata for the plugin. It drives documentation generation, Maven publishing
metadata, release notes, and the contributor list.

```yaml
---
project:
    name: "grails-my-plugin"           # artifact ID, matches settings.gradle
    title: "Grails My Plugin"          # human-readable display name
    description: "One-line description of what the plugin does"
    org: "Grails Plugins"              # organisation display name (for Maven POM)

github:
    org: "gpc"                         # GitHub org slug (gpc or grails-plugins)
    project: "grails-my-plugin"        # GitHub repo name

license:
    name: "Apache-2.0"                 # SPDX licence identifier

contributors: # updated automatically by update-contributors workflow
    githubLogin: "Display Name"

versions:
    current: "1.0.0"                   # updated automatically on each release
    previous:
        - "1.0.0"
    ignore: # version strings to exclude from the docs index
        - "1.0.x"
        - "snapshot"
```

The `contributors` and `versions` blocks are maintained automatically by GitHub Actions — you do not need to edit them
by hand after the initial setup.

### Protected branches

The `update-contributors` and `update-versions` workflows write these blocks back into `project.yml` on the default
branch. Both go through `.github/scripts/commit-or-pr.sh`, which tries a direct push first and only falls back to a
pull request if that push is rejected. So:

- **Unprotected default branch** — the commit lands directly, exactly as before. No PR is created.
- **Protected default branch** — the commit is parked on a `chore/update-contributors` or `chore/update-versions`
  branch and a PR is opened against the default branch, then put into auto-merge (squash).

No repository configuration is required for this — both workflows handle the two things that would otherwise stall
such a PR:

- **Required status checks.** A PR opened with `GITHUB_TOKEN` never triggers workflows, so required checks would
  never report and the PR could never merge. Both workflows therefore mint a token from the org's GitHub App
  (`APP_ID` / `APP_PRIVATE_KEY`, the same credentials `files-sync.yml` uses) when those secrets are present, and only
  fall back to `GITHUB_TOKEN` where they are not. With the App token, CI runs on the chore PR normally.
- **"Allow auto-merge" switched off.** `gh pr merge --auto` fails on such a repo, so the script falls back to watching
  the checks itself and then merging directly. Set `MERGE_WAIT_SECONDS` (default `900`) to change how long it waits.

If the PR still cannot be merged — required reviews, a failing check, a conflict — the script logs a `::warning::`
naming the PR and **exits 0**, so a chore commit never fails the release pipeline. The PR stays open for a human.

The version bump in `gradle.properties` after a release is separate: it is handled by
`apache/grails-github-actions/post-release@asf` in the `close` job of `release.yml`, which already opens a PR.

---

## `gradle.properties`

| Property                     | Description                                                                                                     |
|------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `projectVersion`             | Current version, e.g. `1.0.0-SNAPSHOT`. Use `-SNAPSHOT` suffix on development branches.                         |
| `projectGroup`               | Maven group for the root project and all published subprojects, e.g. `io.github.gpc`.                          |
| `grailsVersion`              | Grails BOM version to compile and test against, e.g. `7.0.11`.                                                 |
| `projectsToPublish`          | Comma-separated list of subproject names to include in Maven publishing. Usually just the plugin artifact name. |
| `checkstyleVersion`          | Optional override of the Checkstyle version used by `config.code-style` (default lives in `conventions/`).      |
| `codenarcVersion`            | Optional override of the CodeNarc version used by `config.code-style`.                                          |
| `jacocoVersion`              | Optional override of the JaCoCo version used by `config.code-coverage*`.                                        |
| `ciBuildScanPublish`         | Set to `true` to publish Gradle build scans from CI.                                                            |
| `ciBuildScanTermsOfUseUrl`   | Build scan terms URL — leave as-is.                                                                             |
| `ciBuildScanTermsOfUseAgree` | Set to `yes` to agree to build scan terms.                                                                      |

The `org.gradle.*` properties at the bottom control daemon, caching, and JVM settings — leave them unchanged unless you
have a specific reason. The Java version comes from `.sdkmanrc`, not from `gradle.properties`.

**Minimal example for a new plugin:**

```properties
projectVersion=1.0.0-SNAPSHOT
projectGroup=io.github.gpc
grailsVersion=7.0.11
projectsToPublish=grails-my-plugin
checkstyleVersion=10.21.4
codenarcVersion=3.6.0
jacocoVersion=0.8.12
ciBuildScanPublish=true
ciBuildScanTermsOfUseUrl=https://gradle.com/terms-of-service
ciBuildScanTermsOfUseAgree=yes
org.gradle.caching=true
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.jvmargs=-Dfile.encoding=UTF-8 -Xmx1024M
```

---

## Registering for Sync

To receive automated infrastructure updates, add your plugin to `.github/projects.yml` in **this template repo** (
`grails-plugin-template`):

```yaml
projects:
    gpc:
        - my-plugin          # → github.com/gpc/grails-my-plugin
    grails-plugins:
        - another-plugin     # → github.com/grails-plugins/grails-another-plugin
```

Once registered, every new release of `grails-plugin-template` will open a PR in your plugin repo updating the
infrastructure files. The PR is opened on a branch named `sync-files-from-template` and will auto-merge if CI passes.

### Locking a file

If you need to diverge from the template for a specific file, create a `.lock` file next to it:

```
touch .github/workflows/ci.yml.lock
```

The sync will skip any file that has a corresponding `.lock` file alongside it. Remove the lock file when you want to
accept template updates again.

---

## SDK Versions

Tool versions are pinned in `.sdkmanrc`. Install [SDKMAN](https://sdkman.io) and run:

```bash
sdk env install
```

| Tool   | Version                         |
|--------|---------------------------------|
| Java   | `17.0.18-librca` (Liberica JDK) |
| Gradle | `8.14.4`                        |
| Groovy | `4.0.30`                        |
