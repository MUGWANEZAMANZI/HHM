package com.hhm.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.ToString;

@Entity
@ToString
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    private String name;

    public Admin() { super(); }

    public Admin(String name) {
        super();
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
