/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.authentication;

import org.phoebus.security.tokens.AuthenticationScope;

public class SaveAndRestoreAuthenticationScope implements AuthenticationScope {
    @Override
    public String getScope() {
        return "save-and-restore";
    }

    @Override
    public String getDisplayName() {
        return "Save and Restore";
    }
}
