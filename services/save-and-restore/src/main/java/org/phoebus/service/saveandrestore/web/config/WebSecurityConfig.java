package org.phoebus.service.saveandrestore.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.logging.Level;
import java.util.logging.Logger;

@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
@SuppressWarnings("unused")
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

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
    @Value("${ldap.user.search.base}")
    String ldap_user_search_base;
    @Value("${ldap.user.search.filter}")
    String ldap_user_search_filter;

    /**
     * Embedded LDAP configuration properties
     */
    @Value("${embedded_ldap.urls:ldaps://localhost:389/}")
    String embedded_ldap_url;
    @Value("${embedded_ldap.base.dn}")
    String embedded_ldap_base_dn;
    @Value("${embedded_ldap.user.dn.pattern}")
    String embedded_ldap_user_dn_pattern;
    @Value("${embedded_ldap.groups.search.base}")
    String embedded_ldap_groups_search_base;
    @Value("${embedded_ldap.groups.search.pattern}")
    String embedded_ldap_groups_search_pattern;

    /**
     * Authentication implementation.
     */
    @Value("${auth.impl:demo}")
    String authenitcationImplementation;

    @Value("${role.user:sar-user}")
    private String roleUser;

    @Value("${role.superuser:sar-superuser}")
    private String roleSuperuser;

    @Value("${role.admin:sar-admin}")
    private String roleAdmin;

    @Value("${demo.user:user}")
    private String demoUser;

    @Value("${demo.user.password:userPass}")
    private String demoUserPassword;

    @Value("${demo.superuser:superuser}")
    private String demoSuperuser;

    @Value("${demo.superuser.password:superuserPass}")
    private String demoSuperuserPassword;

    @Value("${demo.admin:admin}")
    private String demoAdmin;

    @Value("${demo.admin.password:adminPass}")
    private String demoAdminPassword;

    @Value("${demo.readOnly:johndoe}")
    private String demoReadOnly;

    @Value("${demo.readOnly.password:1234}")
    private String demoReadOnlyPassword;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.authorizeRequests().anyRequest().authenticated();
        http.addFilterBefore(new SessionFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    public void configure(WebSecurity web) {
        // The below lists exceptions for authentication.
        web.ignoring().antMatchers(HttpMethod.GET, "/**");
        web.ignoring().antMatchers(HttpMethod.POST, "/**/login*");
    }


    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        switch(authenitcationImplementation){
            case "ad":
                ActiveDirectoryLdapAuthenticationProvider adProvider = new ActiveDirectoryLdapAuthenticationProvider(ad_domain, ad_url);
                adProvider.setConvertSubErrorCodesToExceptions(true);
                adProvider.setUseAuthenticationRequestCredentials(true);
                auth.authenticationProvider(adProvider);
                break;
            case "ldap":
                DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(ldap_url);
                if (ldap_manager_dn != null && !ldap_manager_dn.isEmpty() && ldap_manager_password != null && !ldap_manager_password.isEmpty()) {
                    contextSource.setUserDn(ldap_manager_dn);
                    contextSource.setPassword(ldap_manager_password);
                }
                contextSource.setBase(ldap_base_dn);
                contextSource.afterPropertiesSet();

                DefaultLdapAuthoritiesPopulator myAuthPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, ldap_groups_search_base);
                myAuthPopulator.setGroupSearchFilter(ldap_groups_search_pattern);
                myAuthPopulator.setSearchSubtree(true);
                myAuthPopulator.setIgnorePartialResultException(true);

                LdapAuthenticationProviderConfigurer configurer = auth.ldapAuthentication()
                        .ldapAuthoritiesPopulator(myAuthPopulator);
                if (ldap_user_dn_pattern != null && !ldap_user_dn_pattern.isEmpty()) {
                    configurer.userDnPatterns(ldap_user_dn_pattern);
                }
                if (ldap_user_search_filter != null && !ldap_user_search_filter.isEmpty()) {
                    configurer.userSearchFilter(ldap_user_search_filter);
                }
                if (ldap_user_search_base != null && !ldap_user_search_base.isEmpty()) {
                    configurer.userSearchBase(ldap_user_search_base);
                }
                //configurer.authoritiesMapper(new LDAPAuthoritiesMapper());
                configurer.contextSource(contextSource);
                break;
            case "ldap_embedded":
                contextSource = new DefaultSpringSecurityContextSource(embedded_ldap_url);
                contextSource.afterPropertiesSet();

                myAuthPopulator
                        = new DefaultLdapAuthoritiesPopulator(contextSource, embedded_ldap_groups_search_base);
                myAuthPopulator.setGroupSearchFilter(embedded_ldap_groups_search_pattern);
                myAuthPopulator.setSearchSubtree(true);
                myAuthPopulator.setIgnorePartialResultException(true);

                auth.ldapAuthentication()
                        .userDnPatterns(embedded_ldap_user_dn_pattern)
                        .ldapAuthoritiesPopulator(myAuthPopulator)
                        .groupSearchBase("ou=Group")
                        .contextSource(contextSource);
                break;
            case "demo":
                auth.inMemoryAuthentication()
                        .withUser(demoAdmin).password(encoder().encode(demoAdminPassword)).roles(roleAdmin()).and()
                        .withUser(demoUser).password(encoder().encode(demoUserPassword)).roles(roleUser()).and()
                        .withUser(demoSuperuser).password(encoder().encode(demoSuperuserPassword)).roles(roleSuperuser());
                break;
            default:
                Logger.getLogger(WebSecurityConfig.class.getName())
                        .log(Level.SEVERE, "Authentication Implementation \"" + authenitcationImplementation + "\" not supported");
                throw new IllegalArgumentException("Authentication Implementation \"" + authenitcationImplementation + "\" not supported");
        }
    }

    @Bean
    public PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        try {
            return super.authenticationManager();
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    @Bean
    @Scope("singleton")
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    public String roleUser(){
        return roleUser.toUpperCase();
    }

    @Bean
    public String roleSuperuser(){
        return roleSuperuser.toUpperCase();
    }

    @Bean
    public String roleAdmin(){
        return roleAdmin.toUpperCase();
    }

    @Bean("demoUser")
    public String demoUser(){
        return demoUser;
    }

    @Bean("demoUserPassword")
    public String demoUserPassword(){
        return demoUserPassword;
    }

    @Bean("demoSuperuser")
    public String demoSuperuser(){
        return demoSuperuser;
    }

    @Bean("demoSuperuserPassword")
    public String demoSuperuserPassword(){
        return demoSuperuserPassword;
    }

    @Bean("demoAdmin")
    public String demoAdmin(){
        return demoAdmin;
    }

    @Bean("demoAdminPassword")
    public String demoAdminPassword(){
        return demoAdminPassword;
    }

    @Bean("demoReadOnly")
    public String demoReadOnly(){
        return demoReadOnly;
    }

    @Bean("demoReadOnlyPassword")
    public String demoReadOnlyPassword(){
        return demoReadOnlyPassword;
    }


    /**
     * Configures role hierarchy, i.e. user -> superuser -> admin. Do not remove this {@link Bean}!
     * <p>
     * <h2>NOTE!</h2>
     * Some Spring Security documentation will state that &quot;and&quot; can be used instead of new-line char to
     * separate rule items. But that does NOT work, at least not with the Spring Security version used in this project.
     * </p>
     * @return A {@link RoleHierarchy} object.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_" + roleAdmin.toUpperCase() + " > ROLE_" + roleSuperuser.toUpperCase() + "\n" +
                "ROLE_" + roleSuperuser.toUpperCase() +" > ROLE_" + roleUser.toUpperCase() + "\n" +
                "ROLE_" + roleAdmin.toUpperCase() + " > ROLE_" + roleUser.toUpperCase());
        return hierarchy;
    }
}
