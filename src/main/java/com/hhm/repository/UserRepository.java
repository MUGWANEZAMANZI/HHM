package com.hhm.repository;

import com.hhm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // Returns base User rows that have no discriminator value — i.e. pending OAuth2 users
    List<User> findByUserTypeIsNull();
}
