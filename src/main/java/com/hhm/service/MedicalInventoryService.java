package com.service;

import com.model.MedicalInventory;
import com.repository.MedicalInventoryRepository;
import com.repository.MedicalItemRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicalInventoryService {

    private final MedicalInventoryRepository inventoryRepository;
    private final MedicalItemRepository medicalItemRepository;

    public MedicalInventoryService(MedicalInventoryRepository inventoryRepository,
                                   MedicalItemRepository medicalItemRepository) {
        this.inventoryRepository = inventoryRepository;
        this.medicalItemRepository = medicalItemRepository;
    }

    @Transactional
    @CacheEvict(value = "inventory", allEntries = true)
    public void add(MedicalInventory item) throws ServiceException {
        boolean skuTaken = inventoryRepository.findAll().stream()
            .anyMatch(m -> m.getSku().equalsIgnoreCase(item.getSku()));
        if (skuTaken)
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "SKU \"" + item.getSku() + "\" already exists in inventory.");
        inventoryRepository.save(item);
    }

    @Transactional
    @CacheEvict(value = "inventory", allEntries = true)
    public void update(MedicalInventory item) throws ServiceException {
        if (!inventoryRepository.existsById(item.getId()))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Inventory item with id " + item.getId() + " not found.");
        inventoryRepository.save(item);
    }

    @Transactional
    @CacheEvict(value = "inventory", allEntries = true)
    public void remove(long itemId) throws ServiceException {
        if (!inventoryRepository.existsById(itemId))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Inventory item with id " + itemId + " not found.");

        // Delete child MedicalItems first to satisfy the FK constraint
        medicalItemRepository.deleteByInventoryId(itemId);
        inventoryRepository.deleteById(itemId);
    }

    @Transactional(readOnly = true)
    @Cacheable("inventory")
    public List<MedicalInventory> getAll() {
        return inventoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<MedicalInventory> getAll(Pageable pageable) {
        return inventoryRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "inventory", key = "#keyword")
    public List<MedicalInventory> search(String keyword) {
        String lower = keyword.toLowerCase();
        return inventoryRepository.findAll().stream()
            .filter(m -> m.getName().toLowerCase().contains(lower)
                      || m.getSku().toLowerCase().contains(lower))
            .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable("inventory-lowstock")
    public List<MedicalInventory> getLowStock() {
        return inventoryRepository.findAll().stream()
            .filter(m -> m.getQuantityOnHand() <= m.getReorderLevel())
            .toList();
    }
}
