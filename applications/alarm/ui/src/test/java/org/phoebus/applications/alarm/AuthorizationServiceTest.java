package org.phoebus.applications.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.phoebus.applications.alarm.ui.authorization.FileBasedAuthorizationService;

public class AuthorizationServiceTest
{

    /* The authorization.conf file is assumed to be as follows for these tests.
     
            # Anybody can acknowledge alarms
            alarm_ack=.*
            # Specific users may configure alarms
            alarm_config = 3tl, 8hm, 8w4, 9pj, ac7, amp, c2y, cmp, fd4, fg2, gmc, ky9, kasemir, mkp, nnl, txg, udn, xgc
            # Full authorization.
            FULL = 1es
     */
    @SuppressWarnings("unused")
    @Test
    public void testAuthorizationService()
    {
        FileBasedAuthorizationService as = new FileBasedAuthorizationService();
        
        // Acknowledge only.
        as.setUser("blah");
        assertEquals("blah", as.getUser());
        assertTrue(as.hasAuthorization("alarm_ack"));
        assertFalse(as.hasAuthorization("alarm_config"));
        
        as.setUser("3tl");
        assertEquals("3tl", as.getUser());

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("8hm");
        assertEquals("8hm", as.getUser());

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("8w4");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("9pj");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("ac7");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("amp");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("c2y");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("cmp");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("fd4");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("fg2");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("gmc");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("ky9");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("kasemir");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("mkp");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("nnl");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("txg");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("udn");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        as.setUser("xgc");        
        
        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
        
        // FULL
        as.setUser("1es");

        assertTrue(as.hasAuthorization("alarm_ack"));
        assertTrue(as.hasAuthorization("alarm_config"));
    }
}
