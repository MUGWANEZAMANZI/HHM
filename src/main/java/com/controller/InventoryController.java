package com.controller;

import com.dto.ApiResult;
import com.dto.InventoryRequest;
import com.model.MedicalInventory;
import com.service.MedicalInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory", description = "Operations for managing medical inventory")
public class InventoryController {

    private final MedicalInventoryService inventoryService;

    public InventoryController(MedicalInventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List all inventory items",
               description = "Returns all medical items. Pass ?search= to filter by name or SKU.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory returned successfully")
    })
    public ResponseEntity<ApiResult<List<MedicalInventory>>> getAllItems(
            @Parameter(description = "Filter by item name or SKU")
            @RequestParam(required = false) String search) {
        List<MedicalInventory> result = (search != null && !search.isBlank())
                ? inventoryService.search(search)
                : inventoryService.getAll();
        return ResponseEntity.ok(ApiResult.success("Inventory retrieved", result));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List low-stock items",
               description = "Returns items whose quantity on hand is at or below the reorder level.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Low-stock items returned")
    })
    public ResponseEntity<ApiResult<List<MedicalInventory>>> getLowStock() {
        return ResponseEntity.ok(
                ApiResult.success("Low-stock items retrieved", inventoryService.getLowStock()));
    }

    @PostMapping
    @Operation(summary = "Add an inventory item",
               description = "Creates a new medical inventory entry. SKU must be unique.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Item added successfully"),
        @ApiResponse(responseCode = "409", description = "SKU already exists"),
        @ApiResponse(responseCode = "422", description = "Validation failed — see message field")
    })
    public ResponseEntity<ApiResult<Void>> addItem(@Valid @RequestBody InventoryRequest req) {
        MedicalInventory item = new MedicalInventory(
                req.getName(), req.getSku(),
                req.getQuantityOnHand(), req.getReorderLevel());
        inventoryService.add(item);
        return ResponseEntity.status(201)
                .body(ApiResult.created("Inventory item added successfully", null));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an inventory item",
               description = "Updates name, SKU, quantity, and reorder level.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item updated successfully"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<ApiResult<Void>> updateItem(
            @Parameter(description = "ID of the inventory item to update", example = "1")
            @PathVariable long id,
            @Valid @RequestBody InventoryRequest req) {
        MedicalInventory item = new MedicalInventory(
                req.getName(), req.getSku(),
                req.getQuantityOnHand(), req.getReorderLevel());
        item.setId(id);
        inventoryService.update(item);
        return ResponseEntity.ok(ApiResult.success("Inventory item " + id + " updated", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an inventory item",
               description = "Permanently removes an inventory item.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Item deleted — no content"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "ID of the inventory item to delete", example = "1")
            @PathVariable long id) {
        inventoryService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
