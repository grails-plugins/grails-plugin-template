#!/usr/bin/env groovy
// Points a synced repository's settings.gradle at the published grails-plugin-conventions plugin.
//
// Template-only script, run by files-sync.yml against the checked-out target repository (it is never synced).
//
//   groovy update-conventions-reference.groovy <settings.gradle> <conventions-version>
//
// It is idempotent. It:
//   * removes `includeBuild('./build-logic')` / `includeBuild('./conventions')` from pluginManagement,
//   * ensures pluginManagement.repositories contains mavenCentral() (and, for a -SNAPSHOT version, the Central
//     snapshots repository),
//   * maps every `config.*` plugin id to org.grails.plugins.gradle:grails-plugin-conventions:<version>,
//   * puts the Grails, asset-pipeline and grails-publish Gradle plugins on the settings classpath at the repository's
//     own grailsVersion (the conventions only compile against them, so they work on any Grails line).
// Everything it adds sits between marker comments, so a later run replaces it instead of duplicating it.

import java.util.regex.Pattern

def startMarker = '// >>> grails-plugin-conventions (managed by grails-plugin-template files-sync, do not edit)'
def endMarker = '// <<< grails-plugin-conventions'
def snapshotsUrl = 'https://central.sonatype.com/repository/maven-snapshots/'

def fail = { String message ->
    System.err.println(message)
    System.exit(1)
}

if (args.length != 2) {
    fail('Usage: groovy update-conventions-reference.groovy <settings.gradle> <conventions-version>')
}

def settingsFile = new File(args[0])
def version = args[1].trim()
if (!settingsFile.exists()) {
    fail("${settingsFile} does not exist")
}

// Returns [openBraceIndex, closeBraceIndex] of the first `<header> {` block at or after `from`, or null
def findBlock = { String text, String header, int from ->
    def matcher = Pattern.compile(header + /\s*\{/).matcher(text)
    if (!matcher.find(from)) {
        return null
    }
    def openIndex = matcher.end() - 1
    def depth = 0
    for (int index = openIndex; index < text.length(); index++) {
        def character = text.charAt(index)
        if (character == ('{' as char)) {
            depth++
        } else if (character == ('}' as char)) {
            depth--
            if (depth == 0) {
                return [openIndex, index]
            }
        }
    }
    fail('settings.gradle has unbalanced braces')
}

def text = settingsFile.getText('UTF-8')
def mavenLocal = text.contains('INCLUDE_MAVEN_LOCAL')

// Drop what an earlier run added, and the included build the sources used to come from
text = text.replaceAll(
        '(?s)[ \\t]*' + Pattern.quote(startMarker) + '.*?' + Pattern.quote(endMarker) + '[ \\t]*\\n?', '')
text = text.replaceAll(
        /[ \t]*includeBuild\(\s*['"]\.\/(?:build-logic|conventions)['"]\s*\)\s*(?:\{[^{}]*\})?[ \t]*\n/, '')

def pluginManagement = findBlock(text, 'pluginManagement', 0)
if (!pluginManagement) {
    fail('settings.gradle has no pluginManagement { } block')
}
def repositories = findBlock(text, 'repositories', pluginManagement[0])
if (!repositories || repositories[0] > pluginManagement[1]) {
    fail('settings.gradle has no repositories { } block inside pluginManagement')
}

// resolutionStrategy goes in first (at the end of pluginManagement), so the repositories indexes stay valid
def mapping = [
        "    ${startMarker}",
        '    resolutionStrategy {',
        '        eachPlugin {',
        "            if (requested.id.id.startsWith('config.')) {",
        "                useModule('org.grails.plugins.gradle:grails-plugin-conventions:${version}')",
        '            }',
        '        }',
        '    }',
        "    ${endMarker}",
        '',
].join('\n')
def closeLineStart = text.lastIndexOf('\n', pluginManagement[1]) + 1
text = text.substring(0, closeLineStart) + mapping + text.substring(closeLineStart)

def repositoryLines = [startMarker]
if (!text.substring(repositories[0], repositories[1]).contains('mavenCentral()')) {
    repositoryLines << 'mavenCentral()'
}
if (version.endsWith('-SNAPSHOT')) {
    repositoryLines << "maven { url = '${snapshotsUrl}'; mavenContent { snapshotsOnly() } }".toString()
}
repositoryLines << endMarker
if (repositoryLines.size() > 2) {
    def insertAt = text.indexOf('\n', repositories[0]) + 1
    text = text.substring(0, insertAt) + repositoryLines.collect { "        ${it}\n" }.join('') +
            text.substring(insertAt)
}

// The config.* plugins apply the Grails Gradle plugins without bringing them, so the build has to provide them. They go
// on the settings classpath, which every project inherits, at the grailsVersion from the repository's gradle.properties.
def classpathRepositories = [
        "maven { url = 'https://repo.grails.org/grails/restricted' }",
]
if (mavenLocal) {
    classpathRepositories.addAll(0, [
            "if (System.getenv('INCLUDE_MAVEN_LOCAL')) {",
            '    mavenLocal()',
            '}',
    ])
}
def classpathLines = [
        'repositories {',
        *classpathRepositories.collect { "    ${it}" },
        '}',
        'dependencies {',
        '    classpath platform("org.apache.grails:grails-bom:${grailsVersion}")',
        "    classpath 'org.apache.grails:grails-gradle-plugins'",
        "    classpath 'cloud.wondrify:asset-pipeline-gradle'",
        "    classpath 'org.apache.grails.gradle:grails-publish'",
        '}',
]
def buildscript = findBlock(text, /(?m)^buildscript/, 0)
if (buildscript) {
    // A settings script can have only one buildscript block, so add to the one that is there
    def insertAt = text.indexOf('\n', buildscript[0]) + 1
    text = text.substring(0, insertAt) +
            ([startMarker, *classpathLines, endMarker].collect { "    ${it}\n" }.join('')) +
            text.substring(insertAt)
} else {
    pluginManagement = findBlock(text, 'pluginManagement', 0)
    def insertAt = text.indexOf('\n', pluginManagement[1]) + 1
    def block = [
            '',
            startMarker,
            'buildscript {',
            *classpathLines.collect { "    ${it}" },
            '}',
            endMarker,
    ].join('\n') + '\n'
    text = text.substring(0, insertAt) + block + text.substring(insertAt)
}

text = text.replaceAll(/\n{3,}/, '\n\n')
settingsFile.setText(text, 'UTF-8')
