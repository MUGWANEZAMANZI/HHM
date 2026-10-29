package com.hhm.repository;

import com.hhm.model.MedicalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MedicalItemRepository extends JpaRepository<MedicalItem, Long> {

    // Derived queries
    List<MedicalItem> findByInventoryId(long inventoryId);
    List<MedicalItem> findByCategory(String category);

    // JPQL — bulk delete child items before removing parent inventory row
    @Modifying
    @Transactional
    @Query("DELETE FROM MedicalItem m WHERE m.inventoryId = :inventoryId")
    void deleteByInventoryId(@Param("inventoryId") long inventoryId);
}
