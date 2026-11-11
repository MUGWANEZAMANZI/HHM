package com.hhm.repository;

import com.hhm.model.MedicalItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface MedicalItemRepository extends JpaRepository<MedicalItem, Long> {

    // JPQL — bulk delete child items before removing parent inventory row
    @Modifying
    @Transactional
    @Query("DELETE FROM MedicalItem m WHERE m.inventory.id = :inventoryId")
    void deleteByInventory_Id(@Param("inventoryId") long inventoryId);
}
