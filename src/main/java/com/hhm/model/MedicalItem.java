package com.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.ToString;

@Entity
@ToString
public class MedicalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long inventoryId;
    private int quantity;
    private String category;

    public MedicalItem() {}

    public MedicalItem(long inventoryId, int quantity, String category) {
        this.inventoryId = inventoryId;
        this.quantity = quantity;
        this.category = category;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getInventoryId() { return inventoryId; }
    public void setInventoryId(long inventoryId) { this.inventoryId = inventoryId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
