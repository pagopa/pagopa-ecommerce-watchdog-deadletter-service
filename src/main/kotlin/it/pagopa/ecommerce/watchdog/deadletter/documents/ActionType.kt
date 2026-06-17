package it.pagopa.ecommerce.watchdog.deadletter.documents

import it.pagopa.generated.ecommerce.watchdog.deadletter.v1.model.ActionTypeDto as DtoV1
import it.pagopa.generated.ecommerce.watchdog.deadletter.v2.model.ActionTypeDto as DtoV2

data class ActionType(val value: String, val type: Type) {
    enum class Type {
        FINAL,
        NOT_FINAL,
    }

    companion object {
        inline fun <reified T> fromDto(dto: T): ActionType {
            return when (dto) {
                is DtoV1 -> ActionType(dto.value, Type.valueOf(dto.type.name))
                is DtoV2 -> ActionType(dto.value, Type.valueOf(dto.type.name))
                else -> throw IllegalArgumentException("not a valid data type ${T::class}")
            }
        }
    }

    inline fun <reified T> toDto(): T {
        return when (T::class) {
            DtoV1::class -> DtoV1(this.value, DtoV1.TypeEnum.valueOf(this.type.name))
            DtoV2::class -> DtoV2(this.value, DtoV2.TypeEnum.valueOf(this.type.name))
            else -> throw IllegalArgumentException("not a valid data type ${T::class}")
        }
            as T
    }
}
