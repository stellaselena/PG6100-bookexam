package com.stella.bookexam.e2e

import com.stella.bookexam.schema.BookDto
import com.stella.bookexam.schema.BookForSaleDto
import com.stella.bookexam.schema.MemberDto
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.util.concurrent.TimeUnit

class ModulesIT {
    companion object {

        class KDockerComposeContainer(path: File) : DockerComposeContainer<KDockerComposeContainer>(path)


        @ClassRule
        @JvmField
        val env = KDockerComposeContainer(File("../docker-compose.yml"))
                .withLocalCompose(true)

        private var counter = System.currentTimeMillis()

        @BeforeClass
        @JvmStatic
        fun initialize() {
            RestAssured.baseURI = "http://localhost"
            RestAssured.port = 10000
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()


            Awaitility.await().atMost(240, TimeUnit.SECONDS)
                    .ignoreExceptions()
                    .until({
                        RestAssured.given().get("http://localhost:10000/api/v1/user").then().statusCode(401)

                        true
                    })
        }
    }


    class NeededCookies(val session: String, val csrf: String)

    private fun registerUser(id: String, password: String, role: String?): NeededCookies {

        val xsrfToken = RestAssured.given().contentType(ContentType.URLENC)
                .formParam("the_user", id)
                .formParam("the_password", password)
                .formParam("the_role", role?.toUpperCase())
                .post("/api/v1/signIn")
                .then()
                .statusCode(403)
                .extract().cookie("XSRF-TOKEN")

        val session = RestAssured.given().contentType(ContentType.URLENC)
                .formParam("the_user", id)
                .formParam("the_password", password)
                .formParam("the_role", role?.toUpperCase())
                .header("X-XSRF-TOKEN", xsrfToken)
                .cookie("XSRF-TOKEN", xsrfToken)
                .post("/api/v1/signIn")
                .then()
                .statusCode(204)
                .extract().cookie("SESSION")

        return NeededCookies(session, xsrfToken)
    }

    private fun createUniqueId(): String {
        counter++
        return "foo_$counter"
    }


