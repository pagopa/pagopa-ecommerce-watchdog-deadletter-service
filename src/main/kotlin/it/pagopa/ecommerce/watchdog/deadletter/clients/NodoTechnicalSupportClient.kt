package it.pagopa.ecommerce.watchdog.deadletter.clients

import it.pagopa.generated.nodo.support.api.WorkerResourceApi
import it.pagopa.generated.nodo.support.model.TransactionResponseDto
import java.time.LocalDate
import java.util.function.Consumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class NodoTechnicalSupportClient(private val nodoTechnicalSupportApi: WorkerResourceApi) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Calls the search API with a noticeNumber and organizationFiscalCode as search filter.
     *
     * @param noticeNumber the notice number to search for
     * @param fiscalCode the organization fiscal code to search for
     * @return A Mono emitting the TransactionResponseDto or an error
     */
    fun searchNodoGivenNoticeNumberAndFiscalCode(
        noticeNumber: String?,
        fiscalCode: String?,
        dateFrom: LocalDate,
        dateTo: LocalDate,
    ): Mono<TransactionResponseDto?> {
        return nodoTechnicalSupportApi
            .organizationsOrganizationFiscalCodeNoticeNumberNoticeNumberGet(
                noticeNumber,
                fiscalCode,
                dateFrom,
                dateTo,
            )
            .doOnError(
                Consumer { throwable: Throwable? ->
                    logger.error(
                        "Error calling searchNodoGivenNoticeNumberAndFiscalCode API for noticeNumber: [{}], fiscalCode: [{}]",
                        noticeNumber,
                        fiscalCode,
                        throwable,
                    )
                }
            )
    }
}
