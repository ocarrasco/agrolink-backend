package com.agrolink.services;

import com.agrolink.dto.response.MonthOverMonth;
import com.agrolink.dto.response.RetailerDashboardResponse;
import com.agrolink.repositories.IOrderRepository;
import com.agrolink.repositories.projections.MonthlyCount;
import com.agrolink.repositories.projections.MonthlyFulfilled;
import com.agrolink.security.LoggedUser;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RetailerDashboardService {

  @NonNull
  private final IOrderRepository orderRepository;

  @Transactional(readOnly = true)
  public RetailerDashboardResponse getDashboard(LoggedUser retailer) {
    return build(retailer.id(), YearMonth.now(ZoneOffset.UTC));
  }

  protected RetailerDashboardResponse build(Integer retailerId, YearMonth current) {
    YearMonth previous = current.minusMonths(1);
    LocalDateTime since = previous.atDay(1).atStartOfDay();

    var fulfilledByMonth = orderRepository.monthlyFulfilledSinceForRetailer(retailerId, since)
        .stream()
        .collect(Collectors.toMap(row -> YearMonth.of(row.getYr(), row.getMo()), Function.identity()));

    var placedByMonth = orderRepository.monthlyPlacedSinceForRetailer(retailerId, since)
        .stream()
        .collect(Collectors.toMap(row -> YearMonth.of(row.getYr(), row.getMo()), Function.identity()));

    MonthOverMonth completedOrders = new MonthOverMonth(fulfilledCountOf(fulfilledByMonth, current), fulfilledCountOf(fulfilledByMonth, previous));
    MonthOverMonth investment = new MonthOverMonth(amountOf(fulfilledByMonth, current), amountOf(fulfilledByMonth, previous));
    MonthOverMonth placedOrders = new MonthOverMonth(placedCountOf(placedByMonth, current), placedCountOf(placedByMonth, previous));

    return new RetailerDashboardResponse(completedOrders, placedOrders, investment);
  }

  private long amountOf(Map<YearMonth, MonthlyFulfilled> byMonth, YearMonth month) {
    MonthlyFulfilled row = byMonth.get(month);
    return row == null ? 0L : row.getAmount();
  }

  private long fulfilledCountOf(Map<YearMonth, MonthlyFulfilled> byMonth, YearMonth month) {
    MonthlyFulfilled row = byMonth.get(month);
    return row == null ? 0L : row.getOrderCount();
  }

  private long placedCountOf(Map<YearMonth, MonthlyCount> byMonth, YearMonth month) {
    MonthlyCount row = byMonth.get(month);
    return row == null ? 0L : row.getOrderCount();
  }

}
