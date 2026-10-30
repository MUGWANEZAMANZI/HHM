package com.hhm.graphql;

import com.hhm.dto.InventoryRequest;
import com.hhm.model.MedicalInventory;
import com.hhm.service.MedicalInventoryService;
import jakarta.validation.Valid;
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
    public String addInventoryItem(@Argument @Valid InventoryRequest input) {
        MedicalInventory item = new MedicalInventory(
                input.getName(), input.getSku(), input.getQuantityOnHand(), input.getReorderLevel());
        inventoryService.add(item);
        return "Inventory item added successfully";
    }

    @MutationMapping
    public boolean deleteInventoryItem(@Argument Long id) {
        inventoryService.remove(id);
        return true;
    }
}
