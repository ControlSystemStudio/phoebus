package org.phoebus.applications.alarm.messages;

import java.time.Instant;
import java.util.Arrays;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class AlarmMessageTest {

    @Test
    public void simple() {
        try {
            AlarmMessage message = new AlarmMessage();
            message.setValue("100.0");
            message.setGuidance(Arrays.asList(new AlarmDetail("guidance", "open the instructions")));
            message.setMsgTime(Instant.now());

            System.out.println(message.toJson());

            message.setConfig(false);
            System.out.println(message.toJson());

        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
