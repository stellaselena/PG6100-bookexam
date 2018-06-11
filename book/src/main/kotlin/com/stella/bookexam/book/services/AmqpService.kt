package com.stella.bookexam.book.services

import com.stella.bookexam.schema.BookForSaleDto
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AmqpService{
    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var fanout: FanoutExchange

    fun sendBookForSale(bookForSaleDto: BookForSaleDto){
        rabbitTemplate.convertAndSend(fanout.name, "", bookForSaleDto)
    }
}