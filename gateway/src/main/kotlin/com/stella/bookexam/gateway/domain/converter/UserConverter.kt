package com.stella.bookexam.gateway.domain.converter

import com.stella.bookexam.gateway.domain.model.UserEntity
import com.stella.bookexam.gateway.domain.dto.UserDto


class UserConverter {
    companion object {

        fun transform(entity: UserEntity): UserDto {
            return UserDto(
                    username = entity.username,
                    password = entity.password,
                    roles = entity.roles,
                    enabled = entity.enabled
            )
        }

        fun transform(entities: Iterable<UserEntity>): Iterable<UserDto> {
            return entities.map { transform(it) }
        }
    }
}