package com.stella.bookexam.gateway.controller

import com.stella.bookexam.gateway.service.AmqpService
import com.stella.bookexam.gateway.service.UserService
import com.stella.bookexam.schema.MemberDto
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@Api(value = "/", description = "API for authentication.")
@RestController
@Validated
class AuthController(
        private val service: UserService,
        private val authenticationManager: AuthenticationManager,
        private val userDetailsService: UserDetailsService
){
    @Autowired
    private lateinit var amqpService: AmqpService

    @ApiOperation("Get info about current logged user")
    @ApiResponses(
            ApiResponse(code = 200, message = "Information about logged user")
    )
    @RequestMapping("/user")
    fun user(user: Principal): ResponseEntity<Map<String, Any>> {
        val map = mutableMapOf<String,Any>()
        map.put("name", user.name)
        map.put("roles", AuthorityUtils.authorityListToSet((user as Authentication).authorities))
        return ResponseEntity.ok(map)
    }

    @ApiOperation("Register new user")
    @ApiResponses(
            ApiResponse(code = 204, message = "User is registered"),
            ApiResponse(code = 400, message = "Registration failed due to wrong payload")
    )
    @PostMapping(path = arrayOf("/signIn"),
            consumes = arrayOf(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
    fun signIn(@ModelAttribute(name = "the_user") username: String,
               @ModelAttribute(name = "the_password") password: String,
               @ModelAttribute(name = "the_role") role: String)
            : ResponseEntity<Void> {

        val username = username.toLowerCase()

        val registered : Boolean
        if(role.toUpperCase() == "ADMIN"){
            registered = service.createUserWithHashedPassword(username, password, setOf("ADMIN"))

        } else{
            registered = service.createUserWithHashedPassword(username, password, setOf("USER"))

        }

        if (!registered) {
            return ResponseEntity.status(400).build()
        }
        else {
            try {
                val memberDto = MemberDto(
                        username = username,
                        books = mutableMapOf(),
                        id = username
                )
                amqpService.sendMember(memberDto)
            } catch (e: Exception) {
            }
        }

        val userDetails = userDetailsService.loadUserByUsername(username)
        val token = UsernamePasswordAuthenticationToken(userDetails, password, userDetails.authorities)

        authenticationManager.authenticate(token)

        if (token.isAuthenticated) {
            SecurityContextHolder.getContext().authentication = token
        }

        return ResponseEntity.status(204).build()
    }
}