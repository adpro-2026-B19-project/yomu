package id.ac.ui.cs.advprog.yomu.auth.service;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUserPrincipal implements UserDetails {

    private final String username;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUserPrincipal(
            String username,
            String email,
            String password,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
