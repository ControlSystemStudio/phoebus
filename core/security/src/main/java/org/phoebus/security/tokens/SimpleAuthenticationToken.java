package org.phoebus.security.tokens;

/**
 * A simple authentication token with a username and password
 * 
 * @author Kunal Shroff
 *
 */
public class SimpleAuthenticationToken {

    private final String username;
    private final String password;

    /**
     * Construct a simple authentication token with the given username and password
     * @param username
     * @param password
     */
    public SimpleAuthenticationToken(String username, String password) {
        super();
        this.username = username;
        this.password = password;
    }

    /**
     * get the username for this token
     * @return String username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password for this token
     * @return String password
     */
    public String getPassword() {
        return password;
    }

}
