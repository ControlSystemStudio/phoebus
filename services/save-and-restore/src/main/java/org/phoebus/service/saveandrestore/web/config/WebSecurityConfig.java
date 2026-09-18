/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */package org.phoebus.service.saveandrestore.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * {@link Configuration} class setting up authentication/authorization depending on the
 * auth.impl application property.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@SuppressWarnings("unused")
public class WebSecurityConfig {

    public static final String IN_MEMORY = "inMemory";
    public static final String EMBEDDED_LDAP = "embeddedLdap";
    public static final String LDAP = "ldap";
    public static final String ACTIVE_DIRECTORY = "activeDirectory";

    /**
     * List of supported provider implementations.
     */
    private static final String[] PROVIDER_NAME_LIST = {LDAP, ACTIVE_DIRECTORY, EMBEDDED_LDAP, IN_MEMORY};

    public static final String PROVIDER_LIST_PROPERTY_NAME = "authenticationProviders";


    @Value("${role.user:sar-user}")
    private String roleUser;

    @Value("${role.admin:sar-admin}")
    private String roleAdmin;

    /**
     *
     * @return name of regular user role
     */
    @Bean
    public String roleUser() {
        return roleUser.toUpperCase();
    }

    /**
     *
     * @return name of admin user role
     */
    @Bean
    public String roleAdmin() {
        return roleAdmin.toUpperCase();
    }


    /**
     *
     * @return The authentication implementation as specified in application property auth.impl.
     */
    @SuppressWarnings("unused")
    @Autowired
    private ApplicationContext context;

    private final Logger logger = Logger.getLogger(WebSecurityConfig.class.getName());

    @Bean
    @Scope("singleton")
    public AuthenticationManager authenticationManager() throws Exception {
        return authentication -> {
            for (String providerName : PROVIDER_NAME_LIST) {
                if (context.containsBean(providerName)) {
                    AuthenticationProvider authenticationProvider = context.getBean(providerName, AuthenticationProvider.class);
                    logger.log(Level.INFO, "Authenticating user '" + authentication.getPrincipal() + "' with provider '" + providerName + "'");
                    try {
                        return authenticationProvider.authenticate(authentication);
                    } catch (AuthenticationException e) {
                        logger.log(Level.WARNING, "Authentication failed using provider '" + providerName + "'", e);
                    }
                }
            }
            throw new UsernameNotFoundException("");
        };
    }

    /**
     * Configures endpoints not subject to authentication.
     * @return A {@link WebSecurityCustomizer} object.
     */
    @Bean
    public WebSecurityCustomizer ignoringCustomizer() {
        return web -> {
            // The below lists exceptions for authentication.
            web.ignoring().requestMatchers(HttpMethod.GET, "/**");
            web.ignoring().requestMatchers(HttpMethod.POST, "/**/login*");
        };
    }

    /**
     * Configures http security policy.
     * @param http A {@link HttpSecurity} object provided by Spring
     * @return A {@link SecurityFilterChain} object.
     * @throws Exception on failure
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        http.httpBasic(withDefaults());
        return http.build();
    }

    /**
     *
     * @return An {@link ObjectMapper} object used for serialization/deserialization.
     */
    @SuppressWarnings("unused")
    @Bean
    @Scope("singleton")
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
    }

    /**
     * Configures role hierarchy, i.e. user - superuser - admin. Do not remove this {@link Bean}!
     * <h4>NOTE!</h4>
     * Some Spring Security documentation will state that &quot;and&quot; can be used instead of new-line char to
     * separate rule items. But that does NOT work, at least not with the Spring Security version used in this project.
     *
     * @return A {@link RoleHierarchy} object.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_" + roleAdmin.toUpperCase() + " > ROLE_" + roleUser.toUpperCase());
    }
}
