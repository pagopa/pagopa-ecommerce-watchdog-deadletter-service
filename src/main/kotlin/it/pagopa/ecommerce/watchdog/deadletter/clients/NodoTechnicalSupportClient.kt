package it.pagopa.ecommerce.watchdog.deadletter.clients

import it.pagopa.generated.nodo.support.api.PositionPaymentSnapshotResourceApi
import it.pagopa.generated.nodo.support.model.PositionPaymentSnapshotDtoDto
import java.util.function.Consumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class NodoTechnicalSupportClient(
    private val nodoTechnicalSupportApi: PositionPaymentSnapshotResourceApi
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Calls the search API with a paymentToken as search filter.
     *
     * @param paymentToken the paymentToken to search for
     * @return A Mono emitting the PositionPaymentSnapshotDtoDto or an error
     */
    fun paymentTokenPaymentTokenGet(paymentToken: String): Mono<PositionPaymentSnapshotDtoDto?> {
        return nodoTechnicalSupportApi
            .paymentTokenPaymentTokenGet(paymentToken, "")
            .doOnError(
                Consumer { throwable: Throwable? ->
                    logger.error(
                        "Error calling paymentTokenPaymentTokenGet API for paymentToken: [{}]",
                        paymentToken,
                        throwable,
                    )
                }
            )
    }
}
