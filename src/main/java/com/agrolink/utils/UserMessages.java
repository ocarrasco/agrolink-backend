package com.agrolink.utils;

/**
 * Mensajes de error para el usuario final, en español. Tono <b>formal e impersonal</b>:
 * tercera persona / voz pasiva refleja, sin voseo ni trato directo ("no es posible…",
 * "la orden…", "debe…"). Los nuevos mensajes deben seguir ese registro.
 */
public final class UserMessages {

  private UserMessages() {
  }

  // ── Bean Validation (DTOs) ──────────────────────────────────────
  public static final String NAME_REQUIRED = "El nombre es obligatorio.";
  public static final String NAME_TOO_LONG = "El nombre no puede superar los 120 caracteres.";
  public static final String UNIT_REQUIRED = "La unidad de medida es obligatoria.";
  public static final String MASTER_PRODUCT_ID_REQUIRED = "El producto es obligatorio.";
  public static final String PRICE_REQUIRED = "El precio es obligatorio.";
  public static final String PRICE_POSITIVE = "El precio debe ser mayor a 0.";
  public static final String STOCK_REQUIRED = "El stock es obligatorio.";
  public static final String STOCK_NOT_NEGATIVE = "El stock no puede ser negativo.";
  public static final String SUPPLIER_ID_REQUIRED = "El proveedor es obligatorio.";
  public static final String QUANTITY_REQUIRED = "La cantidad es obligatoria.";
  public static final String QUANTITY_POSITIVE = "La cantidad debe ser mayor a 0.";
  public static final String ORDER_PRODUCTS_REQUIRED = "La orden debe incluir al menos un producto.";
  public static final String NOTE_TOO_LONG = "La nota no puede superar los 500 caracteres.";
  public static final String DELIVERY_DAY_REQUIRED = "El día de entrega preferido es obligatorio.";
  public static final String DELIVERY_SLOT_REQUIRED = "El turno de entrega preferido (AM/PM) es obligatorio.";
  public static final String SHIPPING_METHOD_REQUIRED = "El método de entrega es obligatorio.";
  public static final String ADDRESS_TOO_LONG = "La dirección no puede superar los 255 caracteres.";
  public static final String PHONE_TOO_LONG = "El teléfono no puede superar los 30 caracteres.";
  public static final String CONTACT_NAME_TOO_LONG = "El nombre de contacto no puede superar los 120 caracteres.";

  // ── Validación (genérico) ───────────────────────────────────────
  public static final String VALIDATION_FAILED = "Los datos enviados contienen errores de validación.";

  // ── Producto maestro ────────────────────────────────────────────
  public static String masterProductNotFound(Object id) {
    return "El producto " + id + " no existe.";
  }

  public static String productNameTaken(String name) {
    return "Ya existe un producto con el nombre '" + name + "'.";
  }

  public static String productNotAvailable(String name) {
    return "El producto '" + name + "' no está disponible.";
  }

  // ── Catálogo del proveedor ──────────────────────────────────────
  public static String catalogItemNotFound(Object id) {
    return "El ítem de catálogo " + id + " no existe.";
  }

  public static String alreadyOffering(String productName) {
    return "El producto '" + productName + "' ya forma parte del catálogo.";
  }

  // ── Órdenes ─────────────────────────────────────────────────────
  public static final String ORDER_PRODUCTS_DUPLICATE = "Una orden no puede incluir el mismo producto más de una vez.";
  public static final String ORDER_PRODUCTS_NOT_OFFERED = "El proveedor no ofrece uno o más de los productos indicados.";
  public static final String ORDER_WITH_YOURSELF = "El comprador y el proveedor de una orden deben ser usuarios distintos.";
  public static final String SUPPLIER_HAS_NO_DELIVERY = "El proveedor seleccionado no ofrece despacho propio; solo admite retiro en finca.";
  public static final String PLATFORM_CARRIER_NOT_AVAILABLE = "El envío mediante transportista de la plataforma no está disponible por el momento.";

  public static String itemNoLongerAvailable(String productName) {
    return "El producto '" + productName + "' ya no está disponible.";
  }

  public static String notEnoughStock(String productName, Object available, Object ordered) {
    return "El stock disponible de '" + productName + "' es insuficiente (disponible: " + available + ", solicitado: " + ordered + ").";
  }

  public static String orderNotFound(Object id) {
    return "La orden " + id + " no existe.";
  }

  public static String orderCannotTransition(Object id, String action, Object status) {
    return "No es posible " + action + " la orden " + id + " (estado actual: " + status + ").";
  }

  // ── Autenticación ───────────────────────────────────────────────
  public static final String NOT_AUTHENTICATED = "No hay un usuario autenticado válido.";
  public static final String ACCOUNT_NOT_PROVISIONED = "La cuenta todavía no está habilitada en la plataforma.";
  public static final String INVALID_TOKEN_SUBJECT = "El token de autenticación no es válido.";
  public static final String ROLE_MISMATCH = "Se detectó una inconsistencia en la cuenta. Es necesario contactar al administrador.";

  // ── Sincronización con Keycloak ─────────────────────────────────
  public static String keycloakFetchFailed(String realm) {
    return "No fue posible obtener los usuarios de Keycloak (realm " + realm + ").";
  }

}
