package com.agrolink.dto.response;

public record RetailerDashboardResponse( //@formatter:off
    MonthOverMonth completedOrders,
    MonthOverMonth placedOrders,
    MonthOverMonth investment
) { //@formatter:on

}
