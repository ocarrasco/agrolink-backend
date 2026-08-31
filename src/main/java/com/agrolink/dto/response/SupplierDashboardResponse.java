package com.agrolink.dto.response;

import java.util.List;

public record SupplierDashboardResponse( //@formatter:off
    MonthOverMonth sales,
    MonthOverMonth completedOrders,
    List<MonthlyAmount> salesTrend,
    List<ProductShare> topProducts
) { //@formatter:on

    public record MonthlyAmount(int year, int month, long amount) {

    }

    public record ProductShare( //@formatter:off
      Integer masterProductId,
      String productName,
      long amount,
      int percent
    ) { //@formatter:on

    }

}
