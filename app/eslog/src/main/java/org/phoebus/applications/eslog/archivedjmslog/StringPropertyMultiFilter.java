package org.phoebus.applications.eslog.archivedjmslog;

import java.util.function.Function;

import org.phoebus.applications.eslog.Activator;

/**
 * A {@link PropertyFilter} matching a string property of a message against a
 * set of fixed values.
 */
public class StringPropertyMultiFilter extends PropertyFilter
{
    private final boolean inverted;
    private final String property;
    private final String[] patterns;

    /**
     * Constructor
     *
     * @param property
     *            Property name.
     * @param patterns
     *            Expected Patterns for the property's value.
     * @param inverted
     *            Match, iff none of the patterns is contained in the property
     *            value.
     */
    public StringPropertyMultiFilter(String property, String[] patterns,
            boolean inverted)
    {
        Activator.checkParameterString(property, "property"); //$NON-NLS-1$
        Activator.checkParameter(patterns, "patterns"); //$NON-NLS-1$
        this.inverted = inverted;
        this.property = property;
        this.patterns = patterns;
    }

    public String[] getPatterns()
    {
        return this.patterns;
    }

    public String getProperty()
    {
        return this.property;
    }

    @Override
    public boolean isInverted()
    {
        return this.inverted;
    }

    @Override
    public boolean match(Function<String, Object> msgInfo)
    {
        Object prop = msgInfo.apply(this.property);
        if (null == prop)
        {
            return !this.inverted;
        }
        if (!(prop instanceof String))
        {
            return false;
        }
        for (String pattern : this.patterns)
        {
            // is inverted => match NOT
            if (((String) prop).contains(pattern) != this.inverted)
            {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        final String operator = this.inverted ? "!=" : "==";
        return String.format("'%s' %s '%s'.", this.property, operator,
                this.patterns);
    }
}
