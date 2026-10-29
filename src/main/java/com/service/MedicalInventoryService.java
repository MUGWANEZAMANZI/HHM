package com.service;

import com.dao.MedicalInventoryDAO;
import com.dao.MedicalItemDAO;
import com.model.MedicalInventory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalInventoryService {

    private final MedicalInventoryDAO inventoryDAO;
    private final MedicalItemDAO medicalItemDAO;

    public MedicalInventoryService(MedicalInventoryDAO inventoryDAO,
                                   MedicalItemDAO medicalItemDAO) {
        this.inventoryDAO   = inventoryDAO;
        this.medicalItemDAO = medicalItemDAO;
    }

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
        if (inventoryDAO.findById(itemId) == null)
            throw new ServiceException(ServiceException.Type.NOT_FOUND,
                "Inventory item with id " + itemId + " not found.");

        // Delete child MedicalItems first to satisfy the FK constraint,
        // then delete the parent inventory row.
        medicalItemDAO.deleteByInventoryId(itemId);
        inventoryDAO.delete(itemId);
    }

    public List<MedicalInventory> getAll() throws ServiceException {
        return inventoryDAO.findAll();
    }

    public List<MedicalInventory> search(String keyword) throws ServiceException {
        return inventoryDAO.search(keyword);
    }

    public List<MedicalInventory> getLowStock() throws ServiceException {
        return inventoryDAO.findLowStock();
    }
}
