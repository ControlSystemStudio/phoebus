package org.phoebus.olog.api;

import org.phoebus.logbook.Tag;

public class OlogTag implements Tag {
    private final String name;
    private final String state;

    /**
     * @author berryman from shroffk
     *
     */

    OlogTag(XmlTag xml) {
        this.name = xml.getName();
        this.state = xml.getState();
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
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
        if (!(obj instanceof OlogTag))
            return false;
        OlogTag other = (OlogTag) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
