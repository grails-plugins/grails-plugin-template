package grails.plugins.conventions

import java.nio.file.Path

import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir

/**
 * The conventions only compile against the Grails Gradle plugins; a consuming build puts them on its settings
 * classpath at its own grailsVersion. These specs build a consumer on each supported Grails line to make sure the
 * published conventions do not force a Grails version on it.
 */
class GrailsLineSpec extends Specification {

    @TempDir
    Path projectDir

    // The conventions and their runtime dependencies, as the java-gradle-plugin's pluginUnderTestMetadata lists them
    private static List<File> getConventionsClasspath() {
        def metadata = new Properties()
        GrailsLineSpec.getResourceAsStream('/plugin-under-test-metadata.properties').withCloseable { metadata.load(it) }
        metadata.getProperty('implementation-classpath').split(File.pathSeparator).collect { new File(it) }
    }

    def 'the Grails line specific Gradle plugins are not runtime dependencies of the conventions'() {
        expect: 'they would be in the published POM and force this grailsVersion on every consumer'
        conventionsClasspath*.name.findAll {
            it.startsWith('grails-gradle-plugins') || it.startsWith('asset-pipeline-gradle') ||
                    it.startsWith('grails-publish')
        }.empty
    }

    @Requires({ Runtime.version().feature() >= data.javaVersion })
    def 'config.grails-plugin applies the Grails #grailsVersion Gradle plugin from the build on Gradle #gradleVersion'() {
        given: 'a consumer set up as files-sync.yml leaves it: grailsVersion in gradle.properties, used in settings.gradle'
        projectDir.resolve('gradle.properties').toFile().text = "grailsVersion=${grailsVersion}\n"
        projectDir.resolve('settings.gradle').toFile().text = '''
buildscript {
    repositories {
        maven { url = 'https://repo.grails.org/grails/restricted' }
        maven {
            // Grails snapshots, which repo.grails.org does not serve
            url = 'https://repository.apache.org/content/groups/snapshots'
            content { includeVersionByRegex('org[.]apache[.](grails|groovy).*', '.*', '.*-SNAPSHOT') }
            mavenContent { snapshotsOnly() }
        }
    }
    dependencies {
        classpath platform("org.apache.grails:grails-bom:${grailsVersion}")
        classpath 'org.apache.grails:grails-gradle-plugins'
        classpath 'cloud.wondrify:asset-pipeline-gradle'
        classpath 'org.apache.grails.gradle:grails-publish'
    }
}
rootProject.name = 'sample'
'''
        // A consumer loads the conventions in a project classloader below the settings one (TestKit's
        // withPluginClasspath() would isolate them from the settings classpath), so put them on the buildscript
        projectDir.resolve('build.gradle').toFile().text = """
buildscript {
    dependencies {
        classpath files(${conventionsClasspath.collect { "'${it.absolutePath}'" }.join(', ')})
    }
}
""" + '''
// in the order a plugin project applies them (config.compile first, as the template's plugin/build.gradle does)
apply plugin: 'config.compile'
apply plugin: 'config.grails-plugin'
tasks.register('show') {
    def location = org.grails.gradle.plugin.core.GrailsGradlePlugin.protectionDomain.codeSource.location.path
    def indy = tasks.named('compileGroovy').map { it.groovyOptions.optimizationOptions.indy }
    doLast {
        println "grails-gradle-plugins=${location.tokenize('/').last()}"
        println "indy=${indy.getOrNull()}"
    }
}
'''

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withGradleVersion(gradleVersion)
                .withArguments('show', '-q')
                .forwardOutput()
                .build()

        then:
        result.output.contains("grails-gradle-plugins=grails-gradle-plugins-${grailsVersion}.jar")

        and: 'invokedynamic is the Grails 8 and 9 Gradle plugins\' to decide; on Grails 7 the conventions keep classic call sites'
        result.output.contains("indy=${indy}")

        where:
        gradleVersion                         | grailsVersion                          | javaVersion | indy
        GradleVersion.current().version       | System.getProperty('grailsVersion')    | 17          | false
        System.getProperty('grails8Gradle')   | System.getProperty('grails8Version')   | 21          | false
        System.getProperty('grails9Gradle')   | System.getProperty('grails9Version')   | 21          | null
    }
}
