package it.pagopa.ecommerce.watchdog.deadletter.services
import arrow.core.raise.result
import it.pagopa.ecommerce.watchdog.deadletter.config.ActionTypeConfig
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.test.properties"])
class ActionServiceTest {

    @Autowired private val actionConfig : ActionTypeConfig = ActionTypeConfig()
    private val actionService: ActionService = ActionService(actionConfig)
    private val actionTypesList : ArrayList<ActionTypeDto> = ArrayList()


    @Test
    fun `getActionType should return the Flux of ActionTypeDto with the ActionType available`(){
        StepVerifier.create(actionService.getActionType())
            .expectNextMatches {
                result -> result.javaClass == ActionTypeDto::class.java &&
                result.type == ActionTypeDto.TypeEnum.NOT_FINAL
            }
    }


}