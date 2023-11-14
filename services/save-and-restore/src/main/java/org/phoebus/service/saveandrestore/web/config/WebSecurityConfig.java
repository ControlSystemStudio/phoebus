package org.phoebus.service.saveandrestore.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.ldap.LdapBindAuthenticationManagerFactory;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.PersonContextMapper;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@SuppressWarnings("unused")
public class WebSecurityConfig {

    /**
     * Authentication implementation.
     */
    @Value("${auth.impl:demo}")
    protected String authenitcationImplementation;

    /**
     * External Active Directory configuration properties
     */
    @Value("${ad.url:ldap://localhost:389/}")
    String ad_url;
    @Value("${ad.domain}")
    String ad_domain;
    /**
     * External LDAP configuration properties
     */
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
    @Value("${ldap.user.search.base:invalid}")
    String ldap_user_search_base;
    @Value("${ldap.user.search.filter:invalid}")
    String ldap_user_search_filter;

    @Value("${role.user:sar-user}")
    public String roleUser;

    @Value("${role.admin:sar-admin}")
    public String roleAdmin;

    @Value("${demo.user:user}")
    public String demoUser;

    @Value("${demo.user.password:userPass}")
    public String demoUserPassword;

    @Value("${demo.admin:admin}")
    public String demoAdmin;

    @Value("${demo.admin.password:adminPass}")
    public String demoAdminPassword;

    @Value("${demo.readOnly:johndoe}")
    public String demoReadOnly;

    @Value("${demo.readOnly.password:1234}")
    public String demoReadOnlyPassword;

    @Bean
    public String roleUser() {
        return roleUser.toUpperCase();
    }

    @Bean
    public String roleAdmin() {
        return roleAdmin.toUpperCase();
    }

    @Bean
    public String demoUser(){
        return demoUser;
    }

    @Bean
    public String demoUserPassword(){
        return demoUserPassword;
    }

    @Bean
    public String demoAdmin(){
        return demoAdmin;
    }

    @Bean
    public String demoAdminPassword(){
        return demoAdminPassword;
    }

    @Bean
    public String demoReadOnly(){
        return demoReadOnly;
    }

    @Bean
    public String demoReadOnlyPassword(){
        return demoReadOnlyPassword;
    }

    @Bean
    public String authenticationImplementation(){
        return authenitcationImplementation;
    }
    @Bean
    public WebSecurityCustomizer ignoringCustomizer() {
        return web -> {
            // The below lists exceptions for authentication.
            web.ignoring().antMatchers(HttpMethod.GET, "/**");
            web.ignoring().antMatchers(HttpMethod.POST, "/**/login*");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.authorizeRequests().anyRequest().authenticated();
        http.httpBasic();
        return http.build();
    }

    @Bean
    @Conditional(LdapAuthCondition.class)
    public DefaultSpringSecurityContextSource contextSourceFactoryBeanLdap() {
        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldap_url);
        if (ldap_manager_dn != null && !ldap_manager_dn.isEmpty() && ldap_manager_password != null && !ldap_manager_password.isEmpty()) {
            contextSource.setUserDn(ldap_manager_dn);
            contextSource.setPassword(ldap_manager_password);
        }
        contextSource.setBase(ldap_base_dn);
        return contextSource;
    }

    @Bean
    @Conditional(LdapAuthCondition.class)
    public AuthenticationManager ldapAuthenticationManager(
            BaseLdapPathContextSource contextSource) {
        LdapBindAuthenticationManagerFactory factory =
                new LdapBindAuthenticationManagerFactory(contextSource);
        factory.setUserDnPatterns(ldap_user_dn_pattern);
        factory.setUserDetailsContextMapper(new PersonContextMapper());

        factory.setLdapAuthoritiesPopulator(authorities(contextSource));
        return factory.createAuthenticationManager();
    }

    @Bean
    @Conditional(LdapAuthCondition.class)
    public LdapAuthoritiesPopulator authorities(BaseLdapPathContextSource contextSource) {
        DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
        myAuthPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
        myAuthPopulator.setSearchSubtree(true);
        myAuthPopulator.setIgnorePartialResultException(true);
        LdapAuthenticationProviderConfigurer configurer = new LdapAuthenticationProviderConfigurer();
        if (ldap_user_dn_pattern != null && !ldap_user_dn_pattern.isEmpty()) {
            configurer.userDnPatterns(ldap_user_dn_pattern);
        }
        if (ldap_user_search_filter != null && !ldap_user_search_filter.isEmpty()) {
            configurer.userSearchFilter(ldap_user_search_filter);
        }
        if (ldap_user_search_base != null && !ldap_user_search_base.isEmpty()) {
            configurer.userSearchBase(ldap_user_search_base);
        }
        configurer.contextSource(contextSource);
        return myAuthPopulator;
    }

    @Bean
    @ConditionalOnProperty(name = "auth.impl", havingValue = "ad")
    public AuthenticationManager authenticationProvider() throws Exception {
        ActiveDirectoryLdapAuthenticationProvider adProvider =
                new ActiveDirectoryLdapAuthenticationProvider(ad_domain, ad_url);
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);
        adProvider.setUserDetailsContextMapper(new PersonContextMapper());
        SimpleAuthorityMapper simpleAuthorityMapper = new SimpleAuthorityMapper();
        simpleAuthorityMapper.setConvertToUpperCase(true);
        adProvider.setAuthoritiesMapper(simpleAuthorityMapper);
        return new AuthenticationManagerBuilder(new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        }).authenticationProvider(adProvider).build();
    }

    @Bean
    @ConditionalOnProperty(name = "auth.impl", havingValue = "demo")
    public AuthenticationManager demoAuthenticationManager(AuthenticationManagerBuilder auth) throws Exception {
        return new AuthenticationManagerBuilder(new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        }).inMemoryAuthentication()
                .passwordEncoder(encoder())
                .withUser(demoAdmin).password(encoder().encode(demoAdminPassword)).roles(roleAdmin()).and()
                .withUser(demoUser).password(encoder().encode(demoUserPassword)).roles(roleUser()).and()
                .withUser(demoReadOnly).password(encoder().encode(demoReadOnlyPassword)).roles().and().and().build();
    }

    @Bean
    @Scope("singleton")
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @SuppressWarnings("unused")
    @Bean
    @Scope("singleton")
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    /**
     * Configures role hierarchy, i.e. user - superuser - admin. Do not remove this {@link Bean}!
     * <h2>NOTE!</h2>
     * Some Spring Security documentation will state that &quot;and&quot; can be used instead of new-line char to
     * separate rule items. But that does NOT work, at least not with the Spring Security version used in this project.
     *
     * @return A {@link RoleHierarchy} object.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_" + roleAdmin.toUpperCase() + " > ROLE_" + roleUser.toUpperCase());
        return hierarchy;
    }

    /**
     * {@link Condition} subclass used to select ldap and ldap_embedded
     * authentication/authorization provider.
     */
    private static class LdapAuthCondition implements Condition {
        /**
         * @param context  the condition context
         * @param metadata the metadata of the {@link org.springframework.core.type.AnnotationMetadata class}
         *                 or {@link org.springframework.core.type.MethodMetadata method} being checked
         * @return <code>true</code> if application property <code>auth.impl</code> is <code>ldap</code>
         * or <code>ldap_embedded</code>, otherwise <code>false</code>.
         */
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String testValue = context.getEnvironment().getProperty("auth.impl");
            return "ldap".equals(testValue) || "ldap_embedded".equals(testValue);
        }
    }
}
