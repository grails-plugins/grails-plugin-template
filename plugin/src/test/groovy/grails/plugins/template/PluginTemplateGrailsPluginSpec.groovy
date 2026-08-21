package grails.plugins.template

import spock.lang.Specification

class PluginTemplateGrailsPluginSpec extends Specification {

    void "plugin descriptor can be constructed without a Grails application context"() {
        when:
        PluginTemplateGrailsPlugin plugin = new PluginTemplateGrailsPlugin()

        then:
        plugin != null
    }

    void "plugin descriptor exposes the expected metadata"() {
        given:
        PluginTemplateGrailsPlugin plugin = new PluginTemplateGrailsPlugin()

        expect:
        plugin.grailsVersion == '7.0.0 > *'
        plugin.title == 'Grails Plugin Template'
        plugin.documentation == 'https://grails-plugins.github.io/grails-plugin-template/'
        plugin.description.contains('Grails plugin Template')
        plugin.license == 'APACHE'
        plugin.organization == [name: 'Grails Plugins', url: 'https://github.com/grails-plugins']
        plugin.issueManagement == [system: 'GitHub', url: 'https://github.com/grails-plugins/grails-plugin-template/issues']
        plugin.scm == [url: 'https://github.com/grails-plugins/grails-plugin-template/']
    }
}
