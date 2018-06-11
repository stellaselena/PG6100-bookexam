package com.stella.bookexam.member

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {


    override fun configure(http: HttpSecurity) {
        http.httpBasic()
                .and()
                .authorizeRequests()
                .antMatchers("/v2/api-docs", "/configuration/**", "/swagger-resources/**", "/swagger-ui.html", "/webjars/**", "/api-docs/**").permitAll()
                .antMatchers(HttpMethod.PATCH, "/members/{id}/**").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
                .antMatchers(HttpMethod.PUT, "/members/{id}/**").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
                .antMatchers(HttpMethod.POST, "/members/{id}/**").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
                .antMatchers(HttpMethod.DELETE, "/members/{id}/**").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
                .antMatchers(HttpMethod.POST, "/members/{id}/books").access("hasRole('USER') and @userSecurity.checkId(authentication, #id)")
                .antMatchers(HttpMethod.POST, "/members").hasRole("USER")
                .antMatchers(HttpMethod.GET, "/members/**").permitAll()
                .anyRequest().denyAll()
                .and()
                .csrf().disable()

    }

    @Bean
    fun userSecurity(): UserSecurity {
        return UserSecurity()
    }
}

class UserSecurity {

    fun checkId(authentication: Authentication, id: String): Boolean {

        val current = (authentication.principal as UserDetails).username

        return current == id
    }
}


