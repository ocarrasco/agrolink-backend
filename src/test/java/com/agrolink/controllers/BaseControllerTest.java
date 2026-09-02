package com.agrolink.controllers;

import com.agrolink.model.enums.UserRole;
import com.agrolink.security.LoggedUser;
import com.agrolink.utils.UserMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseControllerTest {

  private static class TestController extends BaseController {
    LoggedUser current() {
      return loggedUser();
    }
  }

  private final TestController controller = new TestController();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void loggedUser_returnsPrincipal_whenItIsALoggedUser() {
    LoggedUser principal = new LoggedUser(7, UUID.randomUUID(), UserRole.RETAILER);
    SecurityContextHolder.getContext().setAuthentication(
        new TestingAuthenticationToken(principal, "token"));

    assertThat(controller.current()).isSameAs(principal);
  }

  @Test
  void loggedUser_throws_whenThereIsNoAuthentication() {
    assertThatThrownBy(controller::current)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage(UserMessages.NOT_AUTHENTICATED);
  }

  @Test
  void loggedUser_throws_whenPrincipalIsNotALoggedUser() {
    SecurityContextHolder.getContext().setAuthentication(
        new TestingAuthenticationToken("anonymousUser", "token"));

    assertThatThrownBy(controller::current)
        .isInstanceOf(AccessDeniedException.class)
        .hasMessage(UserMessages.NOT_AUTHENTICATED);
  }
}
