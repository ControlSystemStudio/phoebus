/*
 * Copyright (C) 2020 European Spallation Source ERIC.
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
 */

package org.phoebus.service.saveandrestore.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filter that will take care of authenticating requests that come with
 * a basic authentication header.
 * <p>
 * This class should not be instantiated as a bean in the application configuration. If it is, the
 * <code>doFilter()</code> method will be called for each endpoint URI, effectively defeating the purpose of the
 * configuration of ignored URI patterns set up in the Spring Security context, see
 * {@link WebSecurityConfig#configure(WebSecurity)}.
 */
public class SessionFilter extends GenericFilterBean {

    private AuthenticationManager authenticationManager;
    private ObjectMapper objectMapper;

    public SessionFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        objectMapper = new ObjectMapper();
    }

    /**
     * @param request     A {@link ServletRequest}
     * @param response    A {@link ServletResponse}
     * @param filterChain The {@link FilterChain} to which this implementation contributes.
     * @throws IOException      May be thrown by upstream filters.
     * @throws ServletException May be thrown by upstream filters.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        String basicAuthenticationHeader = httpServletRequest.getHeader("Authorization");
        String[] usernameAndPassword = getUsernameAndPassword(basicAuthenticationHeader);
        if (usernameAndPassword == null) {
            SecurityContextHolder.getContext().setAuthentication(null);
        } else {
            Authentication authentication = new UsernamePasswordAuthenticationToken(usernameAndPassword[0],
                    usernameAndPassword[1]);
            try {
                authentication = authenticationManager.authenticate(authentication);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AuthenticationException e) {
                Logger.getLogger(SessionFilter.class.getName())
                        .log(Level.FINE, String.format("User %s not authenticated through authorization header",
                                usernameAndPassword[0]));
            }
        }

        filterChain.doFilter(request, response);
    }

    protected String[] getUsernameAndPassword(String authorization) {
        if (authorization != null && authorization.toLowerCase().startsWith("basic")) {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            // credentials = username:password
            return credentials.split(":", 2);
        }
        return null;
    }
}
