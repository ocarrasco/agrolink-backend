package com.agrolink.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.agrolink.utils.StrUtils.normalizeName;

class StrUtilsTest {

  @Test
  void normalizeName_trimsAndCollapsesInternalWhitespace() {
    Assertions.assertEquals("Tomate Cherry", normalizeName("  Tomate    Cherry  "));
  }

}