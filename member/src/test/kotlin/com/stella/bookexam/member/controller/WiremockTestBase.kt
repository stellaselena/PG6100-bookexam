package com.stella.bookexam.member.controller

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.stella.bookexam.member.repository.MemberRepository
import io.restassured.RestAssured
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.util.logging.Logger


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class WiremockTestBase {

    private val logger : Logger = Logger.getLogger(WiremockTestBase::class.java.canonicalName)
    @Autowired
    private lateinit var memberRepository: MemberRepository
    companion object {
        lateinit var wireMockServerBook: WireMockServer

        @BeforeClass
        @JvmStatic
        fun initClass() {
            RestAssured.baseURI = "http://localhost"
            RestAssured.port = 7081
            RestAssured.basePath = "/members"
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()

            wireMockServerBook = WireMockServer(WireMockConfiguration.wireMockConfig().port(8082).notifier(ConsoleNotifier(true)))

            wireMockServerBook.start()

        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            wireMockServerBook.stop()
        }
    }

    @Before
    @After
    fun cleanDatabase() {

        memberRepository.deleteAll()

    }
}