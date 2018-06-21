/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.authorization;

/** Authorization support
 *  @author Evan Smith
 */
public interface Authorization
{
    /** Check if current user is authorized to do something
     *  @param authorization Name of the authorization
     *  @return <code>true</code> if user holds that authorization
     */
    public boolean hasAuthorization(final String authorization);
}
