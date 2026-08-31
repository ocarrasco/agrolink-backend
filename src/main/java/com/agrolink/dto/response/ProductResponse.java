package com.agrolink.dto.response;

import com.agrolink.model.enums.ProductUnit;

/** Trimmed view of a master product for suppliers / retailers (always active). */
public record ProductResponse(
    Integer id,
    String name,
    ProductUnit unit
) {

}
