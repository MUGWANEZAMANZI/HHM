package com.hhm.service;

import com.hhm.model.MedicalInventory;
import com.hhm.repository.MedicalInventoryRepository;
import com.hhm.repository.MedicalItemRepository;
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
    @CacheEvict(value = {"inventory", "inventory-lowstock"}, allEntries = true)
    public void add(MedicalInventory item) throws ServiceException {
        if (inventoryRepository.findBySku(item.getSku()).isPresent())
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "SKU \"" + item.getSku() + "\" already exists in inventory.");
        inventoryRepository.save(item);
    }

    @Transactional
    @CacheEvict(value = {"inventory", "inventory-lowstock"}, allEntries = true)
    public void update(MedicalInventory item) throws ServiceException {
        if (!inventoryRepository.existsById(item.getId()))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Inventory item with id " + item.getId() + " not found.");
        inventoryRepository.save(item);
    }

    @Transactional
    @CacheEvict(value = {"inventory", "inventory-lowstock"}, allEntries = true)
    public void remove(long itemId) throws ServiceException {
        if (!inventoryRepository.existsById(itemId))
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Inventory item with id " + itemId + " not found.");

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
        return inventoryRepository.search(keyword);
    }

    @Transactional(readOnly = true)
    @Cacheable("inventory-lowstock")
    public List<MedicalInventory> getLowStock() {
        return inventoryRepository.findLowStock();
    }
}
