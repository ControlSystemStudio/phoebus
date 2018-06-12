package org.phoebus.applications.alarm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.phoebus.applications.alarm.ui.annunciator.MessageComparator;

public class MessageComparatorTest
{
    @Test
    public void test()
    {
        List<String> messages = Arrays.asList("MAJOR Alarm: blah blah", "MINOR Alarm: blah blah", "UNDEFINED Alarm: blah blah", "INVALID Alarm: blah blah", "INFO INFO INFO", "BLAH BLAH blah...",
                                               "MAJOR Alarm: blah", "MINOR Alarm: blah");
        MessageComparator mc = new MessageComparator();
        messages.sort(mc);
        /*
        for (String message : messages)
            System.out.println(message);
        */
        assertEquals(0, messages.indexOf("UNDEFINED Alarm: blah blah"));
        assertEquals(1, messages.indexOf("INVALID Alarm: blah blah"));
        assertEquals(2, messages.indexOf("MAJOR Alarm: blah blah"));
        assertEquals(3, messages.indexOf("MAJOR Alarm: blah"));
        assertEquals(4, messages.indexOf("MINOR Alarm: blah blah"));
        assertEquals(5, messages.indexOf("MINOR Alarm: blah"));
        assertEquals(6, messages.indexOf("INFO INFO INFO"));
        assertEquals(7, messages.indexOf("BLAH BLAH blah..."));
    }
}
