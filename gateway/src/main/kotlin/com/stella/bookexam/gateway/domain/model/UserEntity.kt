package com.stella.bookexam.gateway.domain.model

import org.hibernate.validator.constraints.NotBlank
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name="USERS")
class UserEntity(

        @get:Id
        @get:NotBlank
        var username: String?,

        @get:NotBlank
        var password: String?,

        @get:ElementCollection
        @get:NotNull
        var roles: Set<String>? = setOf(),

        @get:NotNull
        var enabled: Boolean? = true
)