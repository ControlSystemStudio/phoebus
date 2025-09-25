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

package org.phoebus.applications.credentialsmanagement;

import org.phoebus.framework.nls.NLS;

public class Messages {

    public static String DisplayName;
    public static String ErrorDialogTitle;
    public static String ErrorDialogBody;
    public static String LoginButtonText;
    public static String LogoutButtonText;
    public static String SecureStoreErrorTitle;
    public static String SecureStoreErrorBody;
    public static String Title;
    public static String ServiceConnectionFailure;
    public static String UnknownError;
    public static String UserNotAuthenticated;

    static
    {
        NLS.initializeMessages(Messages.class);
    }

    private Messages()
    {
        // Prevent instantiation
    }
}
