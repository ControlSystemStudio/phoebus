/*
 * Copyright (C) 2024 European Spallation Source ERIC.
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

package org.phoebus.service.saveandrestore.web.controllers;

import org.phoebus.applications.saveandrestore.model.LoginCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for user authentication endpoints.
 */
@SuppressWarnings("unused")
@RestController
public class AuthenticationController extends BaseController {

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Authenticates user.
     *
     * @param loginCredentials User's credentials
     * @return A {@link ResponseEntity} indicating the outcome, e.g. OK (200) or UNAUTHORIZED (401)
     */
    @PostMapping(value = "login")
    public ResponseEntity<Void> login(@RequestBody LoginCredentials loginCredentials) {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(loginCredentials.username(), loginCredentials.password());
        try {
            authenticationManager.authenticate(authentication);
        } catch (AuthenticationException e) {
            Logger.getLogger(AuthenticationController.class.getName()).log(Level.WARNING, "Unable to authenticate user " + loginCredentials.username());
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
