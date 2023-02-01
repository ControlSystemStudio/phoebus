package org.phoebus.applications.eslog;

import java.util.Comparator;
import java.util.logging.Level;

public class SeverityComparator implements Comparator<String>
{
    @Override
    public int compare(String sev1, String sev2)
    {
        Level l1;
        try
        {
            l1 = Level.parse(sev1);
        }
        catch (IllegalArgumentException ex)
        {
            l1 = Level.OFF;
        }
        Level l2;
        try
        {
            l2 = Level.parse(sev2);
        }
        catch (IllegalArgumentException ex)
        {
            l2 = Level.OFF;
        }
        return (l2.intValue() - l1.intValue());
    }
}
