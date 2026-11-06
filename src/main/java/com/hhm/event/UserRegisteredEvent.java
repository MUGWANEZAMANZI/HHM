package com.hhm.event;

import com.hhm.dto.Type;

// Fired after ANY user is saved — PATIENT, DOCTOR, ADMIN, etc.
// Spring holds it until the transaction commits; if the TX rolls back this
// event is discarded and no notification is sent.
public record UserRegisteredEvent(String email, String name, Type type) {}
