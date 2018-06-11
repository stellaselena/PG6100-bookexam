package com.stella.bookexam.gateway.service

import com.stella.bookexam.schema.MemberDto
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AmqpService {

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var fanout: FanoutExchange

    fun sendMember(memberDto: MemberDto) {
        rabbitTemplate.convertAndSend(fanout.name, "", memberDto)
    }

}