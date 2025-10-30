package it.pagopa.ecommerce.watchdog.deadletter.controllers

import it.pagopa.ecommerce.watchdog.deadletter.config.TestSecurityConfig
import it.pagopa.ecommerce.watchdog.deadletter.services.ActionService
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ListDeadletterTransactions200ResponseDto
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.EntityExchangeResult
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@WebFluxTest(ActionController::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
@Import(TestSecurityConfig::class)
class ActionControllerTest {

    @Autowired private lateinit var webClient: WebTestClient
    @MockitoBean lateinit var actionService: ActionService

    @Test
    fun `listActions should return 200 OKAY with the list of the action available`(){
        val actionTypesList : ArrayList<ActionTypeDto> = ArrayList()
        actionTypesList.add(ActionTypeDto("test1", ActionTypeDto.TypeEnum.NOT_FINAL))

        // For catch the generic type during the Test
        val listType = object : ParameterizedTypeReference<List<ActionTypeDto>>() {}

        given(actionService.getActionType())
            .willReturn(
                Flux.fromIterable(actionTypesList)
            )

        webClient
            .get()
            .uri { uriBuilder ->
                uriBuilder
                    .path("/actions")
                    .build()
            }
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(listType)
            .consumeWith { result : EntityExchangeResult<List<ActionTypeDto>> ->
                val list = result.responseBody
                assertNotNull(list)
                assertTrue(list.isNotEmpty())
                assertEquals("test1", list[0].value)
                assertEquals(ActionTypeDto.TypeEnum.NOT_FINAL, list[0].type)
            }

    }

}