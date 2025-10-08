package it.pagopa.ecommerce.watchdog.deadletter;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedResponse;
import com.azure.core.http.rest.PagedResponseBase;
import com.azure.security.keyvault.certificates.models.CertificateProperties;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AzureTestUtils {

    public PagedFlux<CertificateProperties> getCertificatePropertiesPagedFlux(
            List<CertificateProperties> certificatePropertiesList
    ) {
        HttpHeaders httpHeaders = new HttpHeaders().set(HttpHeaderName.fromString("header1"), "value1")
                .set(HttpHeaderName.fromString("header2"), "value2");
        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, "http://localhost");

        String deserializedHeaders = "header1,value1,header2,value2";
        List<PagedResponse<CertificateProperties>> pagedResponses = IntStream.range(0, certificatePropertiesList.size())
                .boxed()
                .map(
                        i -> createPagedResponse(
                                httpRequest,
                                httpHeaders,
                                deserializedHeaders,
                                i,
                                certificatePropertiesList,
                                certificatePropertiesList.size()
                        )
                )
                .collect(Collectors.toList());

        return new PagedFlux<>(
                () -> pagedResponses.isEmpty() ? Mono.empty() : Mono.just(pagedResponses.getFirst()),
                continuationToken -> getNextPage(continuationToken, pagedResponses)
        );
    }

    private Mono<PagedResponse<CertificateProperties>> getNextPage(
            String continuationToken,
            List<PagedResponse<CertificateProperties>> pagedResponses
    ) {

        if (continuationToken == null || continuationToken.isEmpty()) {
            return Mono.empty();
        }

        int parsedToken = Integer.parseInt(continuationToken);
        if (parsedToken >= pagedResponses.size()) {
            return Mono.empty();
        }

        return Mono.just(pagedResponses.get(parsedToken));
    }

    private PagedResponseBase<String, CertificateProperties> createPagedResponse(
            HttpRequest httpRequest,
            HttpHeaders httpHeaders,
            String deserializedHeaders,
            int i,
            List<CertificateProperties> certificatePropertiesList,
            int noOfPages
    ) {
        return new PagedResponseBase<>(
                httpRequest,
                200,
                httpHeaders,
                Collections.singletonList(certificatePropertiesList.get(i)),
                i < noOfPages - 1 ? String.valueOf(i + 1) : null,
                deserializedHeaders
        );
    }
}
