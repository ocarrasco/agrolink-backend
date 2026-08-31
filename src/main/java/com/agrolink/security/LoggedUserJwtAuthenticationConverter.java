package com.agrolink.security;

import com.agrolink.model.UserModel;
import com.agrolink.model.enums.UserRole;
import com.agrolink.services.UserService;
import com.agrolink.utils.UserMessages;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Runs inside {@code BearerTokenAuthenticationFilter}: turns a validated {@link Jwt} into a
 * {@link PreAuthenticatedAuthenticationToken} whose principal is a {@link LoggedUser}.
 * <ul>
 *   <li><b>ADMIN</b> → {@code LoggedUser(id=null, ...)}, no DB lookup (admins are not in
 *       {@code platform_user}).</li>
 *   <li>Other roles → resolve the active {@code platform_user} row; 401 {@code account_not_provisioned}
 *       if absent.</li>
 *   <li>If the JWT's primary role disagrees with {@code platform_user.role} → 401
 *       {@code role_mismatch} (hard block; the frontend shows a "contact the admin" screen).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggedUserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private static final Converter<Jwt, Collection<GrantedAuthority>> AUTHORITIES = new KeycloakRealmRoleConverter();

  private final UserService userService;

  @Override
  public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
    var authorities = AUTHORITIES.convert(jwt);
    UserRole jwtRole = primaryRole(authorities);
    UUID keycloakId = subject(jwt);

    if (jwtRole == UserRole.ADMIN) {
      return authenticated(new LoggedUser(null, keycloakId, UserRole.ADMIN), jwt, authorities);
    }

    UserModel user = userService.findActiveUserByKeycloakId(keycloakId)
        .orElseThrow(() -> reject("account_not_provisioned", UserMessages.ACCOUNT_NOT_PROVISIONED));

    if (jwtRole != user.getRole()) {
      log.warn("Role mismatch for user {}: JWT says {}, platform_user says {}",
          keycloakId, jwtRole, user.getRole());
      throw reject("role_mismatch", UserMessages.ROLE_MISMATCH);
    }

    return authenticated(new LoggedUser(user.getId(), keycloakId, user.getRole()), jwt, authorities);
  }

  private static PreAuthenticatedAuthenticationToken authenticated(
      LoggedUser principal, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
    return new PreAuthenticatedAuthenticationToken(principal, jwt, authorities);
  }

  private UUID subject(Jwt jwt) {
    try {
      return UUID.fromString(jwt.getSubject());
    } catch (IllegalArgumentException e) {
      throw reject("invalid_subject", UserMessages.INVALID_TOKEN_SUBJECT);
    }
  }

  private UserRole primaryRole(Collection<GrantedAuthority> authorities) {
    return authorities.stream()
        .map(GrantedAuthority::getAuthority)
        .map(UserRole::fromAuthority)
        .flatMap(Optional::stream)
        .findFirst()
        .orElse(null);
  }

  private OAuth2AuthenticationException reject(String code, String description) {
    return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
  }

}
