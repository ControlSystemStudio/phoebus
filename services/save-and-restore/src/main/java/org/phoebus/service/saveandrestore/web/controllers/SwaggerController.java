/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.service.saveandrestore.web.controllers;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author georgweiss
 * Created 3 Oct 2018
 */
@Profile("production")
@RestController
public class SwaggerController {

	/**
	 * Ensures that when the service is launched with profile "production", the
	 * Swagger UI (swagger-ui.html) is disabled and instead returns a HTTP status 404.
	 */
	@GetMapping(value = "swagger-ui.html")
	@ResponseStatus(HttpStatus.NOT_FOUND)
    public void getSwaggerUI(){
		// Purpose of this handler method is to return a 404 in a production environment.
    }
}
