/**
 * Copyright (C) 2026 European Spallation Source ERIC.
 */
package org.phoebus.service.saveandrestore.web.controllers;

import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.SnapshotNotFoundException;
import org.phoebus.service.saveandrestore.persistence.config.IndexConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base controller that should be extended to make sure exceptions are handled
 * properly, i.e. make the service return suitable HTTP status codes.
 *
 * @author georgweiss
 * Created 23 Nov 2018
 */
@RestController
@SuppressWarnings("unused")
public abstract class BaseController extends IndexConfig {

    protected static final String JSON = "application/json";

    private final Logger logger = Logger.getLogger(BaseController.class.getName());

    @Autowired
    public long connectionTimeout;

    /**
     * Identity of the admin role
     */
    @Autowired
    public String roleAdmin; // This MUST be public!!!

    /**
     * Identity of regular user.
     */
    @Autowired
    public String roleUser; // This MUST be public!!!


    /**
     * Intercepts {@link SnapshotNotFoundException} and triggers a {@link HttpStatus#NOT_FOUND}.
     *
     * @param req       The servlet request
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
     *
     * @param req       The servlet request
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
     *
     * @param req       The {@link HttpServlet} request
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
