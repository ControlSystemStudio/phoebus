package org.phoebus.logbook.olog.ui.write;

import org.junit.jupiter.api.Test;
import org.phoebus.logbook.*;
import org.phoebus.olog.es.api.model.OlogLog;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LogEntryUtilsTest {

    @Test
    public void testCreateLogEntryFromList(){

        OlogLog ologEntry01 = new OlogLog();
        ologEntry01.setTitle("Title01");
        ologEntry01.setId(1L);

        OlogLog ologEntry02 = new OlogLog();
        ologEntry02.setTitle("Title02");
        ologEntry02.setId(2L);


        OlogLog ologEntry03 = new OlogLog();
        ologEntry03.setTitle("Title03");
        ologEntry03.setId(3L);

        LogEntry testListLog = LogEntryUtils.createLogEntryFromList("someURL", List.of(ologEntry01, ologEntry02, ologEntry03));

        assertEquals("\n[Title01](someURL1)\n\n\n[Title02](someURL2)\n\n\n[Title03](someURL3)\n\n", testListLog.getSource());
    }
}

