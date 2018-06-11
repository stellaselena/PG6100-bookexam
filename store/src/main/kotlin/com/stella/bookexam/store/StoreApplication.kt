package com.stella.bookexam.store

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

@SpringBootApplication(scanBasePackages = arrayOf("com.stella.bookexam.store"))
@EnableEurekaClient
class StoreApplication : WebMvcConfigurerAdapter() {}

fun main(args: Array<String>) {
    SpringApplication.run(StoreApplication::class.java, *args)
}