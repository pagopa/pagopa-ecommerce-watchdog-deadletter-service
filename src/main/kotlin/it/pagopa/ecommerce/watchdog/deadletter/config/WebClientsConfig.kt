package it.pagopa.ecommerce.watchdog.deadletter.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import it.pagopa.generated.ecommerce.helpdesk.ApiClient as EcommerceHelpdeskApiClient
import it.pagopa.generated.ecommerce.helpdesk.api.ECommerceApi
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterEventDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterExcludedStatusesDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchDateTimeRangeDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterSearchEventSourceDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterTransactionInfoDetailsDto
import it.pagopa.generated.ecommerce.helpdesk.model.DeadLetterTransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.EcommerceSearchDeadLetterEventsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.NpgTransactionInfoDetailsDataDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationAdditionalDataDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationResultDto
import it.pagopa.generated.ecommerce.helpdesk.model.OperationTypeDto
import it.pagopa.generated.ecommerce.helpdesk.model.PaymentDetailInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.PaymentInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.PaymentMethodDto
import it.pagopa.generated.ecommerce.helpdesk.model.ProductDto
import it.pagopa.generated.ecommerce.helpdesk.model.PspInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.RedirectTransactionInfoDetailsDataDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchDeadLetterEventResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsRequestDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchNpgOperationsResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionRequestTransactionIdDto
import it.pagopa.generated.ecommerce.helpdesk.model.SearchTransactionResponseDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionInfoDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionResultDto
import it.pagopa.generated.ecommerce.helpdesk.model.TransactionStatusDto
import it.pagopa.generated.ecommerce.helpdesk.model.UserInfoDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.PageInfoDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ProblemJsonDto
import it.pagopa.generated.nodo.support.ApiClient as NodoTechnicalSupportApiClient
import it.pagopa.generated.nodo.support.api.WorkerResourceApi
import it.pagopa.generated.nodo.support.model.BasePaymentInfoDto
import it.pagopa.generated.nodo.support.model.ErrorCodeDto
import it.pagopa.generated.nodo.support.model.InfoResponseDto
import it.pagopa.generated.nodo.support.model.TransactionResponseDto
import java.util.concurrent.TimeUnit
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.NameResolverProvider.NameResolverSpec

@Configuration
class WebClientsConfig {

    @Bean(name = ["ecommerceHelpdeskClient"])
    @RegisterReflectionForBinding(
        SearchDeadLetterEventResponseDto::class,
        EcommerceSearchDeadLetterEventsRequestDto::class,
        DeadLetterEventDto::class,
        DeadLetterExcludedStatusesDto::class,
        DeadLetterSearchDateTimeRangeDto::class,
        DeadLetterSearchEventSourceDto::class,
        DeadLetterTransactionInfoDetailsDto::class,
        DeadLetterTransactionInfoDto::class,
        EcommerceSearchDeadLetterEventsRequestDto::class,
        NpgTransactionInfoDetailsDataDto::class,
        OperationAdditionalDataDto::class,
        OperationDto::class,
        OperationResultDto::class,
        OperationTypeDto::class,
        PageInfoDto::class,
        PaymentDetailInfoDto::class,
        PaymentInfoDto::class,
        PaymentMethodDto::class,
        ProblemJsonDto::class,
        ProductDto::class,
        PspInfoDto::class,
        RedirectTransactionInfoDetailsDataDto::class,
        SearchDeadLetterEventResponseDto::class,
        SearchNpgOperationsRequestDto::class,
        SearchNpgOperationsResponseDto::class,
        SearchTransactionRequestTransactionIdDto::class,
        SearchTransactionResponseDto::class,
        TransactionInfoDto::class,
        TransactionResultDto::class,
        TransactionStatusDto::class,
        UserInfoDto::class,
    )
    fun ecommerceHelpdeskClient(
        @Value("\${ecommerce-helpdesk.server.uri}") serverUri: String,
        @Value("\${ecommerce-helpdesk.server.readTimeoutMillis}") readTimeoutMillis: Int,
        @Value("\${ecommerce-helpdesk.server.connectionTimeoutMillis}") connectionTimeoutMillis: Int,
    ): WebClient {

        val httpClient =
            HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillis)
                .doOnConnected { connection: Connection ->
                    connection.addHandlerLast(
                        ReadTimeoutHandler(readTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                    )
                }
                .resolver { nameResolverSpec: NameResolverSpec -> nameResolverSpec.ndots(1) }

        return EcommerceHelpdeskApiClient.buildWebClientBuilder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(serverUri)
            .build()
    }

    @Bean
    fun eCommerceHelpdeskApi(
        @Value("\${ecommerce-helpdesk.server.uri}") serverUri: String,
        @Value("\${ecommerce-helpdesk.apiKey}") apiKey: String,
        ecommerceHelpdeskClient: WebClient,
    ): ECommerceApi {
        val apiClient = EcommerceHelpdeskApiClient(ecommerceHelpdeskClient)
        apiClient.setBasePath(serverUri)
        apiClient.setApiKey(apiKey)
        return ECommerceApi(apiClient)
    }

    @Bean(name = ["nodoTechnicalSupportWebClient"])
    @RegisterReflectionForBinding(
        BasePaymentInfoDto::class,
        ErrorCodeDto::class,
        InfoResponseDto::class,
        ProblemJsonDto::class,
        TransactionResponseDto::class,
    )
    fun nodoTechnicalSupportWebClient(
        @Value("\${nodo-technical-support.server.uri}") serverUri: String,
        @Value("\${nodo-technical-support.server.readTimeoutMillis}") readTimeoutMillis: Int,
        @Value("\${nodo-technical-support.server.connectionTimeoutMillis}")
        connectionTimeoutMillis: Int,
    ): WebClient {

        val httpClient =
            HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillis)
                .doOnConnected { connection: Connection ->
                    connection.addHandlerLast(
                        ReadTimeoutHandler(readTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                    )
                }
                .resolver { nameResolverSpec: NameResolverSpec -> nameResolverSpec.ndots(1) }

        return NodoTechnicalSupportApiClient.buildWebClientBuilder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(serverUri)
            .build()
    }

    @Bean
    fun nodoTechnicalSupportApi(
        @Value("\${nodo-technical-support.server.uri}") serverUri: String,
        @Value("\${nodo-technical-support.apiKey}") apiKey: String,
        nodoTechnicalSupportWebClient: WebClient,
    ): WorkerResourceApi {
        val apiClient = NodoTechnicalSupportApiClient(nodoTechnicalSupportWebClient)
        apiClient.setBasePath(serverUri)
        apiClient.setApiKey(apiKey)
        return WorkerResourceApi(apiClient)
    }
}
