package org.phoebus.applications.eslog.archivedjmslog;

import java.util.function.Function;

import org.phoebus.applications.eslog.Activator;

/** A filter that can be used on {@link LogMessage}. */
public abstract class PropertyFilter
{
    /**
     * Check if a LogMessage matches the defined filter condition.
     *
     * @param msg
     *            The LogMessage to check.
     * @return {@code true}, iff the message matches the filter condition.
     */
    public boolean match(LogMessage msg)
    {
        Activator.checkParameter(msg, "msg"); //$NON-NLS-1$
        return match(msg::getPropertyValue);
    }

    /**
     * Check if a LogMessage matches the defined filter condition.
     *
     * @param msgInfo
     *            The function to use to access the LogMessage's properties.
     * @return {@code true}, iff the message matches the filter condition.
     */
    public abstract boolean match(Function<String, Object> msgInfo);

    /**
     * Return the inverted property of the filter.
     *
     * The inversion is already considered in the result of
     * {@link PropertyFilter#match(LogMessage)} and
     * {@link PropertyFilter#match(Function).
     *
     * @return The value of the inverted property.
     */
    public boolean isInverted()
    {
        return false;
    }
}
