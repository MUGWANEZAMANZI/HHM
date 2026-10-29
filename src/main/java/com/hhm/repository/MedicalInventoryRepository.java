package com.hhm.repository;

import com.hhm.model.MedicalInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicalInventoryRepository extends JpaRepository<MedicalInventory, Long> {

    Optional<MedicalInventory> findBySku(String sku);

    @Query("SELECT m FROM MedicalInventory m WHERE m.quantityOnHand <= m.reorderLevel")
    List<MedicalInventory> findLowStock();

    @Query("SELECT m FROM MedicalInventory m WHERE " +
           "LOWER(m.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR " +
           "LOWER(m.sku)  LIKE LOWER(CONCAT('%', :kw, '%'))")
    List<MedicalInventory> search(@Param("kw") String keyword);

    Page<MedicalInventory> findAll(Pageable pageable);
}
