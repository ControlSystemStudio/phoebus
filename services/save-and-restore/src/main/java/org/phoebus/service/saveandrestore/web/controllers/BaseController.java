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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.SnapshotNotFoundException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base controller that should be extended to make sure exceptions are handled
 * properly, i.e. make the service return suitable HTTP status codes.
 * @author georgweiss
 * Created 23 Nov 2018
 */
@RestController
@SuppressWarnings("unused")
public abstract class BaseController {
	
	public static final String JSON = "application/json";
	
	private final Logger logger = Logger.getLogger(BaseController.class.getName());


	/**
	 * Intercepts {@link SnapshotNotFoundException} and triggers a {@link HttpStatus#NOT_FOUND}.
	 * @param req The servlet request
	 * @param exception The exception to intercept
	 * @return A {@link ResponseEntity} carrying the underlying exception message.
	 */
	@ExceptionHandler(SnapshotNotFoundException.class)
	public ResponseEntity<String> handleSnapshotNotFoundException(HttpServletRequest req,
			SnapshotNotFoundException exception) {
		log(exception);
		return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
	}
	
	/**
	 * Intercepts {@link IllegalArgumentException} and triggers a {@link HttpStatus#BAD_REQUEST}.
	 * @param req The servlet request
	 * @param exception The exception to intercept
	 * @return A {@link ResponseEntity} carrying the underlying exception message.
	 */
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleIllegalArgumentException(HttpServletRequest req,
			IllegalArgumentException exception) {
		log(exception);
		return new ResponseEntity<>(exception.getMessage(), HttpStatus.BAD_REQUEST);
	}
	
	/**
	 * Intercepts {@link NodeNotFoundException} and triggers a {@link HttpStatus#NOT_FOUND}.
	 * @param req The {@link HttpServlet} request
	 * @param exception The exception to intercept
	 * @return A {@link ResponseEntity} carrying the underlying exception message.
	 */
	@ExceptionHandler(NodeNotFoundException.class)
	public ResponseEntity<String> handleNodeNotFoundException(HttpServletRequest req,
			NodeNotFoundException exception) {
		log(exception);
		return new ResponseEntity<>(exception.getMessage(), HttpStatus.NOT_FOUND);
	}
	
	private void log(Throwable throwable) {
		logger.log(Level.INFO, "Intercepted " + throwable.getClass().getName(), throwable);
	}
}
