/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Version info
 *
 *  <p>Version number defined as
 *  "Major.Minor.Patch",
 *  for example "2.0.0".
 *
 *  <p>Fundamentally the handling of version differences
 *  is left to each widget.
 *  When reading an existing configuration file,
 *  the {@link WidgetConfigurator} can check for the
 *  version that is read and then translate older
 *  input as desired.
 *
 *  <p>Suggested use of the version number components:
 *
 *  <p>Major: Incremented major release indicates
 *  significant new functionality. A display created with
 *  this new version may not work with an older release.
 *
 *  <p>Major: Incremented major release indicates
 *  some new or improved functionality, for example
 *  a new auxiliary property in a widget,
 *  not breaking existing code.
 *  A display file created with this version will still
 *  for the most part work in an older version.
 *
 *  <p>Patch: Bug fixes.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Version implements Comparable<Version>
{
    // Original implementation used org.osgi.framework.Version.
    // Was hoping to use java.lang.Runtime.Version,
    // but it requires the patch level (which it calls "security")
    // to be non-zero, which would not allow loading
    // versions like "1.0.0" or "2.0.0" from existing *.opi or *.bob files.
    private static final Pattern VERSION_PATTERN = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)");

    // Some older displays used a shorter "1.0" format without patch level
    private static final Pattern SHORT_VERSION_PATTERN = Pattern.compile("([0-9]+)\\.([0-9]+)");

    private final int major, minor, patch;

    /** Parse version from text
     *  @param version "Major.Minor.Patch" type of text
     *  @return {@link Version}
     *  @throws IllegalArgumentException on error
     */
    public static Version parse(final String version)
    {
        // First try the long format
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.matches())
            return new Version(Integer.parseInt(matcher.group(1)),
                               Integer.parseInt(matcher.group(2)),
                               Integer.parseInt(matcher.group(3)));

        matcher = SHORT_VERSION_PATTERN.matcher(version);
        if (matcher.matches())
            return new Version(Integer.parseInt(matcher.group(1)),
                               Integer.parseInt(matcher.group(2)),
                               0);
        throw new IllegalArgumentException("Invalid version string '" + version + "'");
    }

    /** Construct version
     *  @param major
     *  @param minor
     *  @param patch
     */
    public Version(final int major, final int minor, final int patch)
    {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /** @return Major version identifier */
    public int getMajor()
    {
        return major;
    }

    /** @return Minor version identifier */
    public int getMinor()
    {
        return minor;
    }

    /** @return Version patch level */
    public int getPatch()
    {
        return patch;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + major;
        result = prime * result + minor;
        result = prime * result + patch;
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof Version))
            return false;
        final Version other = (Version) obj;
        return major == other.major &&
               minor == other.minor &&
               patch == other.patch;
    }

    @Override
    public int compareTo(final Version other)
    {
        if (major != other.major)
            return major - other.major;
        if (minor != other.minor)
            return minor - other.minor;
        return patch - other.patch;
    }

    @Override
    public String toString()
    {
        return major + "." + minor + "." + patch;
    }
}
