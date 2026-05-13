package grails.plugins.template

import groovy.util.logging.Commons

import grails.plugins.Plugin

@Commons
class PluginTemplateGrailsPlugin extends Plugin {
    def grailsVersion = "7.0.0 > *"
    def dependsOn = [:]

    def title = "Grails Plugin Template"
    def documentation = "https://grails-plugins.github.io/grails-plugin-templatae/"
    def description = '''\
A Grails plugin Template, that can be used either as a starting point for a new grails project, or as base for existing plugins that needs a uniform release process
'''
    def license = 'APACHE'
    def organization = [name: 'Grails Plugins', url: 'https://github.com/grails-plugins']
    def issueManagement = [system: 'Github', url: 'https://github.com/grails-plugins/grails-plugin-template/issues']
    def scm = [url: 'https://github.com/grails-plugins/grails-plugin-template/']

}
