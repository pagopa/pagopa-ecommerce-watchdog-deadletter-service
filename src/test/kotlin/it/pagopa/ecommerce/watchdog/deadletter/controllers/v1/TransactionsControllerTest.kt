package it.pagopa.ecommerce.watchdog.deadletter.controllers.v1

import it.pagopa.ecommerce.watchdog.deadletter.config.TestSecurityConfig
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidOtpException
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidTransactionId
import it.pagopa.ecommerce.watchdog.deadletter.exception.RefundGatewayException
import it.pagopa.ecommerce.watchdog.deadletter.exception.TransactionNotRefundableException
import it.pagopa.ecommerce.watchdog.deadletter.services.AuthService
import it.pagopa.ecommerce.watchdog.deadletter.services.RefundOutcome
import it.pagopa.ecommerce.watchdog.deadletter.services.TransactionCheckService
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionCheckDto
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionRefundRequestDto
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionStatusResponseDto
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(TransactionsController::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
@Import(TestSecurityConfig::class)
class TransactionsControllerTest {
    @Autowired private lateinit var webClient: WebTestClient

    @MockitoBean lateinit var authService: AuthService

    @MockitoBean lateinit var transactionCheckService: TransactionCheckService

    private val transactionId = "6460dc3e61ba4ed6bfea2c4468b2862c"

    @Test
    fun `get transaction should return 200 with the transaction`() {
        given(transactionCheckService.getTransaction(transactionId))
            .willReturn(
                Mono.just(
                    TransactionCheckDto()
                        .transactionId(transactionId)
                        .eCommerceStatus("NOTIFIED_OK")
                        .events(listOf())
                )
            )

        webClient
            .get()
            .uri("/v1/transactions/{id}", transactionId)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.transactionId")
            .isEqualTo(transactionId)
            .jsonPath("$.eCommerceStatus")
            .isEqualTo("NOTIFIED_OK")
    }

    @Test
    fun `get transaction should return 404 when not found`() {
        given(transactionCheckService.getTransaction(transactionId))
            .willReturn(Mono.error(InvalidTransactionId()))

        webClient
            .get()
            .uri("/v1/transactions/{id}", transactionId)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `get transaction should return 400 for a malformed id`() {
        webClient
            .get()
            .uri("/v1/transactions/{id}", "not-a-valid-id")
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `get transaction status should return 200 with the eCommerce status`() {
        given(transactionCheckService.getTransactionStatus(transactionId))
            .willReturn(Mono.just(TransactionStatusResponseDto("REFUND_REQUESTED")))

        webClient
            .get()
            .uri("/v1/transactions/{id}/status", transactionId)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.eCommerceStatus")
            .isEqualTo("REFUND_REQUESTED")
    }

    private fun postRefund(body: String) =
        webClient
            .post()
            .uri("/v1/transactions/{id}/refund", transactionId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .exchange()

    @Test
    fun `refund should return 201 when the refund is queued`() {
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("operator"))
        given(transactionCheckService.refundTransaction(any(), any(), any()))
            .willReturn(Mono.just(RefundOutcome.ACCEPTED))

        postRefund("""{"otpCode":"123456","force":false}""").expectStatus().isCreated

        verify(transactionCheckService)
            .refundTransaction(
                eq(transactionId),
                eq("operator"),
                eq(TransactionRefundRequestDto("123456").force(false)),
            )
    }

    @Test
    fun `refund should return 200 when the forced refund is completed`() {
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("operator"))
        given(transactionCheckService.refundTransaction(any(), any(), any()))
            .willReturn(Mono.just(RefundOutcome.COMPLETED))

        postRefund("""{"otpCode":"123456","force":true}""").expectStatus().isOk
    }

    @Test
    fun `refund should return 422 for an invalid otp`() {
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("operator"))
        given(transactionCheckService.refundTransaction(any(), any(), any()))
            .willReturn(Mono.error(InvalidOtpException()))

        postRefund("""{"otpCode":"123456"}""").expectStatus().isEqualTo(422)
    }

    @Test
    fun `refund should return 409 when the transaction is not refundable`() {
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("operator"))
        given(transactionCheckService.refundTransaction(any(), any(), any()))
            .willReturn(Mono.error(TransactionNotRefundableException("not refundable")))

        postRefund("""{"otpCode":"123456"}""")
            .expectStatus()
            .isEqualTo(409)
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("not refundable")
    }

    @Test
    fun `refund should return 502 with the gateway message`() {
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("operator"))
        given(transactionCheckService.refundTransaction(any(), any(), any()))
            .willReturn(Mono.error(RefundGatewayException("PSP refused")))

        postRefund("""{"otpCode":"123456","force":true}""")
            .expectStatus()
            .isEqualTo(502)
            .expectBody()
            .jsonPath("$.detail")
            .isEqualTo("PSP refused")
    }

    @Test
    fun `refund should return 400 for a malformed otp`() {
        given(authService.getAuthenticatedUserId()).willReturn(Mono.just("operator"))

        postRefund("""{"otpCode":"12ab"}""").expectStatus().isBadRequest
    }
}
