/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.service.saveandrestore.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Web security configuration instantiated if authorization/authentication is disabled.
 * Despite its name it still needs to define authorization/authentication related values and beans.
 */
@Configuration
@ConditionalOnProperty(name = "auth.impl", havingValue = "none")
@SuppressWarnings("unused")
public class AnonymousWebSecurityConfig {

    @Value("${role.user:sar-user}")
    public String roleUser;

    @Value("${role.admin:sar-admin}")
    public String roleAdmin;

    @Value("${demo.user:user}")
    public String demoUser;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.anonymous();
        return http.build();
    }
}
