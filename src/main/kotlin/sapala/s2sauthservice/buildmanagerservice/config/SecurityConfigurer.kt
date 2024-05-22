package sapala.s2sauthservice.buildmanagerservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import sapala.s2sauthservice.api.security.RequestFilter


@EnableWebSecurity
@Configuration
class SecurityConfigurer(private val requestFilter: RequestFilter) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http.authorizeHttpRequests {
            it.requestMatchers("/receive-token").authenticated()
                .anyRequest().permitAll()
        }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(requestFilter, UsernamePasswordAuthenticationFilter::class.java)
            .csrf { it.disable() }
            .build()
    }
}