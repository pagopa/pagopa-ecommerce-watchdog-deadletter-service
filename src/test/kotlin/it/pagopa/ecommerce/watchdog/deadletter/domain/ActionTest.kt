package it.pagopa.ecommerce.watchdog.deadletter.domain

import it.pagopa.ecommerce.watchdog.deadletter.documents.ActionType
import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto as DtoV1
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.ActionTypeDto as DtoV2
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.test.properties"])
class ActionTest {

    @Test
    fun `fromDto should return the ActionType from ActionTypeDto for any dto version`() {
        val actionTypeDtoV1 = DtoV1("TEST", DtoV1.TypeEnum.FINAL)
        val actionTypeDtoV2 = DtoV2("TEST", DtoV2.TypeEnum.FINAL)

        val actionTypeV1 = ActionType.fromDto(actionTypeDtoV1)
        val actionTypeV2 = ActionType.fromDto(actionTypeDtoV2)

        assertEquals(actionTypeV1.value, actionTypeDtoV1.value)
        assertEquals(actionTypeV1.type.name, actionTypeDtoV1.type.name)

        assertEquals(actionTypeV2.value, actionTypeDtoV2.value)
        assertEquals(actionTypeV2.type.name, actionTypeDtoV2.type.name)

        assertThrows<IllegalArgumentException> { ActionType.fromDto("Test") }
    }

    @Test
    fun `toDto should return the ActionTypeDto related to ActionType based on dto version`() {
        val actionType = ActionType("TEST", ActionType.Type.FINAL)

        val actionTypeDtoV1 = actionType.toDto<DtoV1>()
        val actionTypeDtoV2 = actionType.toDto<DtoV2>()

        assertEquals(actionTypeDtoV1.value, actionType.value)
        assertEquals(actionTypeDtoV1.type.name, actionType.type.name)

        assertEquals(actionTypeDtoV2.value, actionType.value)
        assertEquals(actionTypeDtoV2.type.name, actionType.type.name)

        assertThrows<IllegalArgumentException> { actionType.toDto<String>() }
    }
}
