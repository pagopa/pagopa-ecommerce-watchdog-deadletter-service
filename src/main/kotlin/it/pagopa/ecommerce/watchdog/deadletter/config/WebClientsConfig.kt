package it.pagopa.ecommerce.watchdog.deadletter.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import it.pagopa.generated.ecommerce.helpdesk.ApiClient as EcommerceHelpdeskApiClient
import it.pagopa.generated.ecommerce.helpdesk.api.ECommerceApi
import it.pagopa.generated.nodo.support.ApiClient as NodoTechnicalSupportApiClient
import it.pagopa.generated.nodo.support.api.WorkerResourceApi
import java.util.concurrent.TimeUnit
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
