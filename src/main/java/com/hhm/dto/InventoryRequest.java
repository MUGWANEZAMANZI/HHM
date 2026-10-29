package com.hhm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request body for adding or updating a medical inventory item")
public class InventoryRequest {

    @NotBlank(message = "Item name is required")
    @Schema(description = "Name of the medical item", example = "Paracetamol 500mg")
    private String name;

    @NotBlank(message = "SKU is required")
    @Schema(description = "Unique stock-keeping unit code", example = "MED-PCM-500")
    private String sku;

    @Min(value = 0, message = "Quantity on hand cannot be negative")
    @Schema(description = "Current quantity in stock", example = "200")
    private int quantityOnHand;

    @Min(value = 0, message = "Reorder level cannot be negative")
    @Schema(description = "Quantity at which restocking is triggered", example = "50")
    private int reorderLevel;
}
