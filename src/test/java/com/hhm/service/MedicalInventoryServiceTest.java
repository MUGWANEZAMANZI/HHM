package com.hhm.service;

import com.hhm.model.MedicalInventory;
import com.hhm.repository.MedicalInventoryRepository;
import com.hhm.repository.MedicalItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalInventoryServiceTest {

    @Mock MedicalInventoryRepository inventoryRepository;
    @Mock MedicalItemRepository medicalItemRepository;
    @InjectMocks MedicalInventoryService inventoryService;

    // ─── add ─────────────────────────────────────────────────────────────────

    @Test
    void add_success_savesItem() {
        MedicalInventory item = new MedicalInventory("Paracetamol", "MED-001", 100, 20);
        given(inventoryRepository.findBySku("MED-001")).willReturn(Optional.empty());

        inventoryService.add(item);

        then(inventoryRepository).should().save(item);
    }

    @Test
    void add_duplicateSku_throwsConflict() {
        MedicalInventory item = new MedicalInventory("Paracetamol", "MED-001", 100, 20);
        given(inventoryRepository.findBySku("MED-001"))
            .willReturn(Optional.of(new MedicalInventory("Other", "MED-001", 50, 10)));

        assertThatThrownBy(() -> inventoryService.add(item))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("MED-001")
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.CONFLICT);
    }

    // ─── remove ──────────────────────────────────────────────────────────────

    @Test
    void remove_success_deletesItemsBeforeInventory() {
        given(inventoryRepository.existsById(1L)).willReturn(true);

        inventoryService.remove(1L);

        then(medicalItemRepository).should().deleteByInventoryId(1L);
        then(inventoryRepository).should().deleteById(1L);
    }

    @Test
    void remove_notFound_throws() {
        given(inventoryRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> inventoryService.remove(99L))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }

    // ─── read ────────────────────────────────────────────────────────────────

    @Test
    void getLowStock_delegatesToRepository() {
        given(inventoryRepository.findLowStock()).willReturn(List.of());

        inventoryService.getLowStock();

        then(inventoryRepository).should().findLowStock();
    }

    @Test
    void search_delegatesToRepository() {
        given(inventoryRepository.search("para")).willReturn(List.of());

        inventoryService.search("para");

        then(inventoryRepository).should().search("para");
    }

    // ─── update ──────────────────────────────────────────────────────────────

    @Test
    void update_notFound_throws() {
        MedicalInventory item = new MedicalInventory("Paracetamol", "MED-001", 100, 20);
        given(inventoryRepository.existsById(0L)).willReturn(false);

        assertThatThrownBy(() -> inventoryService.update(item))
            .isInstanceOf(ServiceException.class)
            .extracting(e -> ((ServiceException) e).getType())
            .isEqualTo(ServiceException.Type.NOT_FOUND);
    }
}
