package com.stella.bookexam.store

import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter


@Configuration
@EnableWebSecurity
class WebSecurityConfig: WebSecurityConfigurerAdapter() {


    override fun configure(http: HttpSecurity) {
        http.httpBasic()
                .and()
                .authorizeRequests()
                .antMatchers("/v2/api-docs", "/configuration/**", "/swagger-resources/**", "/swagger-ui.html", "/webjars/**", "/api-docs/**").permitAll()
                .antMatchers(HttpMethod.DELETE, "/store/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT,"/store/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PATCH,"/store/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.GET,"/store/**").permitAll()
                .antMatchers(HttpMethod.POST,"/store/**").permitAll()
                .anyRequest().denyAll()
                .and()
                .csrf().disable()
    }
}