package com.agrolink.controllers;

import com.agrolink.security.LoggedUser;
import com.agrolink.utils.UserMessages;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class BaseController {

  protected LoggedUser loggedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof LoggedUser loggedUser) {
      return loggedUser;
    }
    throw new AccessDeniedException(UserMessages.NOT_AUTHENTICATED);
  }

}
