package com.agrolink.boostrap;

import com.agrolink.model.WeeklyAvailability;

/**
 * One entry of {@code src/main/resources/init/suppliers.json}: a supplier's bootstrap data,
 * keyed by the user's Keycloak {@code email}. Unknown JSON fields are ignored (Boot's default
 * {@code ObjectMapper}). {@code availability} uses the same shape as {@link WeeklyAvailability}
 * (lower-case day keys, {@code AM}/{@code PM}).
 * <p>
 * {@code catalogSize} = how many random master products to give the supplier as a starter
 * catalog; {@code 0} / absent = none.
 */
record SupplierSeed(
    String email,
    String contactName,
    String address,
    String phone,
    boolean delivery,
    WeeklyAvailability availability,
    int catalogSize
) {

}
