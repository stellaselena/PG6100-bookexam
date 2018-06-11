package com.stella.bookexam.gateway

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.zuul.EnableZuulProxy

@EnableZuulProxy
@SpringBootApplication
class GatewayApplication {}

fun main(args: Array<String>) {
    SpringApplication.run(GatewayApplication::class.java, *args)
}