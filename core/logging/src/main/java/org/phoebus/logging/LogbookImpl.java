package org.phoebus.logging;

/**
 * A default implementation of {@link Logbook}
 * 
 * @author Kunal Shroff
 *
 */
public class LogbookImpl implements Logbook {

    private final String name;
    private final String owner;

    public LogbookImpl(String name, String owner) {
        super();
        this.name = name;
        this.owner = owner;
    }
    
    public LogbookImpl(String name) {
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
