/*
 * Copyright 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.ldap.core.ContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.PasswordComparisonAuthenticator;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.util.Arrays;

/**
 * Security config for embedded LDAP authentication/authorization. Instantiated only if runtime property
 * <code>authenticationProviders</code> contains <code>embedded_ldap</code>.
 */
@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
@Conditional(value = EmbeddedLdapSecurityConfig.EmbeddedLdapCondition.class)
@PropertySource("${embeddedLdapPropertySource}")
public class EmbeddedLdapSecurityConfig {

    @Value("${embedded_ldap.url:ldap://localhost:8389/dc=sar,dc=local}")
    String embedded_ldap_url;
    @Value("${embedded_ldap.base.dn}")
    String embedded_ldap_base_dn;
    @Value("${embedded_ldap.user.dn.pattern}")
    String embedded_ldap_user_dn_pattern;
    @Value("${embedded_ldap.groups.search.base}")
    String embedded_ldap_groups_search_base;
    @Value("${embedded_ldap.groups.search.pattern}")
    String embedded_ldap_groups_search_pattern;
    @Value("${spring.ldap.embedded.port:8389}")
    int spring_ldap_embedded_port;
    @Value("${embedded_ldap_ldif:classpath:sar.ldif}")
    String embedded_ldap_ldif;

    @Bean(WebSecurityConfig.EMBEDDED_LDAP)
    public AuthenticationProvider ldapAuthenticationProvider(DefaultSpringSecurityContextSource contextSource) {

        PasswordComparisonAuthenticator passwordComparisonAuthenticator = new PasswordComparisonAuthenticator(contextSource);
        passwordComparisonAuthenticator.setPasswordEncoder(new BCryptPasswordEncoder());
        passwordComparisonAuthenticator.setUserDnPatterns(new String[]{embedded_ldap_user_dn_pattern});

        return new LdapAuthenticationProvider(passwordComparisonAuthenticator,
                defaultLdapAuthoritiesPopulator(contextSource));
    }


    @Bean
    public SaveAndRestoreUnboundIdContainer ldapContainer() {
        SaveAndRestoreUnboundIdContainer container = new SaveAndRestoreUnboundIdContainer(embedded_ldap_base_dn, embedded_ldap_ldif);
        container.setPort(spring_ldap_embedded_port);
        return container;
    }

    @Bean
    public DefaultSpringSecurityContextSource contextSource(SaveAndRestoreUnboundIdContainer container) {
        return new DefaultSpringSecurityContextSource(embedded_ldap_url);
    }

    private DefaultLdapAuthoritiesPopulator defaultLdapAuthoritiesPopulator(ContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, embedded_ldap_groups_search_base);
        myAuthPopulator.setGroupSearchFilter(embedded_ldap_groups_search_pattern);
        myAuthPopulator.setSearchSubtree(true);
        myAuthPopulator.setIgnorePartialResultException(true);

        return myAuthPopulator;
    }

    public static class EmbeddedLdapCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = context.getEnvironment().getProperty(WebSecurityConfig.PROVIDER_LIST_PROPERTY_NAME);
            if (value == null || value.isEmpty()) {
                return false;
            }
            String[] values = value.split(",");
            return Arrays.asList(values).contains(WebSecurityConfig.EMBEDDED_LDAP);
        }
    }
}
