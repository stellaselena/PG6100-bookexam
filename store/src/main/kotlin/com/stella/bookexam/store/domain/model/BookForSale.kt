package com.stella.bookexam.store.domain.model

import org.hibernate.validator.constraints.NotBlank
import java.time.ZonedDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
data class BookForSale(
        @get:NotBlank @get:Size(max=32)
        var name: String,
        @get:NotBlank @get:Size(max=32)
        var soldBy: String,
        @get:NotNull
        var price: Int,
        @get:Id @get:GeneratedValue
        var id: Long? = null,
        var createdOn: ZonedDateTime? = null

)