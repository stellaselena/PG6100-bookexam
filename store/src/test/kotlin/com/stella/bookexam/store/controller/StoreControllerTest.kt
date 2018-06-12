package com.stella.bookexam.store.controller

import com.stella.bookexam.schema.BookForSaleDto
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Test

class StoreControllerTest : TestBase(){
    @Test
    fun testCleanDB() {
        RestAssured.given()
                .get().then()
                .statusCode(200)
                .body("size()", CoreMatchers.equalTo(0))
    }

    /**Create a book for sale**/
    @Test
    fun testCreateBookForSale() {
        var dto = getValidBookForSaleDto()

        // valid dto
        val id = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(201)
                .extract().`as`(Long::class.java)
        Assert.assertNotNull(id)

        // invalid dto
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("wrongBody")
                .post()
                .then()
                .statusCode(400)

    }

    /**Get a book for sale**/
    @Test
    fun testGetBookForSale() {
        val dto = getValidBookForSaleDto()
        val id = postBookForSaleValid(dto)

        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get().then().statusCode(200)
                .body("size()", CoreMatchers.equalTo(1))

        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .param("name", dto.name)
                .get().then().statusCode(200)
                .body("size()", CoreMatchers.equalTo(1))

        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .param("name", "wrongUsername")
                .get().then()
                .statusCode(200).body("size()", CoreMatchers.equalTo(0))
    }

    /**Get last 10 books posted for sale**/
    @Test
    fun testGetLast10BookForSale() {
        val dto = getValidBookForSaleDto()
        postBookForSaleValid(dto)
        postBookForSaleValid(dto)
        postBookForSaleValid(dto)
        val lastBook = postBookForSaleValid(dto)

        val result = RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/last").then().statusCode(200)
                .body("size()", CoreMatchers.equalTo(4))
                .extract()
                .`as`(Array<BookForSaleDto>::class.java)
        //verify that the first result is the last book posted
        Assert.assertEquals(lastBook, result[0].id!!.toLong())
    }

    /**Get a book for sale by id**/
    @Test
    fun testGetBookForSaleById() {
        val dto = getValidBookForSaleDto()
        val id = postBookForSaleValid(dto)


        //Get a valid book for sale
        val response1 = RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", id)
                .get("/{id}")
                .then()
                .statusCode(200)
                .extract()
                .`as`(BookForSaleDto::class.java)
        Assert.assertTrue(dto.name == response1.name)

        //Invalid id param
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", "invalid_input")
                .get("/{id}")
                .then()
                .statusCode(400)

        //Non existing id

        val nonExistId = 555
        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", nonExistId)
                .get("{id}")
                .then()
                .statusCode(404)
    }

    /**Get all books sold by a member**/
    @Test
    fun testGetAllBookForSaleByMember() {

        val dto = getValidBookForSaleDto()
        dto.soldBy = "foo"
        postBookForSaleValid(dto)
        postBookForSaleValid(dto)

        RestAssured.given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("soldBy", "foo")
                .get("{soldBy}/books")
                .then().statusCode(200)
                .body("size()", CoreMatchers.equalTo(2))


    }

    /**Delete a book for sale by id**/
    @Test
    fun testDeleteBookForSale() {
        val dto = getValidBookForSaleDto()
        dto.soldBy = "foo"
        val id = postBookForSaleValid(dto)


        //Valid
        RestAssured.given()
                .contentType(ContentType.JSON)
                .auth().basic("foo", "123")
                .accept(ContentType.JSON)
                .pathParam("id", id)
                .pathParam("soldBy", "foo")
                .delete("/{soldBy}/book/{id}")
                .then()
                .statusCode(204)

        //Invalid id input
        RestAssured.given()
                .contentType(ContentType.JSON)
                .auth().basic("foo", "123")
                .accept(ContentType.JSON)
                .pathParam("id", "invalid_input")
                .pathParam("soldBy", "foo")
                .delete("/{soldBy}/book/{id}")
                .then()
                .statusCode(400)

    }

    /**Try to merge patch a book for sale**/
    @Test
    fun testUpdateBookForSale_MergePatch() {
        val dto = getValidBookForSaleDto()
        dto.soldBy = "foo"
        val id = postBookForSaleValid(dto)
        //try to json patch merge with a valid user (the owner of the book for sale)
        RestAssured.given()
                .auth().basic("foo", "123")
                .pathParam("id", id)
                .pathParam("soldBy", "foo")
                .contentType("application/merge-patch+json")
                .body("{\"name\":\"newName\"}")
                .patch("/{soldBy}/book/{id}")
                .then()
                .statusCode(204)

        //try to json patch merge with an invalid user (not the owner of the book for sale)
        RestAssured.given()
                .auth().basic("bar", "123")
                .pathParam("id", id)
                .pathParam("soldBy", "foo")
                .contentType("application/merge-patch+json")
                .body("{\"name\":\"newName\", \"soldBy\":null}")
                .patch("/{soldBy}/book/{id}")
                .then()
                .statusCode(403)

        RestAssured.given()
                .pathParam("id", id)
                .get("/{id}")
                .then()
                .statusCode(200)
                .body("name", CoreMatchers.equalTo("newName"))
                .body("soldBy", CoreMatchers.equalTo(dto.soldBy))

    }


    fun getValidBookForSaleDto():BookForSaleDto {

        return BookForSaleDto("bookName", "bookOwner", 5)
    }

    fun postBookForSaleValid(dto: BookForSaleDto) : Long{
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body(dto)
                .post()
                .then()
                .statusCode(201)
                .extract().`as`(Long::class.java)
    }
}