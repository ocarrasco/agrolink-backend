package com.agrolink.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StrUtils {

  public static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public static String normalizeName(String raw) {
    return raw.trim().replaceAll("\\s+", " ");
  }

}
