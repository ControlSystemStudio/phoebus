package org.phoebus.logbook;

/**
 * A default implementation of {@link Logbook}
 * 
 * @author Kunal Shroff
 *
 */
public class LogbookImpl implements Logbook {

    private final String name;
    private final String owner;

    private LogbookImpl(String name, String owner) {
        super();
        this.name = name;
        this.owner = owner;
    }

    private LogbookImpl(String name) {
        super();
        this.name = name;
        this.owner = "";
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    /**
     * Create a new instance of the default implementation of the {@link Logbook} interface with the given name
     * 
     * @param name - logbook name
     * @return {@link Logbook} with the given name
     */
    public static Logbook of(String name) {
        return new LogbookImpl(name);
    }

    /**
     * Create a new instance of the default implementation of the {@link Logbook} interface with the given name and owner
     * @param name - logbook name
     * @param owner - logbook owner
     * @return {@link Logbook} with given name and owner
     */
    public static Logbook of(String name, String owner) {
        return new LogbookImpl(name, owner);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LogbookImpl other = (LogbookImpl) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
