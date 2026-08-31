package com.agrolink.dto;

import com.agrolink.model.enums.ProductUnit;

/** Trimmed view of a master product for suppliers / retailers (always active). */
public record ProductResponse(
    Integer id,
    String name,
    ProductUnit unit
) {

}
