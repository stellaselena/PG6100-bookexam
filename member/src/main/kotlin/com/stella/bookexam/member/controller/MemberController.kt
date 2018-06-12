package com.stella.bookexam.member.controller

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import com.stella.bookexam.member.domain.converter.MemberConverter
import com.stella.bookexam.member.repository.MemberRepository
import com.stella.bookexam.schema.BookDto
import com.stella.bookexam.schema.BookForSaleDto
import com.stella.bookexam.schema.MemberDto
import io.swagger.annotations.*
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import javax.validation.ConstraintViolationException


@Api(value = "/members", description = "API for member entities")
@RequestMapping(
        path = arrayOf("/members"),
        produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
)
@RestController
@Validated
class MemberController {
    @Autowired
    private lateinit var repo: MemberRepository

    @Autowired
    private lateinit var rest: RestTemplate

    @Value("\${bookServerName}")
    private lateinit var bookHost: String

    @RabbitListener(queues = arrayOf("#{queue.name}"))
    fun createMemberRabbit(memberDto: MemberDto) {

        try {
            repo.createMember(
                    username = memberDto.username!!.toLowerCase(),
                    id = memberDto.id!!,
                    books = mutableMapOf()
            )
        } catch (e: Exception) {
        }
    }

    @ApiOperation("Create new member")
    @PostMapping(consumes = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE))
    @ApiResponses(
            ApiResponse(code = 400, message = "Something is wrong with the member-body"),
            ApiResponse(code = 409, message = "Username is not unique")
    )
    fun createMember(
            @ApiParam("Member to save")
            @RequestBody
            memberDto: MemberDto): ResponseEntity<Void> {


        if (!isDtoFieldsNotNull(memberDto)) {
            return ResponseEntity.status(400).build()
        }

        memberDto.username = memberDto.username!!.toLowerCase()

        if (repo.existsByUsername(memberDto.username!!)) {
            return ResponseEntity.status(409).build()
        }

        try {
            repo.createMember(
                    username = memberDto.username!!,
                    id = memberDto.id!!,
                    books = mutableMapOf()
            )
            return ResponseEntity.status(201).build()

        } catch (e: java.lang.Exception) {
            return ResponseEntity.status(400).build()
        }
    }

    @ApiOperation("Fetch all members. Can be filtered by username")
    @GetMapping
    fun getAllMembers(
            @ApiParam("Username of member")
            @RequestParam(name = "username", required = false)
            username: String?
    ): ResponseEntity<Iterable<MemberDto>> {

        if (username != null) {
            val lowercaseUsername = username.toLowerCase()
            return ResponseEntity.ok(MemberConverter.transform(repo.findAllByUsername(lowercaseUsername)))
        }

        return ResponseEntity.ok(MemberConverter.transform(repo.findAll()))

    }

    @ApiOperation("Get member specified by id")
    @GetMapping(path = arrayOf("/{id}"))
    @ApiResponses(
            ApiResponse(code = 400, message = "Id isn't a string"),
            ApiResponse(code = 404, message = "Could not find member")
    )
    fun getMemberById(@ApiParam("Id of member")
                      @PathVariable("id")
                      pathId: String?)
            : ResponseEntity<MemberDto> {
        if (pathId.isNullOrBlank()) {
            return ResponseEntity.status(400).build()

        }

        val dto = repo.findOne(pathId) ?: return ResponseEntity.status(404).build()
        return ResponseEntity.ok(MemberConverter.transform(dto))
    }

    @ApiOperation("Replace the data of a member")
    @PutMapping(path = arrayOf("/{id}"), consumes = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE))
    @ApiResponses(
            ApiResponse(code = 400, message = "Something wrong with member-body sent in this request"),
            ApiResponse(code = 404, message = "Could not find member by this id"),
            ApiResponse(code = 409, message = "Cannot change the id of member in the body")
    )
    fun updateMember(
            @ApiParam("Id defining the member.")
            @PathVariable("id")
            pathId: String?,

            @ApiParam("Data to replace old member. Id cannot be changed, and must be the same in path and RequestBody")
            @RequestBody
            memberDto: MemberDto
    ): ResponseEntity<Void> {

        if (pathId.isNullOrBlank()) {
            return ResponseEntity.status(400).build()
        }

        // Don't change ID
        if (memberDto.id != pathId) {
            return ResponseEntity.status(409).build()
        }
        if (!repo.exists(pathId)) {
            return ResponseEntity.status(404).build()
        }
        if (!isDtoFieldsNotNull(memberDto)) {
            return ResponseEntity.status(400).build()
        }

        try {
            val successful = repo.updateMember(
                    memberDto.username!!,
                    memberDto.books!!,
                    memberDto.id!!
            )
            if (!successful) {
                return ResponseEntity.status(400).build()
            }
            return ResponseEntity.status(204).build()
        } catch (e: ConstraintViolationException) {
            return ResponseEntity.status(400).build()
        }
    }

    @ApiOperation("Replace the username of a member")
    @PatchMapping(path = arrayOf("/{id}/username"), consumes = arrayOf(MediaType.TEXT_PLAIN_VALUE))
    @ApiResponses(
            ApiResponse(code = 204, message = "Member successfully updated. No content to return"),
            ApiResponse(code = 400, message = "Something wrong with new username value"),
            ApiResponse(code = 404, message = "Could not find member to update username for.")
    )
    fun updateUsername(
            @ApiParam("Id defining the member.")
            @PathVariable("id")
            pathId: String,

            @ApiParam("New username for member.")
            @RequestBody
            username: String
    ): ResponseEntity<Void> {
        if (pathId.isNullOrBlank()) {
            return ResponseEntity.status(400).build()
        }

        if (!repo.exists(pathId)) {
            return ResponseEntity.status(404).build()
        }

        if (username.isNullOrBlank()) {
            return ResponseEntity.status(400).build()
        }
        if (!repo.updateUsername(username, pathId)) {
            return ResponseEntity.status(400).build()
        }
        return ResponseEntity.status(204).build()
    }

    @ApiOperation("Modify the member using JSON Merge Patch")
    @PatchMapping(path = arrayOf("/{id}"),
            consumes = arrayOf("application/merge-patch+json"))
    fun mergePatchMember(@ApiParam("Id of the Member")
                         @PathVariable("id")
                         id: String?,
                         @ApiParam("The partial patch")
                         @RequestBody
                         jsonPatch: String)
            : ResponseEntity<Void> {

        val dto = repo.findOne(id) ?: return ResponseEntity.status(404).build()

        val jackson = ObjectMapper()

        val jsonNode: JsonNode
        try {
            jsonNode = jackson.readValue(jsonPatch, JsonNode::class.java)
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }

        if (jsonNode.has("id")) {
            return ResponseEntity.status(409).build()
        }

        var newUsername = dto.username
        var newBooks = dto.books

        //if null, we will keep the old value for mandatory fields
        if (jsonNode.has("username")) {
            val usernameNode = jsonNode.get("username")
            if (usernameNode.isNull) {
                newUsername = dto.username
            } else if (usernameNode.isTextual) {
                newUsername = usernameNode.asText()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("books")) {

            val booksNode = jsonNode.get("books")

            if (booksNode.isNull) {
                newBooks = mutableMapOf()
            } else if (booksNode != null) {
                val temp = dto.books
                val map = jackson.convertValue(booksNode, MutableMap::class.java)
                for (m in map) {
                    val key = m.key.toString()
                    val value = m.value.toString()
                    dto.books[key] = value.toInt()
                }
                dto.books.putAll(temp)
                newBooks = dto.books

            } else {
                return ResponseEntity.status(400).build()
            }
        }

        //now that the input is validated, do the update
        dto.username = newUsername
        dto.books = newBooks

        try {
            val successful = repo.updateMember(
                    dto.username,
                    dto.books,
                    dto.id
            )
            if (!successful) {
                return ResponseEntity.status(400).build()
            }
            return ResponseEntity.status(204).build()
        } catch (e: ConstraintViolationException) {
            return ResponseEntity.status(400).build()
        }

    }

    @ApiOperation("Delete member by id")
    @DeleteMapping(path = arrayOf("/{id}"))
    @ApiResponses(
            ApiResponse(code = 204, message = "No content, member successfully deleted"),
            ApiResponse(code = 400, message = "Id is not a String"),
            ApiResponse(code = 404, message = "Could not find member")
    )
    fun deleteMember(
            @ApiParam("Id of member to delete")
            @PathVariable("id")
            pathId: String?
    ): ResponseEntity<Any> {

        if (pathId.isNullOrBlank()) {
            return ResponseEntity.status(400).build()
        }
        try {
            repo.delete(pathId)
        } catch (e: NumberFormatException) {
            return ResponseEntity.status(400).build()
        } catch (e1: java.lang.Exception) {
            return ResponseEntity.status(404).build()
        }

        return ResponseEntity.status(204).build()
    }


    @ApiOperation("Adds a book to member")
    @PostMapping(path = arrayOf("/{id}/books"), consumes = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE))

    @ApiResponses(
            ApiResponse(code = 200, message = "Book was successfully added to Member"),
            ApiResponse(code = 400, message = "Book  sent in body is not correct"),
            ApiResponse(code = 404, message = "Could not find member or book with specified id")
    )
    fun addBookToMember(@PathVariable("id")
                        id: String,
                        @RequestBody bookForSaleDto: BookForSaleDto)
            : ResponseEntity<Void> {

        if (!repo.exists(id)) {
            return ResponseEntity.status(404).build()
        }

        val member = repo.findOne(id)

        if(member.books.containsKey(bookForSaleDto.name)){
            return ResponseEntity.status(409).build()

        }

        if (bookForSaleDto.price!! <= 0 || bookForSaleDto.soldBy != member.username || bookForSaleDto.name.isNullOrBlank()) {
            return ResponseEntity.status(400).build()
        }

        val response = GetBookByName(bookForSaleDto.name!!).execute()
        if (response.statusCodeValue != 200) {
            return ResponseEntity.status(400).build()
        }

        val postedBookForSale = PostBookForSale(bookForSaleDto).execute()
        if (postedBookForSale != 200) {

            return ResponseEntity.status(400).build()

        }


        if (repo.addBook(id, bookForSaleDto.name!!, bookForSaleDto.price!!.toInt())) {

            return ResponseEntity.status(200).build()

        } else {
            return ResponseEntity.status(400).build()

        }

    }


    private inner class GetBookByName(private val name: String)
        : HystrixCommand<ResponseEntity<BookDto>>(HystrixCommandGroupKey.Factory.asKey("Call get book")) {

        override fun run(): ResponseEntity<BookDto> {

            val response: ResponseEntity<BookDto> = try {
                val bookUrl = "${bookHost}/books/name/$name"
                rest.getForEntity(bookUrl, BookDto::class.java)
            } catch (e: HttpClientErrorException) {
                return ResponseEntity.status(404).build()
            }

            return response
        }

        override fun getFallback(): ResponseEntity<BookDto> {
            return ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    private inner class PostBookForSale(private val bookForSaleDto: BookForSaleDto)
        : HystrixCommand<Int>(HystrixCommandGroupKey.Factory.asKey("Post book for sale")) {

        override fun run(): Int {

            var headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON_UTF8

            val entity = HttpEntity<BookForSaleDto>(
                    bookForSaleDto, headers)
            try {
                val bookUrl = "${bookHost}/books/store"
                rest.exchange(bookUrl, HttpMethod.POST, entity, Void::class.java)
            } catch (e: HttpClientErrorException) {

                return 404
            }

            return 200
        }

        override fun getFallback(): Int {
            return 400
        }
    }


    private fun isDtoFieldsNotNull(memberDto: MemberDto): Boolean {
        if (memberDto.username.isNullOrBlank() || memberDto.id.isNullOrBlank() || memberDto.books == null) {
            return false
        }

        return true

    }
}