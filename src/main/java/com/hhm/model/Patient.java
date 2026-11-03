package com.hhm.model;

import jakarta.persistence.*;
import lombok.ToString;

@Entity
@ToString
@DiscriminatorValue("PATIENT")
public class Patient extends User{
    @Column(nullable = false)
    private String name;
    @Column(nullable = true)
    private String address;
    @Column(unique = true,nullable = false)
    private String phone;

    public Patient() {
        super();
    }

    public Patient(String name, String phone) {
        super();
        this.name = name;
        this.phone = phone;

    }

    public long getId() { return super.getId(); }
    public void setId(long id) { super.setId(id); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

}
