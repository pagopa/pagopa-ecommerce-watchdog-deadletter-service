package it.pagopa.ecommerce.watchdog.deadletter.client

import it.pagopa.ecommerce.watchdog.deadletter.clients.EcommerceHelpdeskCommandsClient
import it.pagopa.generated.ecommerce.helpdesk.commands.api.EcommerceApi
import it.pagopa.generated.ecommerce.helpdesk.commands.model.RefundTransactionRequestDto
import it.pagopa.generated.ecommerce.helpdesk.commands.model.RefundTransactionResponseDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class EcommerceHelpdeskCommandsClientTest {

    private val commandsApi: EcommerceApi = mock()
    private val client = EcommerceHelpdeskCommandsClient(commandsApi)

    @Test
    fun `requestTransactionRefund forwards transaction id and user`() {
        whenever(commandsApi.requestTransactionRefund("tx", "user")).thenReturn(Mono.empty())

        StepVerifier.create(client.requestTransactionRefund("tx", "user")).verifyComplete()
        verify(commandsApi).requestTransactionRefund("tx", "user")
    }

    @Test
    fun `refundOperation forwards the request and returns the response`() {
        val request = RefundTransactionRequestDto().transactionId("tx")
        val response = RefundTransactionResponseDto().refundOperationId("R1")
        whenever(commandsApi.refundOperation("user", request)).thenReturn(Mono.just(response))

        StepVerifier.create(client.refundOperation(request, "user"))
            .expectNext(response)
            .verifyComplete()
    }

    @Test
    fun `errors are propagated`() {
        whenever(commandsApi.requestTransactionRefund("tx", "user"))
            .thenReturn(Mono.error(RuntimeException("boom")))

        StepVerifier.create(client.requestTransactionRefund("tx", "user"))
            .expectError(RuntimeException::class.java)
            .verify()
    }
}
