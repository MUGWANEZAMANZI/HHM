package com.hhm.model;

import jakarta.persistence.*;
import lombok.ToString;

@Entity
@ToString
public class MedicalItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private MedicalInventory inventory;

    private int quantity;
    private String category;

    public MedicalItem() {}

    public MedicalItem(MedicalInventory inventory, int quantity, String category) {
        this.inventory = inventory;
        this.quantity = quantity;
        this.category = category;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public MedicalInventory getInventory() { return inventory; }
    public void setInventory(MedicalInventory inventory) { this.inventory = inventory; }
    public long getInventoryId() { return inventory != null ? inventory.getId() : 0; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
