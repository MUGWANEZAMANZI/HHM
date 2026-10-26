package com.service;

import com.dao.MedicalInventoryDAO;
import com.model.MedicalInventory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalInventoryService {

    private final MedicalInventoryDAO inventoryDAO;

    //Constructor injection for MedicalInventoryDAO
    public MedicalInventoryService(MedicalInventoryDAO inventoryDAO)
    {
        this.inventoryDAO = inventoryDAO;
    }

    // Business rule: SKU must be unique across all inventory items
    public void add(MedicalInventory item) throws ServiceException {
        boolean skuTaken = inventoryDAO.findAll().stream()
            .anyMatch(m -> m.getSku().equalsIgnoreCase(item.getSku()));
        if (skuTaken)
            throw new ServiceException(ServiceException.Type.CONFLICT,
                "SKU \"" + item.getSku() + "\" already exists in inventory.");
        inventoryDAO.insert(item);
    }

    public void update(MedicalInventory item) throws ServiceException {
        int rows = inventoryDAO.update(item);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Inventory item with id " + item.getId() + " not found.");
    }

    public void remove(long itemId) throws ServiceException {
        int rows = inventoryDAO.delete(itemId);
        if (rows == 0)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Inventory item with id " + itemId + " not found.");
    }

    public List<MedicalInventory> getAll() throws ServiceException {
        return inventoryDAO.findAll();
    }

    public List<MedicalInventory> search(String keyword) throws ServiceException {
        return inventoryDAO.search(keyword);
    }

    // Business rule: items at or below reorder level are considered low stock
    public List<MedicalInventory> getLowStock() throws ServiceException {
        return inventoryDAO.findLowStock();
    }
}
