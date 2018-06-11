package com.stella.bookexam.book.controller

import com.stella.bookexam.book.domain.converter.BookConverter
import com.stella.bookexam.book.repository.BookRepository
import com.stella.bookexam.schema.BookDto
import io.swagger.annotations.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import javax.validation.ConstraintViolationException

@Api(value = "/books", description = "API for book entities")
@RequestMapping(
        path = arrayOf("/books"),
        produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
)
@RestController
@Validated
class BookController {
    @Autowired
    private lateinit var repo: BookRepository

    @Autowired
    private lateinit var rest: RestTemplate

    @ApiOperation("Create new book")
    @PostMapping(consumes = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE))
    @ApiResponses(
            ApiResponse(code = 201, message = "Id of created book"),
            ApiResponse(code = 400, message = "Something is wrong with the book-body")
    )
    fun createBook(
            @ApiParam("Book to save")
            @RequestBody
            bookDto: BookDto): ResponseEntity<Long> {

        if (!bookDto.id.isNullOrEmpty()) {
            return ResponseEntity.status(400).build()
        }

        if (!isDtoFieldsNotNull(bookDto)) {
            return ResponseEntity.status(400).build()
        }

        bookDto.name = bookDto.name!!
        bookDto.description = bookDto.description!!
        bookDto.author = bookDto.author!!
        bookDto.genre = bookDto.genre
        bookDto.price = bookDto.price
        bookDto.rating = bookDto.rating

        try {
            val savedId = repo.createBook(
                    name = bookDto.name!!,
                    description = bookDto.description!!,
                    genre = bookDto.genre!!,
                    author = bookDto.author,
                    price = bookDto.price!!,
                    rating = bookDto.rating!!
            )
            return ResponseEntity.status(201).body(savedId)

        } catch (e: java.lang.Exception) {
            return ResponseEntity.status(400).build()
        }
    }

    @ApiOperation("Fetch all books. Can be filtered by name")
    @GetMapping
    fun getAllBooks(
            @ApiParam("Name of the book")
            @RequestParam(name = "name", required = false)
            name: String?
    ): ResponseEntity<Iterable<BookDto>> {

        if (name != null) {
            return ResponseEntity.ok(BookConverter.transform(repo.findAllByName(name)))
        }

        return ResponseEntity.ok(BookConverter.transform(repo.findAll()))

    }

    @ApiOperation("Get book specified by id")
    @GetMapping(path = arrayOf("/{id}"))
    @ApiResponses(
            ApiResponse(code = 400, message = "Id is not correct"),
            ApiResponse(code = 404, message = "Could not find book")
    )
    fun getBookById(@ApiParam("Id of book")
                    @PathVariable("id")
                    pathId: String?)
            : ResponseEntity<BookDto> {
        val id: Long
        try {
            id = pathId!!.toLong()
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }

        val dto = repo.findOne(id) ?: return ResponseEntity.status(404).build()
        return ResponseEntity.ok(BookConverter.transform(dto))
    }

    @ApiOperation("Replace the data of a book")
    @PutMapping(path = arrayOf("/{id}"), consumes = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE))
    @ApiResponses(
            ApiResponse(code = 400, message = "Something is wrong book-body sent in this request"),
            ApiResponse(code = 404, message = "Could not find book by this id"),
            ApiResponse(code = 409, message = "Cannot change the id of book in the body!")
    )
    fun updateBook(
            @ApiParam("Id defining the book.")
            @PathVariable("id")
            pathId: String?,

            @ApiParam("Data to replace old book. Id cannot be changed, and must be the same in path and RequestBody")
            @RequestBody
            bookDto: BookDto
    ): ResponseEntity<Long> {

        val id: Long
        try {
            id = pathId!!.toLong()
        } catch (e: Exception) {
            return ResponseEntity.status(404).build()
        }

        // Don't change ID
        if (bookDto.id != pathId) {
            return ResponseEntity.status(409).build()
        }
        if (!repo.exists(id)) {
            return ResponseEntity.status(404).build()
        }
        if (!isDtoFieldsNotNull(bookDto)) {
            return ResponseEntity.status(400).build()
        }

        try {
            val successful = repo.updateBook(
                    bookDto.name!!,
                    bookDto.description!!,
                    bookDto.genre!!,
                    bookDto.author!!,
                    bookDto.price!!,
                    bookDto.rating!!,
                    bookDto.id!!.toLong()
            )
            if (!successful) {
                return ResponseEntity.status(400).build()
            }
            return ResponseEntity.status(204).build()
        } catch (e: ConstraintViolationException) {
            return ResponseEntity.status(400).build()
        }
    }

    @ApiOperation("Replace the price of a book")
    @PatchMapping(path = arrayOf("/{id}"), consumes = arrayOf(MediaType.TEXT_PLAIN_VALUE))
    @ApiResponses(
            ApiResponse(code = 204, message = "Price successfully update. No content to return"),
            ApiResponse(code = 400, message = "Something is wrong with new price value"),
            ApiResponse(code = 404, message = "Could not find book to update price for.")
    )
    fun updatePrice(
            @ApiParam("Id defining the book.")
            @PathVariable("id")
            id: Long,
            @ApiParam("New price for book. Price cannot be negative.")
            @RequestBody
            price: String
    ): ResponseEntity<Void> {

        var priceToInt = 0
        try {
            priceToInt = price.toInt()
        } catch (e: Exception) {

        }

        if (!repo.exists(id)) {
            return ResponseEntity.status(404).build()
        }

        if (priceToInt < 0 || priceToInt > 1000) {
            return ResponseEntity.status(400).build()
        }
        if (!repo.updatePrice(priceToInt, id)) {
            return ResponseEntity.status(400).build()
        }
        return ResponseEntity.status(204).build()
    }

    @ApiOperation("Delete book by id")
    @DeleteMapping(path = arrayOf("/{id}"))
    @ApiResponses(
            ApiResponse(code = 204, message = "No content, book successfully deleted"),
            ApiResponse(code = 400, message = "Id is not correct"),
            ApiResponse(code = 404, message = "Could not find book")
    )
    fun deleteBookById(
            @ApiParam("Id of book to delete")
            @PathVariable("id")
            pathId: Long?
    ): ResponseEntity<Any> {

        val id: Long
        try {
            id = pathId!!.toLong()
            repo.delete(id)
        } catch (e: NumberFormatException) {
            return ResponseEntity.status(400).build()
        } catch (e1: java.lang.Exception) {
            return ResponseEntity.status(404).build()
        }

        return ResponseEntity.status(204).build()
    }


    private fun isDtoFieldsNotNull(bookDto: BookDto): Boolean {
        if (bookDto.name.isNullOrBlank() || bookDto.description.isNullOrBlank() || bookDto.genre.isNullOrBlank()
                || bookDto.author.isNullOrBlank() || bookDto.price == null || bookDto.price == 0 || bookDto.rating == null || bookDto.rating == 0 || bookDto.rating!! > 5) {
            return false
        }

        return true

    }
}