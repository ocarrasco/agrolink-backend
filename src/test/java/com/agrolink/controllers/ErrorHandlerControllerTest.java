package com.agrolink.controllers;

import com.agrolink.services.DuplicateResourceException;
import com.agrolink.services.KeycloakSyncException;
import com.agrolink.utils.UserMessages;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErrorHandlerControllerTest {

  private final ErrorHandlerController handler = new ErrorHandlerController();

  @Test
  void handleNotFound_maps404() {
    ProblemDetail problem = handler.handleNotFound(new EntityNotFoundException("La orden 9 no existe."));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(problem.getDetail()).isEqualTo("La orden 9 no existe.");
  }

  @Test
  void handleDuplicate_maps409() {
    ProblemDetail problem = handler.handleDuplicate(new DuplicateResourceException("Ya existe."));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getDetail()).isEqualTo("Ya existe.");
  }

  @Test
  void handleIllegalState_maps409() {
    ProblemDetail problem = handler.handleIllegalState(new IllegalStateException("Transición inválida."));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(problem.getDetail()).isEqualTo("Transición inválida.");
  }

  @Test
  void handleKeycloakSync_maps502() {
    ProblemDetail problem = handler.handleKeycloakSync(new KeycloakSyncException("Keycloak caído.", new RuntimeException()));

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
    assertThat(problem.getDetail()).isEqualTo("Keycloak caído.");
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleValidation_maps400_withFieldErrors_firstMessagePerFieldWins() {
    BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "createOrderRequest");
    binding.addError(new FieldError("createOrderRequest", "supplierId", "no puede ser nulo"));
    binding.addError(new FieldError("createOrderRequest", "products", "faltan productos"));
    binding.addError(new FieldError("createOrderRequest", "products", "segundo mensaje ignorado"));

    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(binding);

    ProblemDetail problem = handler.handleValidation(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(problem.getDetail()).isEqualTo(UserMessages.VALIDATION_FAILED);

    assertNotNull(problem.getProperties());

    Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
    assertThat(errors)
        .containsEntry("supplierId", "no puede ser nulo")
        .containsEntry("products", "faltan productos");
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleValidation_maps400_withEmptyErrors_whenNoFieldErrors() {
    BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");

    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    when(ex.getBindingResult()).thenReturn(binding);

    ProblemDetail problem = handler.handleValidation(ex);

    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat((Map<String, String>) problem.getProperties().get("errors")).isEmpty();
  }
}
