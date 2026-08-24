package grails.plugins.template

import grails.testing.mixin.integration.Integration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import spock.lang.Shared
import spock.lang.Specification

@Integration
class TestControllerIntegrationSpec extends Specification {

    @Shared
    RestTemplate restTemplate = new RestTemplate()

    private String getBaseUrl() {
        "http://localhost:${serverPort}"
    }

    private ResponseEntity<String> doGet(String path) {
        restTemplate.exchange("${baseUrl}${path}", HttpMethod.GET, null, String)
    }

    void "root path redirects to the /test endpoint"() {
        when:
        ResponseEntity<String> response = doGet('/')

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('Welcome to Grails Plugin Template')
    }

    void "test controller index action renders the plugin welcome page"() {
        when:
        ResponseEntity<String> response = doGet('/test')

        then:
        response.statusCode == HttpStatus.OK
        response.body.contains('Test Controller')
        response.body.contains('Welcome to Grails Plugin Template')
    }
}
