package com.agrolink.dto.response;

import com.agrolink.dto.enums.Trend;

/**
 * A metric's current-month-vs-previous-month comparison: absolute change, % change, and trend.
 */
public record MonthOverMonth( //@formatter:off
    long currentMonth,
    long previousMonth,
    long absoluteChange,
    Integer percentChange,
    Trend trend
) { //@formatter:on

  public MonthOverMonth(long currentMonth, long previousMonth) {
    this(currentMonth, previousMonth, currentMonth - previousMonth,
        percentChangeOf(currentMonth, previousMonth), trendOf(currentMonth, previousMonth));
  }

  private static Integer percentChangeOf(long currentMonth, long previousMonth) {
    if (previousMonth == 0L) {
      return null;
    }
    long absoluteChange = currentMonth - previousMonth;
    return Math.toIntExact(Math.round(absoluteChange * 100.0 / previousMonth));
  }

  private static Trend trendOf(long currentMonth, long previousMonth) {
    if (currentMonth > previousMonth) {
      return Trend.UP;
    }
    if (currentMonth < previousMonth) {
      return Trend.DOWN;
    }
    return Trend.FLAT;
  }

}
