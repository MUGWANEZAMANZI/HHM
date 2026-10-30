package com.hhm.model;

import jakarta.persistence.*;
import lombok.ToString;

@Entity
@ToString
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = true)
    private String address;
    @Column(unique = true,nullable = false)
    private String phone;
    @Column(unique = true,nullable = false)
    private String email;

    public Patient() {}

    public Patient(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
