package com.stella.bookexam.member.controller

import com.github.tomakehurst.wiremock.client.WireMock
import com.stella.bookexam.schema.BookDto
import com.stella.bookexam.schema.MemberDto
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class MemberControllerTest : WiremockTestBase() {
    @Before
    fun assertThatDatabaseIsEmpty() {
        RestAssured.given()
                .get().then().statusCode(200).body("size()", CoreMatchers.equalTo(0))
    }


    @Test
    fun testGetMemberEndpoint() {
        RestAssured.given()
                .get().then().statusCode(200)

    }

    /**Create a valid member**/
    @Test
    fun createAndGetMember_Valid() {
        val memberDto1 = getValidMemberDtos()[0]

        RestAssured.given()
                .auth().basic("foo", "123")
                .contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)

        val memberDto2 = getValidMemberDtos()[1]

        RestAssured.given()
                .auth().basic("foo", "123")
                .contentType(ContentType.JSON)
                .body(memberDto2)
                .post()
                .then()
                .statusCode(201)

        RestAssured.given().get().then().statusCode(200).body("size()", CoreMatchers.equalTo(2))

        val foundMember1 = RestAssured.given()
                .contentType(ContentType.JSON)
                .pathParam("id", "foo")
                .get("/{id}")
                .then()
                .statusCode(200)
                .extract()
                .`as`(MemberDto::class.java)

        Assert.assertEquals(memberDto1.username, foundMember1.username)
    }

    /**Try to create a member with unauthenticated user**/
    @Test
    fun testCreateAndGetMember_NotAuthenticated_Invalid() {
        val memberDto1 = getValidMemberDtos()[0]

        RestAssured.given()
                .auth().basic("foobar", "123")
                .contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(401)

        //verify that member hasn't been added
        RestAssured.given().get().then().statusCode(200).body("size()", CoreMatchers.equalTo(0))

    }


    /**Try to create a member with invalid body**/
    @Test
    fun testCreateAndGetMember_Invalid() {
        val memberDto1 = getValidMemberDtos()[0]
        memberDto1.username = ""

        val savedId = RestAssured.given()
                .auth().basic("foo", "123")
                .contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(400)

        // Check that nothing was saved
        RestAssured.given().get().then().statusCode(200).body("size()", CoreMatchers.equalTo(0))
    }

    /**Get all members by username**/
    @Test
    fun testGetMemberByUsername() {
        val memberDto1 = getValidMemberDtos()[0]
        postMemberDto(memberDto1, 201)

        RestAssured.given().auth().basic("foo", "123").get().then().statusCode(200).body("size()", CoreMatchers.equalTo(1))

        // Check username that doesnt exist
        val firstResult = RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .param("username", memberDto1.username?.repeat(10))
                .get()
                .then()
                .statusCode(200)
                .extract()
                .`as`(Array<MemberDto>::class.java)
        Assert.assertEquals(0, firstResult.count())

        val secondResult = RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .param("username", memberDto1.username)
                .get()
                .then()
                .statusCode(200)
                .extract()
                .`as`(Array<MemberDto>::class.java)

        Assert.assertEquals(1, secondResult.count())
        Assert.assertEquals(memberDto1.username, secondResult[0].username)
    }

    /**Update a member**/
    @Test
    fun testUpdateMember() {
        val memberDto1 = getValidMemberDtos()[0]

        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)

        val memberDto2 = MemberDto("new foo", mutableMapOf("Book6" to 20), "foo")
        RestAssured.given().auth().basic("foo", "123").get().then().statusCode(200).body("size()", CoreMatchers.equalTo(1))


        RestAssured.given().auth().basic("foo", "123")
                .pathParam("id", "foo")
                .contentType(ContentType.JSON)
                .body(memberDto2)
                .put("/{id}")
                .then()
                .statusCode(204)

        // Validate that member has changed
        RestAssured.given()
                .pathParam("id", "foo")
                .get("/{id}")
                .then()
                .statusCode(200)
                .body("username", CoreMatchers.equalTo(memberDto2.username))

        RestAssured.given().get().then().statusCode(200).body("size()", CoreMatchers.equalTo(1))
    }

    /**Try to update another member's account, should return 403**/
    @Test
    fun testUpdateMember_ShouldFail() {
        val memberDto1 = getValidMemberDtos()[0]

        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)

        val memberDto2 = MemberDto("new foo", mutableMapOf("Book6" to 20), "foo")

        RestAssured.given().auth().basic("bar", "123")
                .pathParam("id", "foo")
                .contentType(ContentType.JSON)
                .body(memberDto2)
                .put("/{id}")
                .then()
                .statusCode(403)

    }

    /**Update member's username**/
    @Test
    fun testUpdateUsername() {
        val memberDto1 = getValidMemberDtos()[0]

        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)


        RestAssured.given().auth().basic("foo", "123")
                .pathParam("id", "foo")
                .contentType(ContentType.TEXT)
                .body("newFoo")
                .patch("/{id}/username")
                .then()
                .statusCode(204)

        //Now try to update foo's account while authenticated as bar
        RestAssured.given().auth().basic("bar", "123")
                .pathParam("id", "foo")
                .contentType(ContentType.TEXT)
                .body("newBar")
                .patch("/{id}/username")
                .then()
                .statusCode(403)
    }

    /**Try to merge patch a member**/
    @Test
    fun testUpdateMember_MergePatch() {
        val memberDto1 = getValidMemberDtos()[0]

        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)


        RestAssured.given().auth().basic("foo", "123")
                .pathParam("id", memberDto1.id)
                .contentType("application/merge-patch+json")
                .body("{\"username\":\"newUsername\", \"books\":{\"book10\": 15}}")
                .patch("/{id}")
                .then()
                .statusCode(204)

        RestAssured.given()
                .pathParam("id", memberDto1.id)
                .get("/{id}")
                .then()
                .statusCode(200)
                .body("username", CoreMatchers.equalTo("newUsername"))

    }

    /**Delete member**/
    @Test
    fun testDeleteMember() {
        val memberDto1 = getValidMemberDtos()[0]
        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)
        val foundMember = RestAssured.given()
                .pathParam("id", "foo")
                .get("/{id}")
                .then()
                .statusCode(200)
                .extract()
                .`as`(MemberDto::class.java)

        RestAssured.given()
                .auth().basic("foo", "123")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", foundMember.id)
                .delete("/{id}")
                .then()
                .statusCode(204)


    }

    /**Try to delete another member's account, should get 403 access denied**/
    @Test
    fun testDeleteMember_ShouldFail() {
        val memberDto1 = getValidMemberDtos()[0]
        val memberDto2 = getValidMemberDtos()[1]

        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)

        RestAssured.given().auth().basic("bar", "123").contentType(ContentType.JSON)
                .body(memberDto2)
                .post()
                .then()
                .statusCode(201)

        //try to delete member foo's account as bar

        val foundMember = RestAssured.given()
                .pathParam("id", "foo")
                .get("/{id}")
                .then()
                .statusCode(200)
                .extract()
                .`as`(MemberDto::class.java)

        RestAssured.given()
                .auth().basic("bar", "123")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", foundMember.id)
                .delete("/{id}")
                .then()
                .statusCode(403)


    }

    /**Add books for sale**/
    @Test
    fun testAddBooksForSale_Valid(){
        val memberDto1 = getValidMemberDtos()[0]

        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)

        //mock the book that we're looking for
        wireMockServerBook.stubFor(
                WireMock.get(WireMock.urlMatching(".*/books/1"))
                        .willReturn(
                                WireMock.aResponse()
                                        .withStatus(200)))


        val bookDto = BookDto(id = 1.toString())

        RestAssured.given()
                .auth().basic("foo", "123")
                .contentType(ContentType.URLENC)
                .pathParam("id", memberDto1.id)
                .formParam("bookId", bookDto.id)
                .formParam("price", "20")
                .post("/{id}/books")
                .then()
                .statusCode(200)
    }

    /**Should return 404 Not Found when adding a book which doesn't exist (after checking against book server if the book exists)**/
    @Test
    fun testAddBooksForSale_BookNotFound(){
        val memberDto1 = getValidMemberDtos()[0]

        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)


        val bookDto = BookDto(id = 1.toString())

        RestAssured.given()
                .auth().basic("foo", "123")
                .contentType(ContentType.URLENC)
                .pathParam("id", memberDto1.id)
                .formParam("bookId", bookDto.id)
                .formParam("price", "20")
                .post("/{id}/books")
                .then()
                .statusCode(404)
    }

    /**Try to add a book to another member's account, should return 403**/
    @Test
    fun testAddBooksForSale_WrongMember(){
        val memberDto1 = getValidMemberDtos()[0]

        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto1)
                .post()
                .then()
                .statusCode(201)

        val memberDto2 = getValidMemberDtos()[1]

        RestAssured.given().auth().basic("bar", "123").contentType(ContentType.JSON)
                .body(memberDto2)
                .post()
                .then()
                .statusCode(201)


        val bookDto = BookDto(id = 1.toString())

        RestAssured.given()
                .auth().basic("bar", "123")
                .contentType(ContentType.URLENC)
                .pathParam("id", memberDto1.id)
                .formParam("bookId", bookDto.id)
                .formParam("price", "20")
                .post("/{id}/books")
                .then()
                .statusCode(403)
    }

    private fun postMemberDto(memberDto: MemberDto, expectedStatusCode: Int) {
        RestAssured.given().auth().basic("foo", "123").contentType(ContentType.JSON)
                .body(memberDto)
                .post()
                .then()
                .statusCode(expectedStatusCode)
    }

    private fun getValidMemberDtos(): List<MemberDto> {
        return listOf(
                MemberDto(
                        "foo",
                        mutableMapOf("Book1" to 5, "Book2" to 15),
                        "foo"

                ),
                MemberDto(
                        "bar",
                        mutableMapOf("Book4" to 25, "Book5" to 15),
                        "bar"
                )
        )
    }
}