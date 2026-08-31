package com.agrolink.services;

import com.agrolink.dto.SupplierDashboardResponse;
import com.agrolink.dto.SupplierDashboardResponse.MonthOverMonth;
import com.agrolink.dto.SupplierDashboardResponse.MonthlyAmount;
import com.agrolink.dto.SupplierDashboardResponse.ProductShare;
import com.agrolink.dto.SupplierDashboardResponse.Trend;
import com.agrolink.repositories.IOrderRepository;
import com.agrolink.repositories.projections.MonthlyFulfilled;
import com.agrolink.repositories.projections.ProductSales;
import com.agrolink.security.LoggedUser;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierDashboardService {

  private static final int TREND_MONTHS = 3;
  private static final int TOP_PRODUCTS = 3;
  private static final String OTHERS_LABEL = "Otros";

  @NonNull
  private final IOrderRepository orderRepository;

  @Transactional(readOnly = true)
  public SupplierDashboardResponse getDashboard(LoggedUser supplier) {
    return build(supplier.id(), YearMonth.now(ZoneOffset.UTC));
  }

  SupplierDashboardResponse build(Integer supplierId, YearMonth current) {
    YearMonth firstMonth = current.minusMonths(TREND_MONTHS - 1L);
    LocalDateTime since = firstMonth.atDay(1).atStartOfDay();

    var byMonth = orderRepository.monthlyFulfilledSince(supplierId, since).stream().collect(Collectors.toMap(row -> YearMonth.of(row.getYr(), row.getMo()), Function.identity()));

    YearMonth previous = current.minusMonths(1);
    MonthOverMonth sales = compare(amountOf(byMonth, current), amountOf(byMonth, previous));
    MonthOverMonth completedOrders = compare(countOf(byMonth, current), countOf(byMonth, previous));

    List<MonthlyAmount> trend = new ArrayList<>(TREND_MONTHS);
    for (int i = 0; i < TREND_MONTHS; i++) {
      YearMonth month = firstMonth.plusMonths(i);
      trend.add(new MonthlyAmount(month.getYear(), month.getMonthValue(), amountOf(byMonth, month)));
    }

    List<ProductShare> topProducts = topProducts(orderRepository.productSales(supplierId));

    return new SupplierDashboardResponse(sales, completedOrders, trend, topProducts);
  }

  /**
   * Top {@value #TOP_PRODUCTS} products by amount + an "Otros" bucket for the rest. Percentages
   * are of the total; "Otros" absorbs the rounding drift so they always sum to 100.
   */
  private static List<ProductShare> topProducts(List<ProductSales> rows) {
    long total = rows.stream().mapToLong(ProductSales::getAmount).sum();
    if (total == 0L) {
      return List.of();
    }

    List<ProductShare> shares = new ArrayList<>();
    int rankedPercent = 0;
    long rankedAmount = 0L;
    int ranked = Math.min(TOP_PRODUCTS, rows.size());
    for (int i = 0; i < ranked; i++) {
      ProductSales row = rows.get(i);
      int percent = (int) Math.round(row.getAmount() * 100.0 / total);
      shares.add(new ProductShare(row.getProductId(), row.getProductName(), row.getAmount(), percent));
      rankedPercent += percent;
      rankedAmount += row.getAmount();
    }

    if (rows.size() > TOP_PRODUCTS) {
      shares.add(new ProductShare(null, OTHERS_LABEL, total - rankedAmount, 100 - rankedPercent));
    }
    return shares;
  }

  private static long amountOf(Map<YearMonth, MonthlyFulfilled> byMonth, YearMonth month) {
    MonthlyFulfilled row = byMonth.get(month);
    return row == null ? 0L : row.getAmount();
  }

  private static long countOf(Map<YearMonth, MonthlyFulfilled> byMonth, YearMonth month) {
    MonthlyFulfilled row = byMonth.get(month);
    return row == null ? 0L : row.getOrderCount();
  }

  private static MonthOverMonth compare(long current, long previous) {
    long absoluteChange = current - previous;
    Integer percentChange = previous == 0L ? null : Math.toIntExact(Math.round(absoluteChange * 100.0 / previous));
    Trend trend = current > previous ? Trend.UP : current < previous ? Trend.DOWN : Trend.FLAT;
    return new MonthOverMonth(current, previous, absoluteChange, percentChange, trend);
  }

}
