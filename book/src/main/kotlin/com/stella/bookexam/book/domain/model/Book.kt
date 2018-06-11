package com.stella.bookexam.book.domain.model

import org.hibernate.validator.constraints.NotBlank
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.Size

@Entity
data class Book(

        @get:NotBlank
        @get:Size(min=1, max = 50)
        @get:Column(unique = true)
        var name: String,

        @get:Size(min=1, max = 250)
        var description: String? = null,

        @get:Size(min=1, max = 50)
        var genre: String? = null,

        @get:Size(max = 50)
        var author: String? = null,

        @get:Min(0)
        var price: Int? = null,

        @get:Min(0)
        @get:Max(5)
        var rating: Int? = null,

        @get:Id
        @get: GeneratedValue
        var id: Long? = null
)