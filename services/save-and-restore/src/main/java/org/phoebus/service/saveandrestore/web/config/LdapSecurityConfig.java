/*
 * Copyright 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.ldap.core.support.DefaultTlsDirContextAuthenticationStrategy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.Arrays;

/**
 * Security config for LDAP authentication/authorization. Instantiated only if runtime property
 * <code>authenticationProviders</code> contains <code>ldap</code>.
 */
@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
@Conditional(value = LdapSecurityConfig.LdapCondition.class)
@PropertySource("${ldapPropertySource}")
public class LdapSecurityConfig  {

    @Value("${ldap.enabled:false}")
    boolean ldap_enabled;
    @Value("${ldap.starttls:false}")
    boolean ldap_starttls;
    @Value("${ldap.urls:ldaps://localhost:389/}")
    String ldap_url;
    @Value("${ldap.base.dn}")
    String ldap_base_dn;
    @Value("${ldap.user.dn.pattern}")
    String ldap_user_dn_pattern;
    @Value("${ldap.groups.search.base}")
    String ldap_groups_search_base;
    @Value("${ldap.groups.search.pattern}")
    String ldap_groups_search_pattern;
    @Value("${ldap.manager.dn}")
    String ldap_manager_dn;
    @Value("${ldap.manager.password}")
    String ldap_manager_password;
    @Value("${ldap.user.search.base}")
    String ldap_user_search_base;
    @Value("${ldap.user.search.filter}")
    String ldap_user_search_filter;

    @Bean(WebSecurityConfig.LDAP)
    public AuthenticationProvider authenticationProvider() {
        DefaultSpringSecurityContextSource defaultSpringSecurityContextSource = defaultSpringSecurityContextSource();
        BindAuthenticator authenticator = new BindAuthenticator(defaultSpringSecurityContextSource);
        authenticator.setUserDnPatterns(new String[]{ldap_user_dn_pattern});
        FilterBasedLdapUserSearch filterBasedLdapUserSearch =
                new FilterBasedLdapUserSearch(ldap_user_search_base, ldap_user_search_filter, defaultSpringSecurityContextSource);
        authenticator.setUserSearch(filterBasedLdapUserSearch);

        return new LdapAuthenticationProvider(authenticator,
                ldapAuthoritiesPopulator(defaultSpringSecurityContextSource));
    }

    private LdapAuthoritiesPopulator ldapAuthoritiesPopulator(DefaultSpringSecurityContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator myAuthPopulator =
                new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
        myAuthPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
        myAuthPopulator.setSearchSubtree(true);
        myAuthPopulator.setIgnorePartialResultException(true);

        return myAuthPopulator;
    }

    private DefaultSpringSecurityContextSource defaultSpringSecurityContextSource() {
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldap_url);
        if (ldap_manager_dn != null && !ldap_manager_dn.isEmpty() && ldap_manager_password != null && !ldap_manager_password.isEmpty()) {
            contextSource.setUserDn(ldap_manager_dn);
            contextSource.setPassword(ldap_manager_password);
        }
        if (ldap_starttls) {
            contextSource.setAuthenticationStrategy(new DefaultTlsDirContextAuthenticationStrategy());
        }
        contextSource.afterPropertiesSet();

        return contextSource;
    }

    public static class LdapCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = context.getEnvironment().getProperty(WebSecurityConfig.PROVIDER_LIST_PROPERTY_NAME);
            if (value == null || value.isEmpty()) {
                return false;
            }
            String[] values = value.split(",");
            return Arrays.asList(values).contains(WebSecurityConfig.LDAP);
        }
    }
}

