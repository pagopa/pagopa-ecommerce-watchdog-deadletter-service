package it.pagopa.ecommerce.watchdog.deadletter.config

import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.BadCredentialsException
import reactor.test.StepVerifier

class ApiKeyAuthenticationTest {

    private val manager = ApiKeyAuthenticationManager("valid-key")

    @Test
    fun `converter should be empty when api key header is missing`() {
        val exchange =
            MockServerWebExchange.from(MockServerHttpRequest.get("/v2/deadletter-transactions"))
        StepVerifier.create(ApiKeyAuthenticationConverter().convert(exchange)).verifyComplete()
    }

    @Test
    fun `converter should read api key header`() {
        val exchange =
            MockServerWebExchange.from(
                MockServerHttpRequest.get("/v2/deadletter-transactions")
                    .header(API_KEY_HEADER, "valid-key")
            )
        StepVerifier.create(ApiKeyAuthenticationConverter().convert(exchange))
            .expectNextMatches { (it as ApiKeyAuthenticationToken).apiKey == "valid-key" }
            .verifyComplete()
    }

    @Test
    fun `manager should authenticate valid api key`() {
        StepVerifier.create(manager.authenticate(ApiKeyAuthenticationToken("valid-key")))
            .expectNextMatches { it.isAuthenticated }
            .verifyComplete()
    }

    @Test
    fun `manager should reject invalid api key`() {
        StepVerifier.create(manager.authenticate(ApiKeyAuthenticationToken("wrong")))
            .expectError(BadCredentialsException::class.java)
            .verify()
    }

    @Test
    fun `manager should reject any key when configured key is blank`() {
        StepVerifier.create(
                ApiKeyAuthenticationManager("").authenticate(ApiKeyAuthenticationToken(""))
            )
            .expectError(BadCredentialsException::class.java)
            .verify()
    }
}
