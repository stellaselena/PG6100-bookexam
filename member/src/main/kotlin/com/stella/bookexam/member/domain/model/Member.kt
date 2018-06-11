package com.stella.bookexam.member.domain.model

import org.hibernate.validator.constraints.NotBlank
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.Id
import javax.validation.constraints.Size

@Entity
data class Member(

        @get:Id
        @get:NotBlank
        var id: String,

        @get:NotBlank
        @get:Size(max = 50)
        @get:Column(unique = true)
        var username: String,

        @get:ElementCollection
        var books: MutableMap<String, Int> = mutableMapOf(),

        var memberSince: ZonedDateTime? = null

)