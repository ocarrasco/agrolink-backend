package com.agrolink.services;

import com.agrolink.dto.request.CreateOrderItemRequest;
import com.agrolink.dto.request.CreateOrderRequest;
import com.agrolink.dto.request.DeliveryPreference;
import com.agrolink.dto.response.CarrierDeliveryResponse;
import com.agrolink.dto.response.OrderItemResponse;
import com.agrolink.dto.response.OrderResponse;
import com.agrolink.dto.response.TransportInterestResponse;
import com.agrolink.dto.response.TransportRequestResponse;
import com.agrolink.events.OrderTerminatedEvent;
import com.agrolink.mappers.OrderMapper;
import com.agrolink.model.*;
import com.agrolink.model.enums.*;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.repositories.IOrderRepository;
import com.agrolink.repositories.ITransportInterestRepository;
import com.agrolink.repositories.IUserProfileRepository;
import com.agrolink.security.LoggedUser;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock
  private IOrderRepository orderRepository;

  @Mock
  private ICatalogItemRepository catalogItemRepository;

  @Spy
  private OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

  @Mock
  private UserService userService;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private IUserProfileRepository userProfileRepository;

  @Mock
  private ITransportInterestRepository transportInterestRepository;

  @InjectMocks
  private OrderService orderService;

  // ────────────────────────── create ──────────────────────────

  @Test
  void create_reservesStockAndComputesTotal() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    UserModel retailerModel = user(1, "Verduleria Central");
    UserModel supplierModel = user(2, "Verduras SPA");
    CatalogItemModel tomate = catalogItem(10, "Tomate", ProductUnit.KILOGRAMO, 1500, 100);
    CatalogItemModel papa = catalogItem(20, "Papa", ProductUnit.SACO, 8000, 30);

    when(userService.getReference(1)).thenReturn(retailerModel);
    when(userService.getReference(2)).thenReturn(supplierModel);
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(2, Set.of(10, 20)))
        .thenReturn(List.of(tomate, papa));
    when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    CreateOrderRequest request = new CreateOrderRequest(2,
        List.of(new CreateOrderItemRequest(10, 5), new CreateOrderItemRequest(20, 2)),
        ShippingMethod.PICKUP, null);

    OrderResponse response = orderService.create(retailer, request);

    assertThat(response.status()).isEqualTo(OrderStatus.PLACED);
    assertThat(response.total()).isEqualTo(1500 * 5 + 8000 * 2);
    assertThat(response.retailerId()).isEqualTo(1);
    assertThat(response.supplierId()).isEqualTo(2);
    assertThat(response.items()).extracting(OrderItemResponse::productName).containsExactly("Tomate", "Papa");
    assertThat(tomate.getAvailableQuantity()).isEqualTo(95);
    assertThat(papa.getAvailableQuantity()).isEqualTo(28);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void create_setsDeliveryPreference_whenProvided() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    CatalogItemModel tomate = catalogItem(10, "Tomate", ProductUnit.KILOGRAMO, 1500, 100);

    when(userService.getReference(1)).thenReturn(user(1, "Verduleria Central"));
    when(userService.getReference(2)).thenReturn(user(2, "Verduras SPA"));
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(2, Set.of(10))).thenReturn(List.of(tomate));
    when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    DeliveryPreference preference = new DeliveryPreference(WeekDay.MONDAY, TimeSlot.AM);
    CreateOrderRequest request = new CreateOrderRequest(2,
        List.of(new CreateOrderItemRequest(10, 1)), ShippingMethod.SUPPLIER_DELIVERY, preference);

    OrderResponse response = orderService.create(retailer, request);

    assertThat(response.shippingMethod()).isEqualTo(ShippingMethod.SUPPLIER_DELIVERY);
    assertThat(response.deliveryPreference()).isEqualTo(preference);
  }

  @Test
  void create_leavesDeliveryPreferenceNull_whenNotProvided() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    CatalogItemModel tomate = catalogItem(10, "Tomate", ProductUnit.KILOGRAMO, 1500, 100);

    when(userService.getReference(1)).thenReturn(user(1, "Verduleria Central"));
    when(userService.getReference(2)).thenReturn(user(2, "Verduras SPA"));
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(2, Set.of(10))).thenReturn(List.of(tomate));
    when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    CreateOrderRequest request = new CreateOrderRequest(2,
        List.of(new CreateOrderItemRequest(10, 1)), ShippingMethod.PICKUP, null);

    OrderResponse response = orderService.create(retailer, request);

    assertThat(response.deliveryPreference()).isNull();
  }

  @Test
  void create_rejectsOrderingFromYourself() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    CreateOrderRequest request = new CreateOrderRequest(1,
        List.of(new CreateOrderItemRequest(10, 1)), ShippingMethod.PICKUP, null);

    assertThatThrownBy(() -> orderService.create(retailer, request))
        .isInstanceOf(IllegalStateException.class);

    verifyNoInteractions(orderRepository, catalogItemRepository, userService, eventPublisher);
  }

  @Test
  void create_rejectsWhenNotEnoughStock() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    CatalogItemModel tomate = catalogItem(10, "Tomate", ProductUnit.KILOGRAMO, 1500, 3);

    when(userService.getReference(1)).thenReturn(user(1, "Verduleria Central"));
    when(userService.getReference(2)).thenReturn(user(2, "Verduras SPA"));
    when(catalogItemRepository.findBySupplierIdAndMasterProductIdIn(2, Set.of(10))).thenReturn(List.of(tomate));

    CreateOrderRequest request = new CreateOrderRequest(2,
        List.of(new CreateOrderItemRequest(10, 5)), ShippingMethod.PICKUP, null);

    assertThatThrownBy(() -> orderService.create(retailer, request))
        .isInstanceOf(IllegalStateException.class);

    assertThat(tomate.getAvailableQuantity()).isEqualTo(3);
    verify(orderRepository, never()).save(any());
  }

  // ────────────────────────── retailer listing / detail ──────────────────────────

  @Test
  void listForRetailer_listsAllOrders_whenNoFiltersProvided() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    when(orderRepository.findForRetailer(1, null, null, null)).thenReturn(List.of(order));

    List<OrderResponse> responses = orderService.listForRetailer(retailer, null, null, null);

    assertThat(responses).extracting(OrderResponse::id).containsExactly(5);
  }

  @Test
  void listForRetailer_forwardsStatusYearAndMonth() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.FULFILLED);
    when(orderRepository.findForRetailer(1, OrderStatus.FULFILLED, 2026, 9)).thenReturn(List.of(order));

    List<OrderResponse> responses = orderService.listForRetailer(retailer, OrderStatus.FULFILLED, 2026, 9);

    assertThat(responses).extracting(OrderResponse::status).containsExactly(OrderStatus.FULFILLED);
    verify(orderRepository).findForRetailer(1, OrderStatus.FULFILLED, 2026, 9);
  }

  @Test
  void getForRetailer_returnsOrder_whenItBelongsToTheRetailer() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    OrderResponse response = orderService.getForRetailer(retailer, 5);

    assertThat(response.id()).isEqualTo(5);
  }

  @Test
  void getForRetailer_throws_whenOrderBelongsToAnotherRetailer() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = order(5, user(99, "Otro retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.getForRetailer(retailer, 5))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void getForRetailer_throws_whenOrderDoesNotExist() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    when(orderRepository.findWithItemsById(99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> orderService.getForRetailer(retailer, 99))
        .isInstanceOf(EntityNotFoundException.class);
  }

  // ────────────────────────── cancel ──────────────────────────

  @Test
  void cancel_movesPlacedOrderToCancelled_andPublishesEvent() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    OrderResponse response = orderService.cancel(retailer, 5);

    assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
    verify(eventPublisher).publishEvent(new OrderTerminatedEvent(5, OrderStatus.CANCELLED));
  }

  @Test
  void cancel_rejects_whenOrderIsNotPlaced() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.CONFIRMED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.cancel(retailer, 5))
        .isInstanceOf(IllegalStateException.class);

    verifyNoInteractions(eventPublisher);
    verify(orderRepository, never()).saveAndFlush(any());
  }

  // ────────────────────────── supplier listing / detail ──────────────────────────

  @Test
  void listForSupplier_listsAllOrders_whenNoFiltersProvided() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    when(orderRepository.findForSupplier(2, null, null, null)).thenReturn(List.of(order));

    List<OrderResponse> responses = orderService.listForSupplier(supplier, null, null, null);

    assertThat(responses).extracting(OrderResponse::id).containsExactly(5);
  }

  @Test
  void listForSupplier_forwardsStatusYearAndMonth() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.FULFILLED);
    when(orderRepository.findForSupplier(2, OrderStatus.FULFILLED, 2026, 8)).thenReturn(List.of(order));

    List<OrderResponse> responses = orderService.listForSupplier(supplier, OrderStatus.FULFILLED, 2026, 8);

    assertThat(responses).extracting(OrderResponse::status).containsExactly(OrderStatus.FULFILLED);
    verify(orderRepository).findForSupplier(2, OrderStatus.FULFILLED, 2026, 8);
  }

  @Test
  void getForSupplier_throws_whenOrderBelongsToAnotherSupplier() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(99, "Otro proveedor"), OrderStatus.PLACED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.getForSupplier(supplier, 5))
        .isInstanceOf(EntityNotFoundException.class);
  }

  // ────────────────────────── confirm / reject / fulfill ──────────────────────────

  @Test
  void confirm_movesPlacedOrderToConfirmed() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    OrderResponse response = orderService.confirm(supplier, 5);

    assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void confirm_rejects_whenOrderIsNotPlaced() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.FULFILLED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.confirm(supplier, 5))
        .isInstanceOf(IllegalStateException.class);

    verify(orderRepository, never()).saveAndFlush(any());
  }

  @Test
  void reject_movesPlacedOrderToRejected_trimsNote_andPublishesEvent() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    OrderResponse response = orderService.reject(supplier, 5, "  Sin stock  ");

    assertThat(response.status()).isEqualTo(OrderStatus.REJECTED);
    assertThat(response.supplierNote()).isEqualTo("Sin stock");
    verify(eventPublisher).publishEvent(new OrderTerminatedEvent(5, OrderStatus.REJECTED));
  }

  @Test
  void reject_setsNoteToNull_whenNoteIsBlank() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    OrderResponse response = orderService.reject(supplier, 5, "   ");

    assertThat(response.supplierNote()).isNull();
  }

  @Test
  void reject_rejects_whenOrderIsNotPlaced() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.CONFIRMED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.reject(supplier, 5, "nota"))
        .isInstanceOf(IllegalStateException.class);

    verifyNoInteractions(eventPublisher);
  }

  @Test
  void fulfill_movesConfirmedOrderToFulfilled() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.CONFIRMED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    OrderResponse response = orderService.fulfill(supplier, 5);

    assertThat(response.status()).isEqualTo(OrderStatus.FULFILLED);
    verifyNoInteractions(eventPublisher);
  }

  @Test
  void fulfill_rejects_whenOrderIsNotConfirmed() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.fulfill(supplier, 5))
        .isInstanceOf(IllegalStateException.class);

    verify(orderRepository, never()).saveAndFlush(any());
  }

  // ────────────────────────── transport (carrier open market) ──────────────────────────

  @Test
  void listOpenTransportRequests_returnsOrdersAwaitingCarrier_withInterestFlag() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = awaitingTransportOrder(5, user(1, "Retailer"), user(2, "Supplier"));
    when(orderRepository.findByShippingMethodAndStatusAndCarrierIsNull(ShippingMethod.PLATFORM_CARRIER, OrderStatus.CONFIRMED))
        .thenReturn(List.of(order));
    when(userProfileRepository.findByUserIdIn(any())).thenReturn(List.of());
    when(transportInterestRepository.existsByOrderIdAndCarrierId(5, 3)).thenReturn(true);

    List<TransportRequestResponse> responses = orderService.listOpenTransportRequests(carrier);

    assertThat(responses).extracting(TransportRequestResponse::orderId).containsExactly(5);
    assertThat(responses).extracting(TransportRequestResponse::alreadyInterested).containsExactly(true);
  }

  @Test
  void confirm_opensTransportRequest_forPlatformCarrierOrder() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED);
    order.setShippingMethod(ShippingMethod.PLATFORM_CARRIER);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    OrderResponse response = orderService.confirm(supplier, 5);

    assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(response.transportStatus()).isEqualTo(TransportStatus.AWAITING_CARRIER);
  }

  @Test
  void fulfill_rejects_forPlatformCarrierOrder() {
    LoggedUser supplier = loggedUser(2, UserRole.SUPPLIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.CONFIRMED);
    order.setShippingMethod(ShippingMethod.PLATFORM_CARRIER);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.fulfill(supplier, 5))
        .isInstanceOf(IllegalStateException.class);

    verify(orderRepository, never()).saveAndFlush(any());
  }

  @Test
  void withdrawInterest_deletesInterest_whenOrderStillAwaiting() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = awaitingTransportOrder(5, user(1, "Retailer"), user(2, "Supplier"));
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    orderService.withdrawInterest(carrier, 5);

    verify(transportInterestRepository).deleteByOrderIdAndCarrierId(5, 3);
  }

  @Test
  void withdrawInterest_noop_whenOrderNoLongerAwaiting() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = assignedTransportOrder(5, user(3, "Transportes Andes"), TransportStatus.ASSIGNED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    orderService.withdrawInterest(carrier, 5);

    verify(transportInterestRepository, never()).deleteByOrderIdAndCarrierId(any(), any());
  }

  @Test
  void markInterested_savesInterest_whenOrderIsAwaitingTransport() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = awaitingTransportOrder(5, user(1, "Retailer"), user(2, "Supplier"));
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(transportInterestRepository.existsByOrderIdAndCarrierId(5, 3)).thenReturn(false);
    when(userService.getReference(3)).thenReturn(user(3, "Transportes Andes"));

    orderService.markInterested(carrier, 5);

    verify(transportInterestRepository).save(any(TransportInterestModel.class));
  }

  @Test
  void markInterested_isIdempotent_whenAlreadyInterested() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = awaitingTransportOrder(5, user(1, "Retailer"), user(2, "Supplier"));
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(transportInterestRepository.existsByOrderIdAndCarrierId(5, 3)).thenReturn(true);

    orderService.markInterested(carrier, 5);

    verify(transportInterestRepository, never()).save(any());
  }

  @Test
  void markInterested_rejects_whenOrderIsNotAwaitingTransport() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = order(5, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.PLACED); // PICKUP by default
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.markInterested(carrier, 5))
        .isInstanceOf(IllegalStateException.class);

    verifyNoInteractions(transportInterestRepository);
  }

  @Test
  void listTransportInterests_returnsCarriersWhoExpressedInterest() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = awaitingTransportOrder(5, user(1, "Retailer"), user(2, "Supplier"));
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    TransportInterestModel interest = new TransportInterestModel();
    interest.setOrder(order);
    interest.setCarrier(user(3, "Transportes Andes"));
    when(transportInterestRepository.findByOrderIdOrderByIdAsc(5)).thenReturn(List.of(interest));
    when(userProfileRepository.findByUserIdIn(any())).thenReturn(List.of());

    List<TransportInterestResponse> responses = orderService.listTransportInterests(retailer, 5);

    assertThat(responses).extracting(TransportInterestResponse::carrierName).containsExactly("Transportes Andes");
  }

  @Test
  void listTransportInterests_throws_whenOrderNotOwnedByRetailer() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = awaitingTransportOrder(5, user(99, "Otro retailer"), user(2, "Supplier"));
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.listTransportInterests(retailer, 5))
        .isInstanceOf(EntityNotFoundException.class);
  }

  @Test
  void acceptCarrier_assignsCarrier_setsAssigned_andClearsInterests() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = awaitingTransportOrder(5, user(1, "Retailer"), user(2, "Supplier"));
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(transportInterestRepository.existsByOrderIdAndCarrierId(5, 3)).thenReturn(true);
    when(userService.getReference(3)).thenReturn(user(3, "Transportes Andes"));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    OrderResponse response = orderService.acceptCarrier(retailer, 5, 3);

    assertThat(response.carrierId()).isEqualTo(3);
    assertThat(response.carrierName()).isEqualTo("Transportes Andes");
    assertThat(response.transportStatus()).isEqualTo(TransportStatus.ASSIGNED);
    verify(transportInterestRepository).deleteByOrderId(5);
  }

  @Test
  void acceptCarrier_rejects_whenCarrierNeverExpressedInterest() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = awaitingTransportOrder(5, user(1, "Retailer"), user(2, "Supplier"));
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(transportInterestRepository.existsByOrderIdAndCarrierId(5, 3)).thenReturn(false);

    assertThatThrownBy(() -> orderService.acceptCarrier(retailer, 5, 3))
        .isInstanceOf(EntityNotFoundException.class);

    verify(orderRepository, never()).saveAndFlush(any());
  }

  @Test
  void acceptCarrier_rejects_whenOrderNotAwaitingCarrier() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = assignedTransportOrder(5, user(4, "Otro transportista"), TransportStatus.ASSIGNED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.acceptCarrier(retailer, 5, 3))
        .isInstanceOf(IllegalStateException.class);

    verifyNoInteractions(transportInterestRepository);
  }

  // ────────────────────────── transport (carrier execution) ──────────────────────────

  @Test
  void listForCarrier_forwardsFilters() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = assignedTransportOrder(5, user(3, "Transportes Andes"), TransportStatus.IN_TRANSIT);
    when(orderRepository.findForCarrier(3, TransportStatus.IN_TRANSIT, 2026, 9)).thenReturn(List.of(order));
    when(userProfileRepository.findByUserIdIn(any())).thenReturn(List.of());

    List<CarrierDeliveryResponse> responses = orderService.listForCarrier(carrier, TransportStatus.IN_TRANSIT, 2026, 9);

    assertThat(responses).extracting(CarrierDeliveryResponse::orderId).containsExactly(5);
    verify(orderRepository).findForCarrier(3, TransportStatus.IN_TRANSIT, 2026, 9);
  }

  @Test
  void pickup_movesAssignedToInTransit() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = assignedTransportOrder(5, user(3, "Transportes Andes"), TransportStatus.ASSIGNED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);
    when(userProfileRepository.findByUserIdIn(any())).thenReturn(List.of());

    CarrierDeliveryResponse response = orderService.pickup(carrier, 5);

    assertThat(response.transportStatus()).isEqualTo(TransportStatus.IN_TRANSIT);
  }

  @Test
  void pickup_rejects_whenNotAssigned() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = assignedTransportOrder(5, user(3, "Transportes Andes"), TransportStatus.IN_TRANSIT);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.pickup(carrier, 5))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void carrierDelivery_throws_whenOrderAssignedToAnotherCarrier() {
    LoggedUser carrier = loggedUser(3, UserRole.CARRIER);
    OrderModel order = assignedTransportOrder(5, user(99, "Otro"), TransportStatus.ASSIGNED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.pickup(carrier, 5))
        .isInstanceOf(EntityNotFoundException.class);
  }

  // ────────────────────────── transport (retailer confirms delivery) ──────────────────────────

  @Test
  void listInTransitForRetailer_returnsRetailersOpenPlatformCarrierOrders() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = assignedTransportOrder(5, user(3, "Transportes Andes"), TransportStatus.IN_TRANSIT);
    when(orderRepository.findByRetailerIdAndShippingMethodAndStatusOrderByCreatedAtDesc(1, ShippingMethod.PLATFORM_CARRIER, OrderStatus.CONFIRMED))
        .thenReturn(List.of(order));

    List<OrderResponse> responses = orderService.listInTransitForRetailer(retailer);

    assertThat(responses).extracting(OrderResponse::id).containsExactly(5);
  }

  @Test
  void confirmDelivery_movesInTransitToDelivered_andFulfillsOrder() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = assignedTransportOrder(5, user(3, "Transportes Andes"), TransportStatus.IN_TRANSIT);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));
    when(orderRepository.saveAndFlush(order)).thenReturn(order);

    OrderResponse response = orderService.confirmDelivery(retailer, 5);

    assertThat(response.transportStatus()).isEqualTo(TransportStatus.DELIVERED);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.FULFILLED);
  }

  @Test
  void confirmDelivery_rejects_whenNotInTransit() {
    LoggedUser retailer = loggedUser(1, UserRole.RETAILER);
    OrderModel order = assignedTransportOrder(5, user(3, "Transportes Andes"), TransportStatus.ASSIGNED);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.confirmDelivery(retailer, 5))
        .isInstanceOf(IllegalStateException.class);

    verify(orderRepository, never()).saveAndFlush(any());
  }

  @Test
  void confirmDelivery_throws_whenOrderBelongsToAnotherRetailer() {
    LoggedUser retailer = loggedUser(99, UserRole.RETAILER);
    OrderModel order = assignedTransportOrder(5, user(3, "Transportes Andes"), TransportStatus.IN_TRANSIT);
    when(orderRepository.findWithItemsById(5)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> orderService.confirmDelivery(retailer, 5))
        .isInstanceOf(EntityNotFoundException.class);
  }

  // ────────────────────────── helpers ──────────────────────────

  private static LoggedUser loggedUser(Integer id, UserRole role) {
    return new LoggedUser(id, UUID.randomUUID(), role);
  }

  private static UserModel user(Integer id, String name) {
    UserModel model = new UserModel();
    model.setId(id);
    model.setName(name);
    return model;
  }

  private static MasterProductModel masterProduct(Integer id, String name, ProductUnit unit) {
    MasterProductModel model = new MasterProductModel();
    model.setId(id);
    model.setName(name);
    model.setUnit(unit);
    return model;
  }

  private static CatalogItemModel catalogItem(Integer masterProductId, String productName, ProductUnit unit, int price, int stock) {
    CatalogItemModel item = new CatalogItemModel();
    item.setMasterProduct(masterProduct(masterProductId, productName, unit));
    item.setUnit(unit);
    item.setPricePerUnit(price);
    item.setAvailableQuantity(stock);
    return item;
  }

  private static OrderModel order(Integer id, UserModel retailer, UserModel supplier, OrderStatus status) {
    OrderModel order = new OrderModel();
    order.setId(id);
    order.setRetailer(retailer);
    order.setSupplier(supplier);
    order.setStatus(status);
    order.setTotal(0);
    order.setShippingMethod(ShippingMethod.PICKUP);

    OrderItemModel item = new OrderItemModel();
    item.setMasterProduct(masterProduct(10, "Tomate", ProductUnit.KILOGRAMO));
    item.setProductName("Tomate");
    item.setUnit(ProductUnit.KILOGRAMO);
    item.setQuantity(1);
    item.setUnitPrice(1000);
    item.setLineTotal(1000);
    order.addItem(item);

    return order;
  }

  /** CONFIRMED + PLATFORM_CARRIER + transportStatus AWAITING_CARRIER — a valid open transport request. */
  private static OrderModel awaitingTransportOrder(Integer id, UserModel retailer, UserModel supplier) {
    OrderModel order = order(id, retailer, supplier, OrderStatus.CONFIRMED);
    order.setShippingMethod(ShippingMethod.PLATFORM_CARRIER);
    order.setTransportStatus(TransportStatus.AWAITING_CARRIER);
    return order;
  }

  /** PLATFORM_CARRIER order with a carrier assigned and the given transport status. */
  private static OrderModel assignedTransportOrder(Integer id, UserModel carrier, TransportStatus transportStatus) {
    OrderModel order = order(id, user(1, "Retailer"), user(2, "Supplier"), OrderStatus.CONFIRMED);
    order.setShippingMethod(ShippingMethod.PLATFORM_CARRIER);
    order.setCarrier(carrier);
    order.setTransportStatus(transportStatus);
    return order;
  }

}
