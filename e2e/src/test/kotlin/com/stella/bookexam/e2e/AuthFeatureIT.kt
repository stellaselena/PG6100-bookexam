package com.stella.bookexam.e2e

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.awaitility.Awaitility
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.util.concurrent.TimeUnit

class AuthFeatureIT {

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


    @Test
    fun testUnauthorizedAccess() {
        RestAssured.given().get("/api/v1/user")
                .then()
                .statusCode(401)
    }

    class NeededCookies(val session:String, val csrf: String)

    private fun registerUser(id: String, password: String, role: String?): NeededCookies {

        val xsrfToken = RestAssured.given().contentType(ContentType.URLENC)
                .formParam("the_user", id)
                .formParam("the_password", password)
                .formParam("the_role", role?.toUpperCase())
                .post("/api/v1/signIn")
                .then()
                .statusCode(403)
                .extract().cookie("XSRF-TOKEN")

        val session =  RestAssured.given().contentType(ContentType.URLENC)
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
    fun testLogin() {
        val id = createUniqueId()
        val pwd = "bar"

        val cookies = registerUser(id, pwd, "USER")

        RestAssured.given().get("/api/v1/user")
                .then()
                .statusCode(401)

        //note the difference in cookie name
        RestAssured.given().cookie("SESSION", cookies.session)
                .get("/api/v1/user")
                .then()
                .statusCode(200)
                .body("name", CoreMatchers.equalTo(id))
                .body("roles", Matchers.contains("ROLE_USER"))


        RestAssured.given().auth().basic(id, pwd)
                .get("/api/v1/user")
                .then()
                .statusCode(200)
                .cookie("SESSION")
                .body("name", CoreMatchers.equalTo(id))
                .body("roles", Matchers.contains("ROLE_USER"))
    }

    @Test
    fun testLoginAdmin() {
        val id = createUniqueId()
        val pwd = "bar"

        val cookies = registerUser(id, pwd, "ADMIN")

        RestAssured.given().get("/api/v1/user")
                .then()
                .statusCode(401)

        //note the difference in cookie name
        RestAssured.given().cookie("SESSION", cookies.session)
                .get("/api/v1/user")
                .then()
                .statusCode(200)
                .body("name", CoreMatchers.equalTo(id))
                .body("roles", Matchers.contains("ROLE_ADMIN"))


        RestAssured.given().auth().basic(id, pwd)
                .get("/api/v1/user")
                .then()
                .statusCode(200)
                .cookie("SESSION")
                .body("name", CoreMatchers.equalTo(id))
                .body("roles", Matchers.contains("ROLE_ADMIN"))
    }
}