/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.net.InetAddress;
import java.util.logging.Level;

import org.epics.pva.acf.AccessConfig;
import org.epics.pva.acf.AccessConfigParser;
import org.epics.pva.acf.AccessSecurityGroup;
import org.epics.pva.common.PVAAuth;
import org.epics.pva.pvlist.PVListFile;

import static org.epics.pva.PVASettings.logger;

/** Determine authorization of a client connected to this server based on pvlist and acf files
 *  @author Kay Kasemir
 */
public class FileBasedServerAuthorization extends ServerAuthorization
{
    private final PVListFile pvlist;
    private final AccessConfig access;

    /** @param pvlist_file <code>*.pvlist</code> file name
     *  @param acf_file <code>*.acf</code> file name
     */
    public FileBasedServerAuthorization(final String pvlist_file, final String acf_file) throws Exception
    {
        logger.log(Level.FINE, "FileBasedServerAuthorization using " + pvlist_file + " and " + acf_file);
        pvlist = new PVListFile(pvlist_file);
        access = new AccessConfigParser().parse(acf_file);
    }

    @Override
    public boolean allowSearch(final String pv_name, final InetAddress host)
    {
        final boolean allowed = pvlist.getAccess(pv_name, host) != null;
        if (! allowed)
            logger.log(Level.FINER, () -> "*.pvlist blocks client on " + host + " from accessing '" + pv_name + "'");
        return allowed;
    }

    @Override
    public boolean hasWriteAccess(final String pv_name, final ClientAuthentication client_auth)
    {
        if (client_auth.getType() == PVAAuth.anonymous)
        {
            logger.log(Level.FINER, () -> client_auth + " write access refused because anonymous");
            return false;
        }

        final String asg_name = pvlist.getAccess(pv_name, client_auth.getHost());
        if (asg_name == null)
        {   // Should not get here because pvlist would already block the search reply...
            logger.log(Level.FINER, () -> "Write access to '" + pv_name + "' refused by pvlist");
            return false;
        }

        final AccessSecurityGroup asg = access.getAccessGroup(asg_name);
        if (asg == null)
        {
            logger.log(Level.FINER, () -> "Write access to '" + pv_name + "' refused because of unknown ASG(" + asg_name + ")");
            return false;
        }

        final boolean write = asg.mayWrite(client_auth.getUser(), client_auth.getHost());
        logger.log(Level.FINER, () -> client_auth + (write ? " has write access" : " has NO write access") + " for ASG(" + asg_name + ")");
        return write;
    }
}
