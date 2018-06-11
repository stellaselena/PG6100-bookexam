package com.stella.bookexam.schema

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.io.Serializable

@ApiModel("DTO for book. It represent an book entity")
data class BookDto(

        @ApiModelProperty("The name of the book")
        var name: String?=null,

        @ApiModelProperty("Description of the book")
        var description: String?=null,

        @ApiModelProperty("Genre")
        var genre: String?=null,

        @ApiModelProperty("Author")
        var author: String?=null,

        @ApiModelProperty("Original price of the book")
        var price: Int?=null,

        @ApiModelProperty("Book rating")
        var rating: Int?=null,

        @ApiModelProperty("Book id")
        var id: String?=null
) : Serializable