    @Test
    fun testAccessToModulesAndRabbitMq() {

        /***********************************************Region Book API***********************************************/


        /**Performing GET requests to book-server while not authenticated will return 401 status code**/

        RestAssured.given().get("/api/v1/book-server/books").then().statusCode(401)

        /**When authenticated, both USER and ADMIN roles can perform GET requests to book-server**/

        //login as user
        val id = createUniqueId()
        val cookies = registerUser(id, "password", "USER")

        RestAssured.given().cookie("SESSION", cookies.session)
                .get("/api/v1/book-server/books")
                .then().statusCode(200)

        /**An user cannot perform POST requests to book-server**/

        val book = BookDto("bookName", "bookDescription", "bookGenre", "bookAuthor", 25, 4)
        val book2 = BookDto("bookName2", "bookDescription2", "bookGenre2", "bookAuthor2", 50, 5)

        //Post
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .body(book)
                .post("/api/v1/book-server/books")
                .then()
                .statusCode(403)

        /**Only admin can perform POST requests to book-server**/

        //Login as admin
        val id2 = createUniqueId()
        val cookies2 = registerUser(id2, "password", "ADMIN")

        //Post
        val bookId = RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .contentType(ContentType.JSON)
                .body(book)
                .post("/api/v1/book-server/books")
                .then()
                .statusCode(201)
                .extract()
                .`as`(Long::class.java)

        /**An user cannot perform PUT or PATCH requests to book-server**/

        //Json merge patch
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", bookId)
                .contentType("application/merge-patch+json")
                .body("{\"name\":null, \"description\":null, \"genre\":\"newGenre\", \"price\":30, \"rating\":2}")
                .patch("/api/v1/book-server/books/{id}")
                .then()
                .statusCode(403)

        //Put
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", bookId)
                .contentType(ContentType.TEXT)
                .body(book2.price.toString())
                .patch("/api/v1/book-server/books/{id}/price")
                .then()
                .statusCode(403)

        /**Admin can perform PUT or PATCH requests to book-server**/

        //Json merge patch
        RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .pathParam("id", bookId)
                .contentType("application/merge-patch+json")
                .body("{\"name\":null, \"description\":null, \"genre\":\"newGenre\", \"price\":30, \"rating\":2}")
                .patch("/api/v1/book-server/books/{id}")
                .then()
                .statusCode(204)

        //Put
        RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .pathParam("id", bookId)
                .contentType(ContentType.TEXT)
                .body(book2.price.toString())
                .patch("/api/v1/book-server/books/{id}/price")
                .then()
                .statusCode(204)

        //Get the updated book
        val returnedBook = RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .pathParam("id", bookId)
                .contentType(ContentType.JSON)
                .get("/api/v1/book-server/books/{id}")
                .then()
                .statusCode(200)
                .extract()
                .`as`(BookDto::class.java)

        //Get a book by its name
        RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .pathParam("name", returnedBook.name)
                .contentType(ContentType.JSON)
                .get("/api/v1/book-server/books/name/{name}")
                .then()
                .statusCode(200)

        /**An user cannot perform DELETE requests to book-server**/

        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", bookId)
                .delete("/api/v1/book-server/books/{id}")
                .then()
                .statusCode(403)

        /**Admin can perform DELETE requests to book-server**/

        RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .pathParam("id", bookId)
                .delete("/api/v1/book-server/books/{id}")
                .then()
                .statusCode(204)

        /***********************************************Region Member API***********************************************/

        /**RabbitMq(when creating user send a message to member module and create a member for that user**/

        Awaitility.await().atMost(100, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until({
                    RestAssured.given().cookie("SESSION", cookies.session)
                            .get("/api/v1/member-server/members")
                            .then()
                            .statusCode(200)
                            .and()
                            .body("username", CoreMatchers.hasItem(id))
                    true
                })

        /**All authenticated users can perform GET request to member-server**/

        RestAssured.given()
                .get("/api/v1/member-server/members")
                .then()
                .statusCode(401)

        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .get("/api/v1/member-server/members")
                .then()
                .statusCode(200)

        /**A logged in user is be able to perform PUT requests (i.e. changing his info), but not of other users**/

        //find the members created via RabbitMq
        val member1 = RestAssured.given()
                .cookie("SESSION", cookies.session)
                .pathParam("id", id)
                .contentType(ContentType.JSON)
                .get("/api/v1/member-server/members/{id}")
                .then()
                .statusCode(200)
                .extract()
                .`as`(MemberDto::class.java)

        val member2 = RestAssured.given()
                .cookie("SESSION", cookies.session)
                .pathParam("id", id)
                .contentType(ContentType.JSON)
                .get("/api/v1/member-server/members/{id}")
                .then()
                .statusCode(200)
                .extract()
                .`as`(MemberDto::class.java)

        val memberDto = MemberDto("foobar", member1.books, id)

        //Try to modify own info
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", id)
                .contentType(ContentType.JSON)
                .body(memberDto)
                .put("/api/v1/member-server/members/{id}")
                .then()
                .statusCode(204)

        //Try to modify info of another user
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", id2)
                .contentType(ContentType.JSON)
                .body(memberDto)
                .put("/api/v1/member-server/members/{id}")
                .then()
                .statusCode(403)

        /**A logged in user is able to specify books that he has a copy of
         *  that he wants to sell and for how much, but not for other users.
         *
         *  When a user successfully adds a book for sale, the book server is
         *  notified, and then sends a message to store-server via RabbitMq,
         *  which then posts the book for sale
         *  **/

        val bookForSaleDto = BookForSaleDto(name = returnedBook.name, soldBy = member1.username, price = 30)

//        Add a book that a member is selling
//        Awaitility.await().atMost(300, TimeUnit.SECONDS)
//                .ignoreExceptions()
//                .until({
//                    RestAssured.given()
//                            .cookie("SESSION", cookies.session)
//                            .header("X-XSRF-TOKEN", cookies.csrf)
//                            .cookie("XSRF-TOKEN", cookies.csrf)
//                            .pathParam("id", member1.id)
//                            .contentType(ContentType.JSON)
//                            .body(bookForSaleDto)
//                            .post("/api/v1/member-server/members/{id}/books")
//                            .then()
//                            .statusCode(200)
//                    true
//                })

        //Try to do the same for another user
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", id2)
                .contentType(ContentType.JSON)
                .body(bookForSaleDto)
                .post("/api/v1/member-server/members/{id}/books")
                .then()
                .statusCode(403)


        /**A logged in user is be able to perform PATCH requests, but not for other users**/

        //Json merge patch
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", id)
                .contentType("application/merge-patch+json")
                .body("{\"username\":null, \"books\":{\"book10\": 15}}")
                .patch("/api/v1/member-server/members/{id}")
                .then()
                .statusCode(204)

        //Try the same for another user
        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", id2)
                .contentType("application/merge-patch+json")
                .body("{\"username\":null, \"books\":{\"book10\": 15}}")
                .patch("/api/v1/member-server/members/{id}")
                .then()
                .statusCode(403)


        /**A logged in user is be able to perform DELETE requests, but not for other users**/

        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", id)
                .delete("/api/v1/member-server/members/{id}")
                .then()
                .statusCode(204)

        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", id2)
                .delete("/api/v1/member-server/members/{id}")
                .then()
                .statusCode(403)

        /***********************************************Region Store API***********************************************/

        /**Only logged in users can access the book store**/

        RestAssured.given()
                .get("/api/v1/store-server/store")
                .then().statusCode(401)

        RestAssured.given().cookie("SESSION", cookies.session)
                .get("/api/v1/store-server/store")
                .then().statusCode(200)



        /**Only from the store can users can also sell books they want to sell
         *  that are not listed in existing books**/

        val bookForSaleId = RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .contentType(ContentType.JSON)
                .body(BookForSaleDto("new book by foo", id, 5))
                .post("/api/v1/store-server/store")
                .then()
                .statusCode(201)
                .extract()
                .`as`(Long::class.java)

        val bookForSaleId2 = RestAssured.given()
                .cookie("SESSION", cookies2.session)
                .header("X-XSRF-TOKEN", cookies2.csrf)
                .cookie("XSRF-TOKEN", cookies2.csrf)
                .contentType(ContentType.JSON)
                .body(BookForSaleDto("new book", id2, 5))
                .post("/api/v1/store-server/store")
                .then()
                .statusCode(201)
                .extract()
                .`as`(Long::class.java)

        //Not allowed if not authenticated
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(BookForSaleDto("new book by foo", id, 5))
                .post("/api/v1/store-server/store")
                .then()
                .statusCode(403)

        /**Users can perform PATCH and PUT for books that they are selling (such as editing
         * the price), but cannot perform these actions for other users**/

        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("id", bookForSaleId)
                .pathParam("soldBy", member1.id)
                .contentType("application/merge-patch+json")
                .body("{\"name\":\"newName\"}")
                .patch("/api/v1/store-server/store/{soldBy}/book/{id}")
                .then()
                .statusCode(204)

        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("soldBy", id2)
                .pathParam("id", bookForSaleId2)
                .contentType("application/merge-patch+json")
                .body("{\"name\":\"newName\"}")
                .patch("/api/v1/store-server/store/{soldBy}/book/{id}")
                .then()
                .statusCode(403)

        /**Users can perform DELETE for books that they've set for sale, but not for books
         * that other users have posted**/

        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("soldBy", id)
                .pathParam("id", bookForSaleId)
                .delete("/api/v1/store-server/store/{soldBy}/book/{id}")
                .then()
                .statusCode(204)

        RestAssured.given()
                .cookie("SESSION", cookies.session)
                .header("X-XSRF-TOKEN", cookies.csrf)
                .cookie("XSRF-TOKEN", cookies.csrf)
                .pathParam("soldBy", id2)
                .pathParam("id", bookForSaleId2)
                .delete("/api/v1/store-server/store/{soldBy}/book/{id}")
                .then()
                .statusCode(403)

    }
}