/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.web.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.phoebus.applications.saveandrestore.model.LoginCredentials;
import org.phoebus.applications.saveandrestore.model.UserData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller class for user authentication endpoints.
 */
@SuppressWarnings("unused")
@RestController
public class AuthenticationController extends BaseController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private String roleServerRestore;

    /**
     * Authenticates user.
     *
     * @param loginCredentials User's credentials
     * @return A {@link ResponseEntity} indicating the outcome, e.g. OK (200) or UNAUTHORIZED (401)
     */
    @PostMapping(value = "login")
    public ResponseEntity<UserData> login(@RequestBody LoginCredentials loginCredentials) {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(loginCredentials.username(), loginCredentials.password());
        try {
            authentication = authenticationManager.authenticate(authentication);
        } catch (AuthenticationException e) {
            Logger.getLogger(AuthenticationController.class.getName()).log(Level.WARNING, "Unable to authenticate user " + loginCredentials.username(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        return new ResponseEntity<>(
                new UserData(loginCredentials.username(), roles),
                HttpStatus.OK);
    }


    /**
     * Checks if a user is authorized to do server side restore. Note that this must be a POST endpoint
     * as GET endpoints do not provide information on user authorization.
     * @param request A {@link HttpServletRequest} object
     * @return <code>true</code> if authorized, i.e. has the associated role.
     */
    @PostMapping(value = "server-restore-authorized")
    public boolean authorizedForServerRestore(HttpServletRequest request) {
        return request.isUserInRole("ROLE_" + roleServerRestore.toUpperCase());
    }
}
