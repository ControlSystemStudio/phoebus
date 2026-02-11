/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.pvlist;

import java.net.InetAddress;
import java.util.regex.Pattern;

import org.epics.pva.common.Network;

/** Rule that allows or denies access by PV name or client host */
abstract class PVListFileRule
{
    public static final String DEFAULT = "DEFAULT";

    protected final Pattern pv_pattern;

    private PVListFileRule(final String pv_pattern)
    {
        this.pv_pattern = Pattern.compile(pv_pattern);
    }

    /** Rule that allows access to name pattern, providing access security group for the PV */
    static class AllowingRule extends PVListFileRule
    {
        protected final String access_security_group;

        AllowingRule(final String pv_pattern, final String access_security_group)
        {
            super(pv_pattern);
            this.access_security_group = access_security_group;
        }

        /** Does this rule allow access?
         *  @param pv_name PV name to check
         *  @return Access security group or <code>null</code>
         */
        public String getAccessSecurityGroup(final String pv_name)
        {
            if (pv_pattern.matcher(pv_name).matches())
                return access_security_group;
            return null;
        }

        @Override
        public String toString()
        {
            return String.format("%-30s    ALLOW %s", pv_pattern.pattern(), access_security_group);
        }
    }

    /** Rule that blocks access to name pattern and optionally client host address */
    static class DenyingRule extends PVListFileRule
    {
        protected final InetAddress address;

        DenyingRule(final String pv_pattern, final InetAddress address)
        {
            super(pv_pattern);
            this.address = address;
        }

        /** Does this rule deny access?
         *  @param pv_name PV name to check
         *  @param address Client's address
         *  @return Is this client denied access to the PV?
         */
        public boolean isDenied(final String pv_name, final InetAddress address)
        {
            return pv_pattern.matcher(pv_name).matches() &&
                   (this.address == null || this.address.equals(address));
        }

        @Override
        public String toString()
        {
            String result = String.format("%-30s    DENY", pv_pattern.pattern());
            if (address != null)
                result += " FROM " + Network.format(address);
            return result;
        }
    }

    /** Parse pvlist file line
     *
     *  Will perform a DNS lookup when "DENY FROM ..." is
     *  used with host names; fast when used with "123.45.67.100".
     *
     *  @param line "pattern  ALLOW/DENY ..."
     *  @return {@link AllowingRule} or {@link DenyingRule}
     *  @throws Exception on error, including name lookup for "DENY FROM ..."
     */
    static PVListFileRule create(final String line) throws Exception
    {
        final String[] parts = line.split("\\s+");
        if (parts.length < 2)
            throw new Exception("Missing {pattern} DENY|ALLOW");

        final String pv_pattern = parts[0];
        if ("ALLOW".equalsIgnoreCase(parts[1]))
        {
            if (parts.length >= 3)
                return new AllowingRule(pv_pattern, parts[2]);
            return new AllowingRule(pv_pattern, DEFAULT);
        }
        else if ("DENY".equalsIgnoreCase(parts[1]))
        {
            InetAddress address = null;
            if (parts.length >= 4  &&  "FROM".equals(parts[2]))
                address = InetAddress.getByName(parts[3]);
            return new DenyingRule(pv_pattern, address);
        }
        else
            throw new Exception("Expect ALLOW or DENY");
    }
}
