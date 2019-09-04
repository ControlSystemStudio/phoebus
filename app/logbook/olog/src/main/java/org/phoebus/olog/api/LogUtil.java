/**
 * 
 */
package org.phoebus.olog.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.phoebus.logbook.LogEntry;


/**
 * @author Eric Beryman taken from shroffk
 * 
 */
public class LogUtil {

    /**
     * This class is not meant to be instantiated or extended
     */
    private LogUtil() {

    }

    static Collection<LogEntry> toLogs(XmlLogs xmlLogs) {
        Collection<LogEntry> logs = new HashSet<LogEntry>();
        for (XmlLog xmlLog : xmlLogs.getLogs()) {
            logs.add(new OlogLog(xmlLog));
        }
        return logs;
    }

    public static Collection<String> getLogDescriptions(Collection<OlogLog> logs) {
        Collection<String> logDescriptions = new ArrayList<String>();
        for (OlogLog log : logs) {
            logDescriptions.add(log.getDescription());
        }
        return logDescriptions;
    }

}