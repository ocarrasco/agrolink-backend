package com.agrolink.services;

import com.agrolink.dto.response.OrderSuggestionResponse;
import com.agrolink.model.CatalogItemModel;
import com.agrolink.repositories.ICatalogItemRepository;
import com.agrolink.security.LoggedUser;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Purchase suggestions for a retailer ("qué conviene pedir"). <b>Placeholder implementation</b>:
 * a handful of random active catalog items with small suggested quantities, only to show the
 * endpoint working. The real demand-driven model (from {@code order_item} history) is
 * {@code design_plan.md} iteración 6.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSuggestionService {

  private static final int MAX_SUGGESTIONS = 4;

  @NonNull
  private final ICatalogItemRepository catalogItemRepository;

  @Transactional(readOnly = true)
  public List<OrderSuggestionResponse> suggestForRetailer(LoggedUser retailer) {
    List<CatalogItemModel> items = new ArrayList<>(catalogItemRepository.findActiveItems(null, null));
    Collections.shuffle(items);

    ThreadLocalRandom random = ThreadLocalRandom.current();
    Map<Integer, OrderSuggestionResponse> byProduct = new LinkedHashMap<>();
    for (CatalogItemModel item : items) {
      if (byProduct.size() >= MAX_SUGGESTIONS) {
        break;
      }
      Integer productId = item.getMasterProduct().getId();
      byProduct.computeIfAbsent(productId, id -> {
        int minStock = 2 + random.nextInt(6);
        int suggested = minStock + random.nextInt(10);
        return new OrderSuggestionResponse(
            id,
            item.getMasterProduct().getName(),
            item.getUnit(),
            4 + random.nextInt(12),
            minStock,
            suggested,
            item.getPricePerUnit());
      });
    }

    log.info("Order suggestions for retailer {}: {} item(s) (placeholder heuristic)", retailer.id(), byProduct.size());
    return List.copyOf(byProduct.values());
  }
}
