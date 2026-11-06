package com.hhm.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@ToString(exclude = "password")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "email", name = "uk_user_email"))
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String email;
    private String password;
    private LocalDateTime registeredAt;

    // Populated for OAuth2 users (e.g. Google); null for form-registered users.
    @Column(name = "display_name")
    private String displayName;

    // "GOOGLE" for OAuth2 users; null for form-registered users.
    private String provider;

    // Reads the discriminator column so getAuthorities() can derive the role.
    // insertable/updatable = false because Hibernate owns this column.
    @Column(name = "user_type", insertable = false, updatable = false)
    private String userType;

    public User() {}

    public User(String email, String password, LocalDateTime registeredAt) {
        this.email = email;
        this.password = password;
        this.registeredAt = registeredAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Derives DOCTOR, PATIENT, ADMIN, etc. directly from the discriminator value.
    // Pending OAuth2 users have userType null and get no authorities until promoted.
    // Use hasAuthority('DOCTOR') in @PreAuthorize — NOT hasRole (which adds ROLE_ prefix).
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (userType == null) return List.of();
        return List.of(new SimpleGrantedAuthority(userType));
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String getUsername() { return this.email; }

    @Override
    public boolean isEnabled() { return true; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }

    public String getUserType() { return userType; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
