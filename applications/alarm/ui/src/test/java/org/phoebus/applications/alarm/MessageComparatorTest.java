package org.phoebus.applications.alarm;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.phoebus.applications.alarm.ui.annunciator.MessageComparator;

public class MessageComparatorTest
{
    @Test
    public void test()
    {
        List<String> messages = Arrays.asList("MAJOR Alarm: blah blah", "MINOR Alarm: blah blah", "UNDEFINED Alarm: blah blah", "INVALID Alarm: blah blah");
        MessageComparator mc = new MessageComparator();
        messages.sort(mc);
        for (String message : messages)
            System.out.println(message);
    }
}
