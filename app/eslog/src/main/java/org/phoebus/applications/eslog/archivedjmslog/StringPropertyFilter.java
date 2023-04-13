package org.phoebus.applications.eslog.archivedjmslog;

import java.util.function.Function;

import org.phoebus.applications.eslog.Activator;

/**
 * A {@link PropertyFilter} matching a string property of a message against a
 * fixed value.
 */
public class StringPropertyFilter extends PropertyFilter
{
    private final boolean inverted;
    private final String property;
    private final String pattern;

    /**
     * Constructor.
     *
     * @param property
     *            Property name
     * @param pattern
     *            Pattern for the property's value
     * @param inverted
     *            Invert the match result.
     */
    public StringPropertyFilter(String property, String pattern,
            boolean inverted)
    {
        Activator.checkParameterString(property, "property"); //$NON-NLS-1$
        Activator.checkParameter(pattern, "pattern"); //$NON-NLS-1$
        this.inverted = inverted;
        this.property = property;
        this.pattern = pattern;
    }

    public String getPattern()
    {
        return this.pattern;
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
        return ((String) prop).contains(this.pattern) != this.inverted;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        final String operator = this.inverted ? "!=" : "==";
        return String.format("'%s' %s '%s'.", this.property, operator,
                this.pattern);
    }
}
