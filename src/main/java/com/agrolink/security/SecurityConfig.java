package com.agrolink.security;

import com.agrolink.utils.UserMessages;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.agrolink.security.PathConstants.PUBLIC_PATHS;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@SuppressWarnings("java:S4502")
public class SecurityConfig {

    private final LoggedUserJwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityConfig(LoggedUserJwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationEntryPoint jsonAuthenticationEntryPoint) throws Exception { //@formatter:off
    return http.csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
          .requestMatchers(PUBLIC_PATHS).permitAll()
          .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2
          .authenticationEntryPoint(jsonAuthenticationEntryPoint)
          .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
      )
      .build(); //@formatter:on
    }

    @Bean
    AuthenticationEntryPoint jsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, authException) -> {
            String error = "unauthorized";
            String message = UserMessages.NOT_AUTHENTICATED;
            if (authException instanceof OAuth2AuthenticationException oauthException) {
                error = oauthException.getError().getErrorCode();
                if (StringUtils.hasText(oauthException.getError().getDescription())) {
                    message = oauthException.getError().getDescription();
                }
            }
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of("error", error, "message", message));
        };
    }

}
