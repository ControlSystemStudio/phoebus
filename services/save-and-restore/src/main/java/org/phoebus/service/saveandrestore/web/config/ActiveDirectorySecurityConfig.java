/*
 * Copyright 2025 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

import java.util.Arrays;

/**
 * Security config for Active Directory authentication/authorization. Instantiated only if runtime property
 * <code>authenticationProviders</code> contains <code>activeDirectory</code>.
 */
@SuppressWarnings("unused")
@EnableWebSecurity
@Configuration
@Conditional(value = ActiveDirectorySecurityConfig.ActiveDirectoryCondition.class)
@PropertySource("${activeDirectoryPropertySource}")
public class ActiveDirectorySecurityConfig extends WebSecurityConfig {

    @Value("${ad.url:ldap://localhost:389/}")
    String ad_url;
    @Value("${ad.domain}")
    String ad_domain;

    @Bean(WebSecurityConfig.ACTIVE_DIRECTORY)
    public AuthenticationProvider activeDirectoryLdapAuthenticationProvider() {
        ActiveDirectoryLdapAuthenticationProvider adProvider = new ActiveDirectoryLdapAuthenticationProvider(ad_domain, ad_url);
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);

        return adProvider;
    }

    public static class ActiveDirectoryCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String value = context.getEnvironment().getProperty(WebSecurityConfig.PROVIDER_LIST_PROPERTY_NAME);
            if (value == null || value.isEmpty()) {
                return false;
            }
            String[] values = value.split(",");
            return Arrays.asList(values).contains(WebSecurityConfig.ACTIVE_DIRECTORY);
        }
    }
}
