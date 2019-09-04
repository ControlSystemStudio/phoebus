/**
 * 
 */
package org.phoebus.olog.api;

import org.phoebus.logbook.Logbook;

/**
 * @author Eric Berryman taken from shroffk
 * 
 */
public class OlogLogbook implements Logbook{
    private final String name;
    private final String owner;

    /**
     * @param xmlLogbook
     */
    OlogLogbook(XmlLogbook xmlLogbook) {
        this.name = xmlLogbook.getName();
        this.owner = xmlLogbook.getOwner();
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof OlogLogbook))
            return false;
        OlogLogbook other = (OlogLogbook) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
