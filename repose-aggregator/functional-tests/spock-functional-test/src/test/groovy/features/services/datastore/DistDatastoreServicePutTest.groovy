package features.services.datastore

import framework.ReposeValveTest
import org.apache.commons.lang.RandomStringUtils
import org.rackspace.deproxy.Deproxy
import org.rackspace.deproxy.MessageChain

class DistDatastoreServicePutTest extends ReposeValveTest {

    String DD_URI
    def DD_HEADERS = ['X-PP-Host-Key':'temp', 'X-TTL':'10']
    def BODY = "test body"
    static def KEY
    def DD_PATH = "/powerapi/dist-datastore/objects/"
    static def distDatastoreEndpoint = "http://localhost:4999"

    def setupSpec() {
        deproxy = new Deproxy()
        deproxy.addEndpoint(properties.targetPort)
        def params = properties.getDefaultTemplateParams()
        repose.configurationProvider.applyConfigs("common", params)
        repose.configurationProvider.applyConfigs("features/services/datastore/", params)
        repose.start()
        waitUntilReadyToServiceRequests()
    }

    def setup() {
        DD_URI = distDatastoreEndpoint + DD_PATH
        KEY = UUID.randomUUID().toString()
    }

    def cleanupSpec() {
        repose.stop()
        deproxy.shutdown()
    }

    def "PUT a new cache object should return 202 response" () {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '202'
    }


    def "PUT with query parameters should ignore query params and return 202"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY + "?foo=bar", headers:DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '202'

        when:
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then:
        mc.receivedResponse.body == BODY

    }

    def "PUT a cache object to an existing key should overwrite the cached value"() {

        when: "I make 2 PUT calls for 2 different values for the same key"
        String newBody = "MY NEW VALUE"
        deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:DD_HEADERS, requestBody: BODY])
        deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:DD_HEADERS, requestBody: newBody])

        and: "I get the value for the key"
        MessageChain mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then: "The body of the get response should be my second request body"
        mc.receivedResponse.body == newBody
    }

    def "PUT with missing X-TTL is allowed"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:['X-PP-Host-Key':'temp'], requestBody: BODY])

        then:
        mc.receivedResponse.code == '202'

        when: "I get the value for the key"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then:
        mc.receivedResponse.body == BODY
    }

    def "PUT with empty string as body is allowed, but GET will respond with 404"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers:DD_HEADERS, requestBody: ""])

        then:
        mc.receivedResponse.code == '202'

        when: "I get the value from cache with the empty body"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '404'
    }

    def "PUT with no key should return 500 INTERNAL SERVER ERROR"() {
        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI, headers:DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '500'
        mc.receivedResponse.body.toString().contains("Cache key specified is invalid")
    }

    def "PUT with missing X-PP-Host-Key should return a 500 INTERNAL SERVER ERROR and not be stored"() {

        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url:DD_URI + KEY, headers: ['X-TTL':'10'], requestBody: BODY])

        then:
        mc.receivedResponse.code == '500'
        mc.receivedResponse.body.toString().contains("No host key specified in header x-pp-host-key")

        when: "I attempt to get the value from cache"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then: "The key is valid but does not exist, so should return a 404 NOT FOUND"
        mc.receivedResponse.code == '404'
    }

    def "PUT of invalid key should fail with 500 INTERNAL SERVER ERROR"() {

        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url: distDatastoreEndpoint, path: DD_PATH + key,
                headers: DD_HEADERS, requestBody: BODY])

        then:
        mc.receivedResponse.code == '500'

        when: "I attempt to get the value from cache"
        mc = deproxy.makeRequest([method: 'GET', url: distDatastoreEndpoint, path: DD_PATH + key,
                headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '500'
        mc.receivedResponse.body.toString().contains("Cache key specified is invalid")

        where:
        key                                       | scenario
        UUID.randomUUID().toString() +"///"       | "unnecessary slashes on path"
        "foo/bar/?assd=adff"                      | "includes query parameters"
        "foo"                                     | "less than 36 chars"
        UUID.randomUUID().toString() + "a"        | "more than 36 chars"
        "////////" + UUID.randomUUID().toString() | "leading slashes on path"
    }


    def "PUT with really large body within limit (2MEGS 2097152) should return 202"() {
        given:
        def largeBody = RandomStringUtils.random(2097152, ('A'..'Z').join().toCharArray())

        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: largeBody])

        then:
        mc.receivedResponse.code == '202'

        when: "I attempt to get the value from cache"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '200'
        mc.receivedResponse.body == largeBody
        mc.receivedResponse.body.toString().length() == 2097152
    }


    def "PUT with really large body outside limit (2MEGS 2097152) should return 500 INTERNAL SERVER ERROR"() {
        given:
        def largeBody = RandomStringUtils.random(2097153, ('A'..'Z').join().toCharArray())

        when:
        MessageChain mc = deproxy.makeRequest([method: 'PUT', url: DD_URI + KEY, headers: DD_HEADERS, requestBody: largeBody])

        then:
        mc.receivedResponse.code == '500'
        mc.receivedResponse.body.toString().contains("Object is too large to store into the cache")

        when: "I attempt to get the value from cache"
        mc = deproxy.makeRequest([method: 'GET', url:DD_URI + KEY, headers:DD_HEADERS])

        then:
        mc.receivedResponse.code == '404'
    }
}
