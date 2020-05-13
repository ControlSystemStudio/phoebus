/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.security.authorization;

/** Authorization support
 *
 *  <p>'authorization' describes something that a user may
 *  be permitted to do.
 *  Each application module can define its own authorizations.
 *  For example, the alarm UI uses the 'alarm_ack' authorization
 *  to determine if the current user may acknowledge alarms.
 *
 *  @author Evan Smith
 *  @author Tanvi Ashwarya
 *  @author Kay Kasemir
 */
public interface Authorization
{
    /** Check if an authorization has been defined,
     *  i.e. if the authorization settings contain
     *  a specific entry for this authorization.
     *
     *  <p>Note that authorizations that are not specifically
     *  provided are not categorically denied.
     *  Instead, they fall back to the "FULL" authorization.
     *
     *  @param authorization Name of the authorization rule
     *  @return <code>true</code> if auth rule exists
     */
    public boolean isAuthorizationDefined(final String authorization);

    /** Check if current user is authorized to do something
     *  @param authorization Name of the authorization
     *  @return <code>true</code> if user holds that authorization
     */
    public boolean hasAuthorization(final String authorization);
}
