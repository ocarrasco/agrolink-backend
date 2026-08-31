package com.agrolink.services;

import com.agrolink.dto.SupplierDashboardResponse;
import com.agrolink.dto.SupplierDashboardResponse.Trend;
import com.agrolink.repositories.IOrderRepository;
import com.agrolink.repositories.projections.MonthlyFulfilled;
import com.agrolink.repositories.projections.ProductSales;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierDashboardServiceTest {

  @Mock
  private IOrderRepository orderRepository;

  @InjectMocks
  private SupplierDashboardService service;

  private static final YearMonth AUG = YearMonth.of(2026, 8);

  @org.junit.jupiter.api.BeforeEach
  void noProductSalesByDefault() {
    lenient().when(orderRepository.productSales(any())).thenReturn(List.of());
  }

  @Test
  void comparesThisMonthAgainstTheLastAndBuildsA3MonthTrend() {
    when(orderRepository.monthlyFulfilledSince(eq(1), any())).thenReturn(List.of(row(2026, 6, 200_000, 2), row(2026, 7, 400_000, 4), row(2026, 8, 500_000, 5)));

    SupplierDashboardResponse dashboard = service.build(1, AUG);

    assertThat(dashboard.sales().currentMonth()).isEqualTo(500_000);
    assertThat(dashboard.sales().previousMonth()).isEqualTo(400_000);
    assertThat(dashboard.sales().absoluteChange()).isEqualTo(100_000);
    assertThat(dashboard.sales().percentChange()).isEqualTo(25);
    assertThat(dashboard.sales().trend()).isEqualTo(Trend.UP);

    assertThat(dashboard.completedOrders().currentMonth()).isEqualTo(5);
    assertThat(dashboard.completedOrders().previousMonth()).isEqualTo(4);
    assertThat(dashboard.completedOrders().absoluteChange()).isEqualTo(1);

    assertThat(dashboard.salesTrend()).containsExactly(new SupplierDashboardResponse.MonthlyAmount(2026, 6, 200_000), new SupplierDashboardResponse.MonthlyAmount(2026, 7, 400_000),
        new SupplierDashboardResponse.MonthlyAmount(2026, 8, 500_000));
  }

  @Test
  void treatsMissingMonthsAsZero() {
    when(orderRepository.monthlyFulfilledSince(eq(1), any())).thenReturn(List.of(row(2026, 8, 300_000, 3)));

    SupplierDashboardResponse dashboard = service.build(1, AUG);

    assertThat(dashboard.sales().previousMonth()).isZero();
    assertThat(dashboard.sales().percentChange()).isNull(); // no se puede % contra 0
    assertThat(dashboard.sales().trend()).isEqualTo(Trend.UP);
    assertThat(dashboard.salesTrend()).extracting(SupplierDashboardResponse.MonthlyAmount::amount).containsExactly(0L, 0L, 300_000L);
  }

  @Test
  void reportsADownTrendWhenSalesDropped() {
    when(orderRepository.monthlyFulfilledSince(eq(1), any())).thenReturn(List.of(row(2026, 7, 1_000_000, 10), row(2026, 8, 750_000, 8)));

    SupplierDashboardResponse dashboard = service.build(1, AUG);

    assertThat(dashboard.sales().absoluteChange()).isEqualTo(-250_000);
    assertThat(dashboard.sales().percentChange()).isEqualTo(-25);
    assertThat(dashboard.sales().trend()).isEqualTo(Trend.DOWN);
  }

  @Test
  void handlesASupplierWithNoSales() {
    when(orderRepository.monthlyFulfilledSince(eq(1), any())).thenReturn(List.of());

    SupplierDashboardResponse dashboard = service.build(1, AUG);

    assertThat(dashboard.sales().currentMonth()).isZero();
    assertThat(dashboard.sales().trend()).isEqualTo(Trend.FLAT);
    assertThat(dashboard.sales().percentChange()).isNull();
    assertThat(dashboard.salesTrend()).hasSize(3).allMatch(month -> month.amount() == 0L);
  }

  private static MonthlyFulfilled row(int year, int month, long amount, long orderCount) {
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

  @Test
  void ranksTheTop3ProductsAndBucketsTheRestAsOtros() {
    when(orderRepository.productSales(1)).thenReturn(List.of(
        productRow(10, "Tomate", 600_000),
        productRow(20, "Papa", 300_000),
        productRow(30, "Cebolla", 60_000),
        productRow(40, "Lechuga", 30_000),
        productRow(50, "Zanahoria", 10_000)));

    var top = service.build(1, AUG).topProducts();

    assertThat(top).hasSize(4);
    assertThat(top).extracting(SupplierDashboardResponse.ProductShare::productName)
        .containsExactly("Tomate", "Papa", "Cebolla", "Otros");
    assertThat(top).extracting(SupplierDashboardResponse.ProductShare::percent)
        .containsExactly(60, 30, 6, 4);
    assertThat(top.get(3).masterProductId()).isNull();
    assertThat(top.get(3).amount()).isEqualTo(40_000);
    assertThat(top).extracting(SupplierDashboardResponse.ProductShare::percent)
        .satisfies(percents -> assertThat(percents.stream().mapToInt(Integer::intValue).sum()).isEqualTo(100));
  }

  @Test
  void topProductsHasNoOtrosWhenThereAre3OrFewerProducts() {
    when(orderRepository.productSales(1)).thenReturn(List.of(
        productRow(10, "Tomate", 700_000),
        productRow(20, "Papa", 300_000)));

    var top = service.build(1, AUG).topProducts();

    assertThat(top).extracting(SupplierDashboardResponse.ProductShare::productName)
        .containsExactly("Tomate", "Papa");
  }

  @Test
  void topProductsIsEmptyWithoutFulfilledSales() {
    when(orderRepository.productSales(1)).thenReturn(List.of());

    assertThat(service.build(1, AUG).topProducts()).isEmpty();
  }

  private static ProductSales productRow(int id, String name, long amount) {
    return new ProductSales() {
      public Integer getProductId() {
        return id;
      }

      public String getProductName() {
        return name;
      }

      public long getAmount() {
        return amount;
      }
    };
  }

}
