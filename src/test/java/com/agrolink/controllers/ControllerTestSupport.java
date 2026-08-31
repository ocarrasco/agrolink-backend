package com.agrolink.controllers;

import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/** Shared helpers for {@code @WebMvcTest} + {@code @Import(SecurityConfig.class)} slice tests. */
abstract class ControllerTestSupport {

  /** Drives the request with a real {@link LoggedUser} principal, as {@code BaseController.loggedUser()} expects. */
  protected static RequestPostProcessor loggedAs(UserRole role) {
    var authorities = List.of(new SimpleGrantedAuthority(role.authority()));
    var principal = new LoggedUser(1, UUID.randomUUID(), role);
    return authentication(new PreAuthenticatedAuthenticationToken(principal, "jwt-token", authorities));
  }

  /** Reads a fixture from {@code src/test/resources} (e.g. {@code "request/CreateOrderRequest_OK.json"}). */
  protected static String readResource(String classpathLocation) {
    try {
      return new String(new ClassPathResource(classpathLocation).getInputStream().readAllBytes(),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo leer " + classpathLocation, e);
    }
  }

}
