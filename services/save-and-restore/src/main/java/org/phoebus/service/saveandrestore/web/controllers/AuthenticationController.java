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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RestController
public class AuthenticationController extends BaseController {

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Authenticates user.
     *
     * @param userName The user principal name
     * @param password User's password
     * @return A {@link ResponseEntity} carrying a {@link UserData} object if the login was successful,
     * otherwise the body will be <code>null</code>.
     */
    @PostMapping(value = "login")
    public ResponseEntity<UserData> login(@RequestParam(value = "username") String userName,
                                          @RequestParam(value = "password") String password) {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(userName, password);
        try {
            authentication = authenticationManager.authenticate(authentication);
        } catch (AuthenticationException e) {
            Logger.getLogger(AuthenticationController.class.getName()).log(Level.WARNING, "Unable to authenticate", e);
            return new ResponseEntity<>(
                    null,
                    HttpStatus.UNAUTHORIZED);
        }
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        return new ResponseEntity<>(
                new UserData(userName, roles),
                HttpStatus.OK);
    }
}
