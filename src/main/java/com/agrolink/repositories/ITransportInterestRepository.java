package com.agrolink.repositories;

import com.agrolink.model.TransportInterestModel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ITransportInterestRepository extends JpaRepository<TransportInterestModel, Integer> {

  @EntityGraph(attributePaths = "carrier")
  List<TransportInterestModel> findByOrderIdOrderByIdAsc(Integer orderId);

  boolean existsByOrderIdAndCarrierId(Integer orderId, Integer carrierId);

  void deleteByOrderIdAndCarrierId(Integer orderId, Integer carrierId);

  /** Once a carrier is assigned the remaining expressions of interest are moot. */
  void deleteByOrderId(Integer orderId);

}
