package org.phoebus.alarm.logging;

/**
 *
 * A bean representing an alarm command message
 * 
 * @author Kunal Shroff
 *
 */
public class AlarmCommandMessage {

    private String user;
    private String host;
    private String command;

    public AlarmCommandMessage() {
        super();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
