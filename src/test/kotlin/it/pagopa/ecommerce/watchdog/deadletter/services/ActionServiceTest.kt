package it.pagopa.ecommerce.watchdog.deadletter.services
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import reactor.test.StepVerifier
import kotlin.test.assertEquals

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.test.properties"])
class ActionServiceTest {

    @Autowired private lateinit var actionService: ActionService


    @Test
    fun `getActionType should return the Flux of ActionTypeDto with the ActionType available`(){
        StepVerifier.create(actionService.getActionType())
            .assertNext { action ->
                assertEquals(action.type, ActionTypeDto.TypeEnum.FINAL, "The action type is not the same")
                assertEquals(action.value,"Nessuna azione richiesta", "The action value is not the same")
            }.verifyComplete()
    }


}