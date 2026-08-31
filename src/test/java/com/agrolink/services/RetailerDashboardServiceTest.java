package com.agrolink.services;

import com.agrolink.dto.enums.Trend;
import com.agrolink.dto.response.RetailerDashboardResponse;
import com.agrolink.repositories.IOrderRepository;
import com.agrolink.repositories.projections.MonthlyCount;
import com.agrolink.repositories.projections.MonthlyFulfilled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Month;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetailerDashboardServiceTest {

  private static final YearMonth AUG = YearMonth.of(2026, Month.AUGUST);

  @Mock
  private IOrderRepository orderRepository;

  @InjectMocks
  private RetailerDashboardService service;

  @BeforeEach
  void noActivityByDefault() {
    lenient().when(orderRepository.monthlyFulfilledSinceForRetailer(any(), any())).thenReturn(List.of());
    lenient().when(orderRepository.monthlyPlacedSinceForRetailer(any(), any())).thenReturn(List.of());
  }

  @Test
  void comparesCompletedOrdersAndInvestmentAgainstTheLastMonth() {
    when(orderRepository.monthlyFulfilledSinceForRetailer(eq(1), any()))
        .thenReturn(List.of(fulfilledRow(2026, 7, 400_000, 4), fulfilledRow(2026, 8, 500_000, 5)));

    RetailerDashboardResponse dashboard = service.build(1, AUG);

    assertThat(dashboard.completedOrders().currentMonth()).isEqualTo(5);
    assertThat(dashboard.completedOrders().previousMonth()).isEqualTo(4);
    assertThat(dashboard.completedOrders().absoluteChange()).isEqualTo(1);
    assertThat(dashboard.completedOrders().percentChange()).isEqualTo(25);
    assertThat(dashboard.completedOrders().trend()).isEqualTo(Trend.UP);

    assertThat(dashboard.investment().currentMonth()).isEqualTo(500_000);
    assertThat(dashboard.investment().previousMonth()).isEqualTo(400_000);
    assertThat(dashboard.investment().percentChange()).isEqualTo(25);
    assertThat(dashboard.investment().trend()).isEqualTo(Trend.UP);
  }

  @Test
  void comparesPlacedOrdersAgainstTheLastMonth() {
    when(orderRepository.monthlyPlacedSinceForRetailer(eq(1), any()))
        .thenReturn(List.of(placedRow(2026, 7, 6), placedRow(2026, 8, 3)));

    RetailerDashboardResponse dashboard = service.build(1, AUG);

    assertThat(dashboard.placedOrders().currentMonth()).isEqualTo(3);
    assertThat(dashboard.placedOrders().previousMonth()).isEqualTo(6);
    assertThat(dashboard.placedOrders().absoluteChange()).isEqualTo(-3);
    assertThat(dashboard.placedOrders().trend()).isEqualTo(Trend.DOWN);
  }

  @Test
  void treatsAMissingPreviousMonthAsZero() {
    when(orderRepository.monthlyFulfilledSinceForRetailer(eq(1), any()))
        .thenReturn(List.of(fulfilledRow(2026, 8, 300_000, 3)));
    when(orderRepository.monthlyPlacedSinceForRetailer(eq(1), any()))
        .thenReturn(List.of(placedRow(2026, 8, 5)));

    RetailerDashboardResponse dashboard = service.build(1, AUG);

    assertThat(dashboard.completedOrders().previousMonth()).isZero();
    assertThat(dashboard.completedOrders().percentChange()).isNull(); // no se puede % contra 0
    assertThat(dashboard.completedOrders().trend()).isEqualTo(Trend.UP);
    assertThat(dashboard.investment().previousMonth()).isZero();
    assertThat(dashboard.placedOrders().previousMonth()).isZero();
  }

  @Test
  void reportsAFlatTrend_whenNothingChanged() {
    when(orderRepository.monthlyPlacedSinceForRetailer(eq(1), any()))
        .thenReturn(List.of(placedRow(2026, 7, 4), placedRow(2026, 8, 4)));

    RetailerDashboardResponse dashboard = service.build(1, AUG);

    assertThat(dashboard.placedOrders().absoluteChange()).isZero();
    assertThat(dashboard.placedOrders().percentChange()).isZero();
    assertThat(dashboard.placedOrders().trend()).isEqualTo(Trend.FLAT);
  }

  @Test
  void handlesARetailerWithNoActivity() {
    RetailerDashboardResponse dashboard = service.build(1, AUG);

    assertThat(dashboard.completedOrders().currentMonth()).isZero();
    assertThat(dashboard.completedOrders().trend()).isEqualTo(Trend.FLAT);
    assertThat(dashboard.investment().currentMonth()).isZero();
    assertThat(dashboard.investment().trend()).isEqualTo(Trend.FLAT);
    assertThat(dashboard.placedOrders().currentMonth()).isZero();
    assertThat(dashboard.placedOrders().trend()).isEqualTo(Trend.FLAT);
  }

  private static MonthlyFulfilled fulfilledRow(int year, int month, long amount, long orderCount) {
    return new MonthlyFulfilled() {

      public int getYr() {
        return year;
      }

      public int getMo() {
        return month;
      }

      public long getAmount() {
        return amount;
      }

      public long getOrderCount() {
        return orderCount;
      }
    };
  }

  private static MonthlyCount placedRow(int year, int month, long orderCount) {
    return new MonthlyCount() {

      public int getYr() {
        return year;
      }

      public int getMo() {
        return month;
      }

      public long getOrderCount() {
        return orderCount;
      }
    };
  }

}
