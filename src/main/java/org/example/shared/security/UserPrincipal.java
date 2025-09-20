package org.example.shared.security;

import org.example.shared.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserPrincipal implements UserDetails {
    private final User user;
    public UserPrincipal(User user) { this.user = user; }
    public User getUser() { return user; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return java.util.List.of(); }
    @Override public String getPassword() { return user.getPasswordHash(); }
    @Override public String getUsername() { return user.getEmail(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
