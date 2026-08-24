#!/usr/bin/env groovy
@Grab('org.apache.groovy:groovy-yaml')
import groovy.json.JsonOutput
import groovy.yaml.YamlSlurper

def projectsYml = new YamlSlurper().parse(new File('.github/projects.yml')) as Map
def filterProject = System.getenv('FILTER_PROJECT') ?: ''

def repos = []
(projectsYml.projects as Map<String, List>).each { org, repoList ->
    repoList.each { entry ->
        // entry is either a plain repo name, or a map with repo:/branch: to
        // pin the sync PR to a specific branch instead of the GitHub-reported
        // default branch.
        def repo = entry instanceof Map ? entry.repo : entry
        def branch = entry instanceof Map ? (entry.branch ?: '') : ''
        if (!filterProject || filterProject == "${org}/${repo}") {
            repos << [org: org, repo: repo, branch: branch]
        }
    }
}

def line = "repos=${JsonOutput.toJson(repos)}"
def outputFile = System.getenv('GITHUB_OUTPUT')
if (outputFile) {
    new File(outputFile) << "${line}\n"
} else {
    System.err.println "GITHUB_OUTPUT not set — would output: ${line}"
}
println "Matrix: ${repos.size()} repo(s)"
