package com.hhm.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@DiscriminatorValue("NURSE")
@Data
public class Nurse extends User {
    private String nurseName;
}
