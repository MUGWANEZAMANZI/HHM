package com.hhm.dto;

public enum Type
{
    ADMIN,
    DOCTOR,
    NURSE,
    RECEPTIONIST,
    PATIENT;

    public String Type(){
        return this.name();
    }

}
