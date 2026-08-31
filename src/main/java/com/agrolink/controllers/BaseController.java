package com.agrolink.controllers;

import com.agrolink.security.LoggedUser;
import com.agrolink.utils.UserMessages;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Convenience base for controllers that act on behalf of the authenticated platform user.
 * {@link com.agrolink.security.LoggedUserJwtAuthenticationConverter} makes the principal a
 * {@link LoggedUser} on every authenticated request.
 */
public abstract class BaseController {

  protected LoggedUser loggedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof LoggedUser loggedUser) {
      return loggedUser;
    }
    throw new AccessDeniedException(UserMessages.NOT_AUTHENTICATED);
  }

}
