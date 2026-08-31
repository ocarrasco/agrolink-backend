package com.agrolink.repositories;

import com.agrolink.model.MasterProductModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IMasterProductRepository extends JpaRepository<MasterProductModel, Integer> {

  List<MasterProductModel> findByActiveTrueOrderByNameAsc();

  List<MasterProductModel> findAllByOrderByNameAsc();

  Optional<MasterProductModel> findByNameIgnoreCase(String name);

}
