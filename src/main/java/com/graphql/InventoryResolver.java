package com.graphql;

import com.model.MedicalInventory;
import com.service.MedicalInventoryService;
import com.service.ServiceException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class InventoryResolver {

    private final MedicalInventoryService inventoryService;

    public InventoryResolver(MedicalInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @QueryMapping
    public List<MedicalInventory> inventory(@Argument String search) {
        if (search != null && !search.isBlank())
            return inventoryService.search(search);
        return inventoryService.getAll();
    }

    @QueryMapping
    public List<MedicalInventory> lowStockItems() {
        return inventoryService.getLowStock();
    }

    @MutationMapping
    public String addInventoryItem(
            @Argument String name,
            @Argument String sku,
            @Argument int quantityOnHand,
            @Argument int reorderLevel) {
        try {
            MedicalInventory item = new MedicalInventory(name, sku, quantityOnHand, reorderLevel);
            inventoryService.add(item);
            return "Inventory item added successfully";
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @MutationMapping
    public boolean deleteInventoryItem(@Argument Long id) {
        try {
            inventoryService.remove(id);
            return true;
        } catch (ServiceException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
