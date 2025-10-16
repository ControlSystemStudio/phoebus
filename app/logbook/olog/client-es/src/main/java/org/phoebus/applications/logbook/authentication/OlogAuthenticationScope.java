/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.logbook.authentication;

import org.phoebus.security.tokens.AuthenticationScope;

public class OlogAuthenticationScope implements AuthenticationScope {

    @Override
    public String getScope() {
        return "logbook";
    }

    @Override
    public String getDisplayName() {
        return "Logbook";
    }
}
