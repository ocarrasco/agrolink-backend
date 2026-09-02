package com.agrolink.repositories;

import com.agrolink.model.OrderModel;
import com.agrolink.model.enums.OrderStatus;
import com.agrolink.model.enums.ShippingMethod;
import com.agrolink.model.enums.TransportStatus;
import com.agrolink.repositories.projections.MonthlyCount;
import com.agrolink.repositories.projections.MonthlyFulfilled;
import com.agrolink.repositories.projections.ProductSales;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IOrderRepository extends JpaRepository<OrderModel, Integer> {

  @EntityGraph(attributePaths = { "retailer", "supplier", "items" })
  Optional<OrderModel> findWithItemsById(Integer id);

  boolean existsByRetailerId(Integer retailerId);

  /** Orders awaiting a platform carrier: confirmed, shipped by the platform, no carrier assigned yet. */
  @EntityGraph(attributePaths = { "retailer", "supplier" })
  List<OrderModel> findByShippingMethodAndStatusAndCarrierIsNull(ShippingMethod shippingMethod, OrderStatus status);

  /**
   * A carrier's assigned transport jobs (orders where {@code carrier_id = me}), optionally filtered
   * by transport status and/or year+month of {@code createdAt}. Mirror of {@link #findForSupplier};
   * backs the carrier's "my trips" active view and its history page. Newest first.
   */
  @EntityGraph(attributePaths = { "retailer", "supplier", "items" })
  @Query("""
      select o from OrderModel o
      where o.carrier.id = :carrierId
        and (:status is null or o.transportStatus = :status)
        and (:year is null or year(o.createdAt) = :year)
        and (:month is null or month(o.createdAt) = :month)
      order by o.createdAt desc, o.id desc
      """)
  List<OrderModel> findForCarrier(Integer carrierId, TransportStatus status, Integer year, Integer month);

  /**
   * Overwrites the Hibernate-assigned {@code @CreationTimestamp}. Only for bootstrap seeding
   * (see {@code RetailerSeedService}), where orders need a specific historical date.
   */
  @Modifying(clearAutomatically = true)
  @Query("update OrderModel o set o.createdAt = :createdAt where o.id = :id")
  void backdateCreatedAt(Integer id, LocalDateTime createdAt);

  /**
   * A retailer's orders, optionally filtered by status and/or year+month of {@code createdAt}.
   * Backs both {@code /retailer/orders} use cases: the dashboard (status=PLACED, current month)
   * and the history page (any combination, defaults to the current month with no status filter).
   */
  @EntityGraph(attributePaths = { "retailer", "supplier" })
  @Query("""
      select o from OrderModel o
      where o.retailer.id = :retailerId
        and (:status is null or o.status = :status)
        and (:year is null or year(o.createdAt) = :year)
        and (:month is null or month(o.createdAt) = :month)
      order by o.id desc
      """)
  List<OrderModel> findForRetailer(Integer retailerId, OrderStatus status, Integer year, Integer month);

  /**
   * A supplier's received orders, optionally filtered by status and/or year+month of
   * {@code createdAt}. Mirror of {@link #findForRetailer}: backs both {@code /supplier/orders}
   * use cases — the "Órdenes recibidas" active view (no filters, actionable ones filtered
   * client-side) and the history page (any combination of status + month). Newest first, id as
   * a stable tie-break.
   */
  @EntityGraph(attributePaths = { "retailer", "supplier" })
  @Query("""
      select o from OrderModel o
      where o.supplier.id = :supplierId
        and (:status is null or o.status = :status)
        and (:year is null or year(o.createdAt) = :year)
        and (:month is null or month(o.createdAt) = :month)
      order by o.createdAt desc, o.id desc
      """)
  List<OrderModel> findForSupplier(Integer supplierId, OrderStatus status, Integer year, Integer month);

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
