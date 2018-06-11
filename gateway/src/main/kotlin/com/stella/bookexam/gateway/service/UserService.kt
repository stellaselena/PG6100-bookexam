package com.stella.bookexam.gateway.service

import com.stella.bookexam.gateway.domain.model.UserEntity
import com.stella.bookexam.gateway.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService{


    @Autowired
    private lateinit var repo: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    fun createUserWithHashedPassword(username: String, password: String, roles: Set<String> = setOf()) : Boolean {
        try {
            val hash = passwordEncoder.encode(password)

            if (repo.findUserByUsername(username)!=null) {
                return false
            }

            val user = UserEntity(username, hash, roles.map{"ROLE_$it"}.toSet())

            repo.save(user)

            return true
        } catch (e: Exception){
            return false
        }
    }
}