/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.security.authorization;

import java.util.Collections;
import java.util.Set;

/** Authorizations that a user has
 *
 *  <p>Each authorization is a string.
 *  The meaning is defined by the application that uses it.
 *  For example, "alarm_config" may be an authorization that
 *  the alarm system requires to permit changes.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Authorizations
{
    /** Special authorization that allows subject to do anything */
    final public static String FULL = "FULL";

    /** What the user is allowed to do */
    final private Set<String> authorizations;

    /** Initialize
     *  @param authorizations Authorizations held by the user
     */
    public Authorizations(final Set<String> authorizations)
    {
        this.authorizations = Collections.unmodifiableSet(authorizations);
    }

    /** @return Authorizations held by a user */
    public Set<String> getAuthorizations()
    {
        return authorizations;
    }

    /** Check if user may do something
     *
     *  @param authorization Authorization to check
     *  @return <code>true</code> if user has authorization
     */
    public boolean haveAuthorization(final String authorization)
    {
        return authorizations.contains(authorization)  ||  authorizations.contains(FULL);
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return "Authorizations: " + authorizations;
    }
}