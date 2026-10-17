package grails.plugins.conventions

import java.nio.file.Path

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir

/**
 * config.docs runs Groovydoc in the root project over the sources of the plugin project. Gradle 9 does not let a task
 * resolve another project's configurations, so the aggregation must only resolve configurations of its own project.
 */
class PluginDocsSpec extends Specification {

    @TempDir
    Path projectDir

    private void write(String path, String text) {
        def file = projectDir.resolve(path).toFile()
        file.parentFile.mkdirs()
        file.text = text
    }

    @Requires({ Runtime.version().feature() >= data.javaVersion })
    def 'config.docs aggregates the Groovydoc of a Grails #grailsVersion plugin project on Gradle #gradleVersion'() {
        given: 'a root project with config.docs and a plugin project laid out like the template'
        write('settings.gradle', '''
dependencyResolutionManagement {
    repositories { mavenCentral() }
}
rootProject.name = 'sample-root'
include 'sample'
''')
        write('gradle.properties', "grailsVersion=${grailsVersion}\n")
        write('build.gradle', "plugins { id 'config.docs' }\n")
        write('sample/build.gradle', """
plugins { id 'groovy' }
dependencies {
    implementation platform('org.apache.grails:grails-bom:${grailsVersion}')
    implementation 'org.apache.groovy:groovy'
}
""")
        write('sample/src/main/groovy/sample/Greeter.groovy', '''
package sample

/** Greets people. */
class Greeter {
    String greet(String name) { "Hello ${name}" }
}
''')

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withGradleVersion(gradleVersion)
                // consumers build in parallel (org.gradle.parallel=true), where a task only holds the lock of its own project
                .withArguments('aggregateGroovyApiDoc', '--parallel', '--stacktrace')
                .forwardOutput()
                .build()

        then:
        result.task(':aggregateGroovyApiDoc').outcome == TaskOutcome.SUCCESS
        projectDir.resolve('build/docs/gapi/sample/Greeter.html').toFile().file

        and: 'only the sources are taken from the plugin project, its own Groovydoc is not generated'
        result.task(':sample:groovydoc') == null

        where:
        gradleVersion                         | grailsVersion                          | javaVersion
        GradleVersion.current().version       | System.getProperty('grailsVersion')    | 17
        System.getProperty('grails8Gradle')   | System.getProperty('grails8Version')   | 21
    }
}
