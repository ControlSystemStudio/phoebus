/*
 * Copyright 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Arrays;

/**
 * Security config for in-memory authentication/authorization. Instantiated only if runtime property
 * <code>authenticationProviders</code> contains <code>inMemory</code>.
 */
@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
@Conditional(value = InMemorySecurityConfig.InMemoryCondition.class)
@PropertySource("${inMemoryPropertySource}")
public class InMemorySecurityConfig {

    @Value("${demo.user:user}")
    private String demoUser;

    @Value("${demo.user.password:userPass}")
    private String demoUserPassword;

    @Value("${demo.admin:admin}")
    private String demoAdmin;

    @Value("${demo.admin.password:adminPass}")
    private String demoAdminPassword;

    @Value("${demo.readOnly:johndoe}")
    private String demoReadOnly;

    @Value("${demo.readOnly.password:1234}")
    private String demoReadOnlyPassword;

    @Autowired
    private String roleUser;

    @Autowired
    private String roleAdmin;

    @Bean(WebSecurityConfig.IN_MEMORY)
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService());
        daoAuthenticationProvider.setPasswordEncoder(new BCryptPasswordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserDetails user = User.builder()
                .username(demoUser)
                .password(passwordEncoder.encode(demoUserPassword))
                .roles(roleUser.toUpperCase())
                .build();
        UserDetails adminUser = User.builder()
                .username(demoAdmin)
                .password(passwordEncoder.encode(demoAdminPassword))
                .roles(roleAdmin.toUpperCase())
                .build();
        UserDetails readOnlyUser = User.builder()
                .username(demoReadOnly)
                .password(passwordEncoder.encode(demoReadOnlyPassword))
                .build();
        return new InMemoryUserDetailsManager(user, adminUser, readOnlyUser);
    }

    public static class InMemoryCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = context.getEnvironment().getProperty(WebSecurityConfig.PROVIDER_LIST_PROPERTY_NAME);
            if (value == null || value.isEmpty()) {
                return true;
            }
            String[] values = value.split(",");
            return Arrays.asList(values).contains(WebSecurityConfig.IN_MEMORY);
        }
    }
}