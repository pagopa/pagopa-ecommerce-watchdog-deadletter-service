package it.pagopa.ecommerce.watchdog.deadletter.services

import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskCommandsClient
import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskServiceClient
import it.pagopa.ecommerce.watchdog.deadletter.clients.NodoTechnicalSupportClient
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidOtpException
import it.pagopa.ecommerce.watchdog.deadletter.exception.InvalidTransactionId
import it.pagopa.ecommerce.watchdog.deadletter.exception.RefundGatewayException
import it.pagopa.ecommerce.watchdog.deadletter.exception.TransactionNotRefundableException
import it.pagopa.generated.ecommerce.helpdesk.commands.model.RefundTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.commands.model.RefundTransactionResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.EventInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationResultDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationTypeDto
import it.pagopa.generated.ecommerce.helpdesk.model.PageInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.PaymentDetailInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.PaymentInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.PspInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
import it.pagopa.generated.ecommerce.helpdesk.model.UserInfoDto
import it.pagopa.generated.ecommerce.watchdog.transactions.v1.model.TransactionRefundRequestDto
import it.pagopa.generated.nodo.support.model.PositionPaymentSnapshotDtoDto
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class TransactionCheckServiceTest {

    private val transactionId = "6460dc3e61ba4ed6bfea2c4468b2862c"
    private val correlationId = UUID.fromString("30846e8f-efa1-47ad-abad-08cfb30e5c09")

    private val helpdeskClient: EcommerceHelpdeskServiceClient = mock()
    private val nodoClient: NodoTechnicalSupportClient = mock()
    private val commandsClient: EcommerceHelpdeskCommandsClient = mock()
    private val totpService: TotpService = mock()
    private val service =
        TransactionCheckService(helpdeskClient, nodoClient, commandsClient, totpService)

    private fun transaction(paymentGateway: String = "NPG") =
        TransactionResultDto()
            .userInfo(UserInfoDto().notificationEmail("mario.rossi@test.it"))
            .transactionInfo(
                TransactionInfoDto()
                    .eventStatus(TransactionStatusDto.NOTIFIED_OK)
                    .paymentGateway(paymentGateway)
                    .paymentMethodName("CARDS")
                    .grandTotal(12100)
                    .correlationId(correlationId)
                    .authorizationOperationId("fallback-op")
                    .gatewayAuthorizationStatus("AUTHORIZED")
                    .events(
                        listOf(
                            EventInfoDto()
                                .eventCode("TRANSACTION_ACTIVATED_EVENT")
                                .creationDate(OffsetDateTime.parse("2025-07-02T16:07:24Z"))
                        )
                    )
            )
            .paymentInfo(
                PaymentInfoDto().details(listOf(PaymentDetailInfoDto().paymentToken("token")))
            )
            .pspInfo(PspInfoDto().pspId("PSP1"))

    private val npgOperations =
        SearchNpgOperationsResponseDto()
            .operations(
                listOf(
                    OperationDto()
                        .operationId("auth-op")
                        .operationType(OperationTypeDto.AUTHORIZATION)
                        .operationResult(OperationResultDto.AUTHORIZED)
                        .operationTime("2025-07-02T16:07:30.000Z"),
                    OperationDto()
                        .operationId("capture-op")
                        .operationType(OperationTypeDto.CAPTURE)
                        .operationResult(OperationResultDto.EXECUTED)
                        .operationTime("2025-07-02T16:07:40.000Z"),
                )
            )

    private fun givenTransaction(result: TransactionResultDto? = transaction()) {
        whenever(helpdeskClient.searchTransactions(transactionId))
            .thenReturn(
                Mono.just(
                    SearchTransactionResponseDto()
                        .transactions(listOfNotNull(result))
                        .page(PageInfoDto().current(0).total(1).results(1))
                )
            )
    }

    private fun downstreamError(status: HttpStatus, body: String = "") =
        WebClientResponseException.create(
            status.value(),
            status.reasonPhrase,
            HttpHeaders().apply { add(HttpHeaders.CONTENT_TYPE, "application/json") },
            body.toByteArray(),
            StandardCharsets.UTF_8,
        )

    @Test
    fun `getTransaction builds statuses, events and details`() {
        givenTransaction()
        whenever(helpdeskClient.searchNpgOperations(transactionId))
            .thenReturn(Mono.just(npgOperations))
        whenever(nodoClient.paymentTokenPaymentTokenGet("token"))
            .thenReturn(Mono.just(PositionPaymentSnapshotDtoDto().status("PAID")))

        StepVerifier.create(service.getTransaction(transactionId))
            .assertNext { dto ->
                assertEquals(transactionId, dto.transactionId)
                assertEquals("NOTIFIED_OK", dto.geteCommerceStatus())
                assertEquals("PAID", dto.nodoStatus)
                assertEquals("EXECUTED", dto.gatewayStatus)
                assertEquals("TRANSACTION_ACTIVATED_EVENT", dto.events.single().eventCode)
                val details = dto.geteCommerceDetails() as TransactionResultDto
                assertEquals(false, details.userInfo.notificationEmail.contains("mario.rossi"))
            }
            .verifyComplete()
    }

    @Test
    fun `getTransaction tolerates missing NPG and Nodo details`() {
        givenTransaction()
        whenever(helpdeskClient.searchNpgOperations(transactionId))
            .thenReturn(Mono.error(RuntimeException("npg down")))
        whenever(nodoClient.paymentTokenPaymentTokenGet("token"))
            .thenReturn(Mono.error(RuntimeException("nodo down")))

        StepVerifier.create(service.getTransaction(transactionId))
            .assertNext { dto ->
                assertEquals("AUTHORIZED", dto.gatewayStatus)
                assertEquals(null, dto.nodoStatus)
            }
            .verifyComplete()
    }

    @Test
    fun `getTransaction and getTransactionStatus fail with 404 when the transaction does not exist`() {
        givenTransaction(null)

        StepVerifier.create(service.getTransaction(transactionId))
            .expectError(InvalidTransactionId::class.java)
            .verify()
        StepVerifier.create(service.getTransactionStatus(transactionId))
            .expectError(InvalidTransactionId::class.java)
            .verify()
    }

    @Test
    fun `getTransactionStatus returns the eCommerce status`() {
        givenTransaction()

        StepVerifier.create(service.getTransactionStatus(transactionId))
            .assertNext { assertEquals("NOTIFIED_OK", it.geteCommerceStatus()) }
            .verifyComplete()
    }

    @Test
    fun `queued refund verifies the otp and calls the commands service`() {
        whenever(totpService.verifyAndConsume("op", "123456", transactionId))
            .thenReturn(Mono.empty())
        whenever(commandsClient.requestTransactionRefund(transactionId, "op"))
            .thenReturn(Mono.empty())

        StepVerifier.create(
                service.refundTransaction(
                    transactionId,
                    "op",
                    TransactionRefundRequestDto("123456"),
                )
            )
            .expectNext(RefundOutcome.ACCEPTED)
            .verifyComplete()
    }

    @Test
    fun `queued refund does not call the commands service when the otp is invalid`() {
        whenever(totpService.verifyAndConsume("op", "123456", transactionId))
            .thenReturn(Mono.error(InvalidOtpException()))

        StepVerifier.create(
                service.refundTransaction(
                    transactionId,
                    "op",
                    TransactionRefundRequestDto("123456"),
                )
            )
            .expectError(InvalidOtpException::class.java)
            .verify()
        verify(commandsClient, never()).requestTransactionRefund(any(), any())
    }

    @Test
    fun `queued refund maps downstream 422 to not refundable and 404 to not found`() {
        whenever(totpService.verifyAndConsume("op", "123456", transactionId))
            .thenReturn(Mono.empty())
        whenever(commandsClient.requestTransactionRefund(transactionId, "op"))
            .thenReturn(Mono.error(downstreamError(HttpStatus.UNPROCESSABLE_ENTITY)))
            .thenReturn(Mono.error(downstreamError(HttpStatus.NOT_FOUND)))

        val request = TransactionRefundRequestDto("123456")
        StepVerifier.create(service.refundTransaction(transactionId, "op", request))
            .expectError(TransactionNotRefundableException::class.java)
            .verify()
        StepVerifier.create(service.refundTransaction(transactionId, "op", request))
            .expectError(InvalidTransactionId::class.java)
            .verify()
    }

    private fun forcedRefund() =
        service.refundTransaction(
            transactionId,
            "op",
            TransactionRefundRequestDto("123456").force(true),
        )

    @Test
    fun `forced refund sends the authorization operation data to the commands service`() {
        givenTransaction()
        whenever(helpdeskClient.searchNpgOperations(transactionId))
            .thenReturn(Mono.just(npgOperations))
        whenever(totpService.verifyAndConsume("op", "123456", transactionId))
            .thenReturn(Mono.empty())
        whenever(commandsClient.refundOperation(any(), any()))
            .thenReturn(Mono.just(RefundTransactionResponseDto().refundOperationId("R1")))

        StepVerifier.create(forcedRefund()).expectNext(RefundOutcome.COMPLETED).verifyComplete()

        val captor = argumentCaptor<RefundTransactionRequestDto>()
        verify(commandsClient).refundOperation(captor.capture(), any())
        with(captor.firstValue) {
            assertEquals(transactionId, this.transactionId)
            assertEquals("CARDS", paymentMethodName)
            assertEquals("PSP1", pspId)
            assertEquals("auth-op", operationId)
            assertEquals(correlationId.toString(), this.correlationId)
            assertEquals(12100, amount)
        }
    }

    @Test
    fun `forced refund falls back to the eCommerce authorization operation id`() {
        givenTransaction()
        whenever(helpdeskClient.searchNpgOperations(transactionId)).thenReturn(Mono.empty())
        whenever(totpService.verifyAndConsume("op", "123456", transactionId))
            .thenReturn(Mono.empty())
        whenever(commandsClient.refundOperation(any(), any()))
            .thenReturn(Mono.just(RefundTransactionResponseDto()))

        StepVerifier.create(forcedRefund()).expectNext(RefundOutcome.COMPLETED).verifyComplete()

        val captor = argumentCaptor<RefundTransactionRequestDto>()
        verify(commandsClient).refundOperation(captor.capture(), any())
        assertEquals("fallback-op", captor.firstValue.operationId)
    }

    @Test
    fun `forced refund maps downstream errors to 502 with the downstream detail`() {
        givenTransaction()
        whenever(helpdeskClient.searchNpgOperations(transactionId))
            .thenReturn(Mono.just(npgOperations))
        whenever(totpService.verifyAndConsume("op", "123456", transactionId))
            .thenReturn(Mono.empty())
        whenever(commandsClient.refundOperation(any(), any()))
            .thenReturn(
                Mono.error(
                    downstreamError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        """{"detail":"PSP refused"}""",
                    )
                )
            )

        StepVerifier.create(forcedRefund())
            .expectErrorSatisfies {
                assertEquals(RefundGatewayException::class, it::class)
                assertEquals("PSP refused", it.message)
            }
            .verify()
    }

    @Test
    fun `forced refund is not supported for non NPG transactions and does not consume the otp`() {
        givenTransaction(transaction(paymentGateway = "REDIRECT"))

        StepVerifier.create(forcedRefund())
            .expectError(TransactionNotRefundableException::class.java)
            .verify()
        verify(totpService, never()).verifyAndConsume(any(), any(), any())
    }
}
