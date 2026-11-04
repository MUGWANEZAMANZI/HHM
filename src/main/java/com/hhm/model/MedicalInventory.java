package com.hhm.model;

import jakarta.persistence.*;
import lombok.ToString;

@Entity
@ToString
@Table(name = "medical_inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sku"}, name = "uk_inventory_sku")
})
public class MedicalInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String name;
    private String sku;
    private int quantityOnHand;
    private int reorderLevel;

    public MedicalInventory() {}

    public MedicalInventory(String name, String sku, int quantityOnHand, int reorderLevel) {
        this.name = name;
        this.sku = sku;
        this.quantityOnHand = quantityOnHand;
        this.reorderLevel = reorderLevel;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getQuantityOnHand() { return quantityOnHand; }
    public void setQuantityOnHand(int quantityOnHand) { this.quantityOnHand = quantityOnHand; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
}
