package com.dao;

import com.model.User;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

public class UserDAO {


    JdbcTemplate jdbc;
    public UserDAO(JdbcTemplate jdbc)
    {
        this.jdbc = jdbc;
    }

    public int registerUser(User user)
    {
        return jdbc.update("INSERT INTO users (email, password, registeredAt) VALUES (?, ?, ?)",
                user.getEmail(), user.getPassword(), Timestamp.valueOf(user.getRegisteredAt()));

    }

}
