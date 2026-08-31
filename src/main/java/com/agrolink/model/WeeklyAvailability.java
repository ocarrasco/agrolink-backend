package com.agrolink.model;

import com.agrolink.model.enums.TimeSlot;

import java.util.List;

/**
 * Weekly AM/PM availability of a supplier.
 * <p>
 * Keys mirror the frontend mock ({@code monday}..{@code sunday}, lower-case English). Persisted as JSON.
 */
public record WeeklyAvailability( //@formatter:off
    List<TimeSlot> monday,
    List<TimeSlot> tuesday,
    List<TimeSlot> wednesday,
    List<TimeSlot> thursday,
    List<TimeSlot> friday,
    List<TimeSlot> saturday,
    List<TimeSlot> sunday
) { //@formatter:on

  public static WeeklyAvailability empty() {
    return new WeeklyAvailability(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
  }

  /**
   * Returns a copy with every {@code null} day replaced by an empty list, so callers and JSON output never have to deal with nulls.
   */
  public WeeklyAvailability normalized() { //@formatter:off
    return new WeeklyAvailability(
        orEmpty(monday),
        orEmpty(tuesday),
        orEmpty(wednesday),
        orEmpty(thursday),
        orEmpty(friday),
        orEmpty(saturday),
        orEmpty(sunday)
    ); //@formatter:on
  }

  private static List<TimeSlot> orEmpty(List<TimeSlot> slots) {
    return slots == null ? List.of() : List.copyOf(slots);
  }

}
