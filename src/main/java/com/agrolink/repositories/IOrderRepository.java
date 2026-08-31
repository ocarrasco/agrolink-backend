package com.agrolink.repositories;

import com.agrolink.model.OrderModel;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.repositories.projections.MonthlyCount;
import com.agrolink.repositories.projections.MonthlyFulfilled;
import com.agrolink.repositories.projections.ProductSales;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IOrderRepository extends JpaRepository<OrderModel, Integer> {

  @EntityGraph(attributePaths = { "retailer", "supplier", "items" })
  Optional<OrderModel> findWithItemsById(Integer id);

  @EntityGraph(attributePaths = { "retailer", "supplier" })
  List<OrderModel> findByRetailerIdOrderByIdDesc(Integer retailerId);

  @EntityGraph(attributePaths = { "retailer", "supplier" })
  List<OrderModel> findByRetailerIdAndStatusOrderByIdDesc(Integer retailerId, OrderStatus status);

  @EntityGraph(attributePaths = { "retailer", "supplier" })
  List<OrderModel> findBySupplierIdOrderByIdDesc(Integer supplierId);

  @EntityGraph(attributePaths = { "retailer", "supplier" })
  List<OrderModel> findBySupplierIdAndStatusOrderByIdDesc(Integer supplierId, OrderStatus status);

  @Query("""
      select year(o.createdAt) as yr, month(o.createdAt) as mo,
             coalesce(sum(o.total), 0) as amount, count(o) as orderCount
      from OrderModel o
      where o.supplier.id = :supplierId
        and o.status = com.agrolink.model.enums.OrderStatus.FULFILLED
        and o.createdAt >= :since
      group by year(o.createdAt), month(o.createdAt)
      """)
  List<MonthlyFulfilled> monthlyFulfilledSince(Integer supplierId, LocalDateTime since);

  /** A retailer's FULFILLED orders grouped by month: total spent (investment) and how many completed. */
  @Query("""
      select year(o.createdAt) as yr, month(o.createdAt) as mo,
             coalesce(sum(o.total), 0) as amount, count(o) as orderCount
      from OrderModel o
      where o.retailer.id = :retailerId
        and o.status = com.agrolink.model.enums.OrderStatus.FULFILLED
        and o.createdAt >= :since
      group by year(o.createdAt), month(o.createdAt)
      """)
  List<MonthlyFulfilled> monthlyFulfilledSinceForRetailer(Integer retailerId, LocalDateTime since);

  /** A retailer's orders grouped by month, regardless of status. */
  @Query("""
      select year(o.createdAt) as yr, month(o.createdAt) as mo, count(o) as orderCount
      from OrderModel o
      where o.retailer.id = :retailerId
        and o.createdAt >= :since
      group by year(o.createdAt), month(o.createdAt)
      """)
  List<MonthlyCount> monthlyPlacedSinceForRetailer(Integer retailerId, LocalDateTime since);

  /**
   * A supplier's all-time FULFILLED sales grouped by master product, biggest first. Used by
   * the supplier dashboard's "most-sold products" ranking.
   */
  @Query("""
      select i.masterProduct.id as productId, i.productName as productName,
             coalesce(sum(i.lineTotal), 0) as amount
      from OrderItemModel i
      where i.order.supplier.id = :supplierId
        and i.order.status = com.agrolink.model.enums.OrderStatus.FULFILLED
      group by i.masterProduct.id, i.productName
      order by sum(i.lineTotal) desc
      """)
  List<ProductSales> productSales(Integer supplierId);

}
