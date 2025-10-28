package it.pagopa.ecommerce.watchdog.deadletter

import it.pagopa.ecommerce.watchdog.deadletter.config.jwt.JwtAuthenticationConverter
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.test.properties"])
class PagopaEcommerceWatchdogDeadletterApplicationTests {
    @MockitoBean lateinit var jwtDecoder: ReactiveJwtDecoder
    @MockitoBean lateinit var jwtAuthenticationConverter: JwtAuthenticationConverter

    @Test
    fun contextLoads() {
        assertTrue { true }
    }
}
