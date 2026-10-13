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
//   * maps every `config.*` plugin id to org.grails.plugins.gradle:grails-plugin-conventions:<version>.
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

text = text.replaceAll(/\n{3,}/, '\n\n')
settingsFile.setText(text, 'UTF-8')
