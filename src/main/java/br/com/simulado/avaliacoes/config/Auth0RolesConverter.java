package br.com.simulado.avaliacoes.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class Auth0RolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String rolesClaim;

    public Auth0RolesConverter(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        addRoles(authorities, values(jwt.getClaims().get(rolesClaim)));
        addPermissions(authorities, values(jwt.getClaims().get("permissions")));
        return authorities;
    }

    private void addRoles(Set<GrantedAuthority> authorities, List<String> roles) {
        for (String role : roles) {
            String normalized = normalize(role);
            if (normalized.equals("ADMIN") || normalized.equals("USER")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
            }
        }
    }

    private void addPermissions(Set<GrantedAuthority> authorities, List<String> permissions) {
        for (String permission : permissions) {
            switch (normalize(permission)) {
                case "AVALIACOES:WRITE" -> authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                case "AVALIACOES:READ" -> authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                default -> {
                }
            }
        }
    }

    private String normalize(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private List<String> values(Object claim) {
        if (claim instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        if (claim instanceof String value) {
            return List.of(value);
        }
        return List.of();
    }
}
