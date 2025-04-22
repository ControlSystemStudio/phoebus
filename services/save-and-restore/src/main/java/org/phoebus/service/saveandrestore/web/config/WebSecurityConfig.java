package org.phoebus.service.saveandrestore.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpMethod;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.ldap.LdapBindAuthenticationManagerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.PersonContextMapper;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.List;

/**
 * {@link Configuration} class setting up authentication/authorization depending on the
 * auth.impl application property.
 */
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
    private String roleUser;

    @Value("${role.admin:sar-admin}")
    private String roleAdmin;

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

    /**
     * @return name of regular user role
     */
    @Bean
    public String roleUser() {
        return roleUser.toUpperCase();
    }

    /**
     * @return name of admin user role
     */
    @Bean
    public String roleAdmin() {
        return roleAdmin.toUpperCase();
    }

    /**
     * @return Identity of demo regular user
     */
    @Bean
    public String demoUser() {
        return demoUser;
    }

    /**
     * @return Password of demo regular user
     */
    @Bean
    public String demoUserPassword() {
        return demoUserPassword;
    }

    /**
     * @return Identity of the demo admin user.
     */
    @Bean
    public String demoAdmin() {
        return demoAdmin;
    }

    /**
     * @return Password of the demo admin user.
     */
    @Bean
    public String demoAdminPassword() {
        return demoAdminPassword;
    }

    /**
     * @return Identity of the demo read-only user.
     */
    @Bean
    public String demoReadOnly() {
        return demoReadOnly;
    }

    /**
     * @return Password of the demo read-only user.
     */
    @Bean
    public String demoReadOnlyPassword() {
        return demoReadOnlyPassword;
    }

    /**
     * @return The authentication implementation as specified in application property auth.impl.
     */
    @SuppressWarnings("unused")
    @Bean
    public String authenticationImplementation() {
        return authenitcationImplementation;
    }

    /**
     * Configures endpoints not subject to authentication.
     *
     * @return A {@link WebSecurityCustomizer} object.
     */
    @Bean
    public WebSecurityCustomizer ignoringCustomizer() {
        return web -> {
            // The below lists exceptions for authentication.
            web.ignoring().requestMatchers(HttpMethod.GET, "/**");
            web.ignoring().requestMatchers(HttpMethod.POST, "/login");
        };
    }

    /**
     * Configures http security policy.
     *
     * @param http A {@link HttpSecurity} object provided by Spring
     * @return A {@link SecurityFilterChain} object.
     * @throws Exception on failure
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(Customizer.withDefaults());
        http.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> authorizationManagerRequestMatcherRegistry.anyRequest().authenticated());
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Created based on condition implemented in {@link LdapAuthCondition}.
     *
     * @return A {@link DefaultSpringSecurityContextSource} object
     */
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


    /**
     * Created based on condition implemented in {@link LdapAuthCondition}.
     *
     * @param contextSource provided by Spring
     * @return A {@link AuthenticationManager} object
     */
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

    /**
     * Created based on condition implemented in {@link LdapAuthCondition}.
     *
     * @param contextSource provided by Spring
     * @return A {@link LdapAuthoritiesPopulator} object
     */
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

    /**
     * Created only if application property auth.impl = ad.
     *
     * @return A {@link AuthenticationManager} object
     * @throws Exception on error
     */
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
        return new AuthenticationManagerBuilder(new org.springframework.security.config.ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        }).authenticationProvider(adProvider).build();
    }

    /**
     * Created only if application property auth.impl = demo.
     *
     * @param userDetailsService Injected by Spring
     * @param passwordEncoder    Injected by Spring
     * @return A {@link AuthenticationManager} object
     * @throws Exception on error, e.g. unknown user or wrong password.
     */
    @Bean
    @ConditionalOnProperty(name = "auth.impl", havingValue = "demo")
    public AuthenticationManager demoAuthenticationManager(UserDetailsService userDetailsService,
                                                           PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }

    @Bean
    @ConditionalOnProperty(name = "auth.impl", havingValue = "demo")
    public UserDetailsService userDetailsService() {
        return username -> {
            UserDetails userDetails;
            if (demoUser.equals(username)) {
                return new User(demoUser, encoder().encode(demoUserPassword), List.of(new SimpleGrantedAuthority(roleUser())));
            } else if (demoAdmin.equals(username)) {
                return new User(demoAdmin, encoder().encode(demoAdminPassword), List.of(new SimpleGrantedAuthority(roleAdmin())));
            } else if (demoReadOnly.equals(username)) {
                return new User(demoReadOnly, encoder().encode(demoReadOnlyPassword), Collections.emptyList());
            }
            throw new UsernameNotFoundException("Unknown user: " + username);
        };
    }

    /**
     * @return A {@link PasswordEncoder} object.
     */
    @Bean
    @Scope("singleton")
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * @return An {@link ObjectMapper} object used for serialization/deserialization.
     */
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
     * <h4>NOTE!</h4>
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
