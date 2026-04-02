package it.pagopa.ecommerce.watchdog.deadletter.client

import it.pagopa.ecommerce.watchdog.deadletter.clients.NodoTechnicalSupportClient
import it.pagopa.generated.nodo.support.api.PositionPaymentSnapshotResourceApi
import it.pagopa.generated.nodo.support.model.PositionPaymentSnapshotDtoDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
class NodoTechnicalSupportClientTest {

    @Mock private lateinit var nodoTechnicalSupportApi: PositionPaymentSnapshotResourceApi

    @InjectMocks private lateinit var client: NodoTechnicalSupportClient

    @Test
    fun `paymentTokenPaymentTokenGet should call API with correct paymentToken`() {
        // Given
        val paymentToken = "TOKEN123456"
        val expectedResponse = PositionPaymentSnapshotDtoDto()

        whenever(nodoTechnicalSupportApi.paymentTokenPaymentTokenGet(eq(paymentToken), eq("")))
            .thenReturn(Mono.just(expectedResponse))

        // When
        val result = client.paymentTokenPaymentTokenGet(paymentToken)

        // Then
        StepVerifier.create(result).expectNext(expectedResponse).verifyComplete()

        verify(nodoTechnicalSupportApi).paymentTokenPaymentTokenGet(eq(paymentToken), eq(""))
    }

    @Test
    fun `paymentTokenPaymentTokenGet should pass empty string as second parameter`() {
        // Given
        val paymentToken = "TOKEN123456"
        val expectedResponse = PositionPaymentSnapshotDtoDto()

        var capturedSecondParam: String? = null
        whenever(nodoTechnicalSupportApi.paymentTokenPaymentTokenGet(eq(paymentToken), eq("")))
            .thenAnswer { invocation ->
                capturedSecondParam = invocation.getArgument(1)
                Mono.just(expectedResponse)
            }

        // When
        client.paymentTokenPaymentTokenGet(paymentToken).block()

        // Then
        assert(capturedSecondParam == "")
    }

    @Test
    fun `paymentTokenPaymentTokenGet should handle API errors`() {
        // Given
        val paymentToken = "TOKEN123456"
        val expectedException = RuntimeException("API Error")

        whenever(nodoTechnicalSupportApi.paymentTokenPaymentTokenGet(eq(paymentToken), eq("")))
            .thenReturn(Mono.error(expectedException))

        // When
        val result = client.paymentTokenPaymentTokenGet(paymentToken)

        // Then
        StepVerifier.create(result).expectError(RuntimeException::class.java).verify()
    }

    @Test
    fun `paymentTokenPaymentTokenGet should return null when API returns empty`() {
        // Given
        val paymentToken = "TOKEN123456"

        whenever(nodoTechnicalSupportApi.paymentTokenPaymentTokenGet(eq(paymentToken), eq("")))
            .thenReturn(Mono.empty())

        // When
        val result = client.paymentTokenPaymentTokenGet(paymentToken)

        // Then
        StepVerifier.create(result).verifyComplete()
    }
}
