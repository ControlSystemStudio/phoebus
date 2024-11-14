/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.olog.es.authentication;

/**
 * Wrapper around user's credentials
 *
 * @param username Self-explanatory
 * @param password Self-explanatory
 */
public record LoginCredentials(String username, String password) {
}
