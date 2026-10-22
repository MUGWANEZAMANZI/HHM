package service;

import dao.MedicalInventoryDAO;
import model.MedicalInventory;

import java.sql.SQLException;
import java.util.List;

public class MedicalInventoryService {

    private final MedicalInventoryDAO inventoryDAO = new MedicalInventoryDAO();

    // Business rule: SKU must be unique across all inventory items
    public void add(MedicalInventory item) throws ServiceException {
        try {
            boolean skuTaken = inventoryDAO.findAll().stream()
                .anyMatch(m -> m.getSku().equalsIgnoreCase(item.getSku()));
            if (skuTaken)
                throw new ServiceException("SKU \"" + item.getSku() + "\" already exists in inventory.");
            inventoryDAO.insert(item);
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public void update(MedicalInventory item) throws ServiceException {
        try { inventoryDAO.update(item); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public void remove(long itemId) throws ServiceException {
        try { inventoryDAO.delete(itemId); }
        catch (SQLException e) {
            // FK violation means item is used in prescriptions
            if (e.getMessage().contains("foreign key constraint"))
                throw new ServiceException("Cannot delete: item is referenced in existing prescriptions.");
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    public List<MedicalInventory> getAll() throws ServiceException {
        try { return inventoryDAO.findAll(); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    public List<MedicalInventory> search(String keyword) throws ServiceException {
        try { return inventoryDAO.search(keyword); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }

    // Business rule: items at or below reorder level are considered low stock
    public List<MedicalInventory> getLowStock() throws ServiceException {
        try { return inventoryDAO.findLowStock(); }
        catch (SQLException e) { throw new ServiceException(e.getMessage(), e); }
    }
}
