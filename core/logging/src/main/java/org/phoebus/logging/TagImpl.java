package org.phoebus.logging;

/**
 * A default implementation for {@link Tag}
 * @author Kunal Shroff
 *
 */
public class TagImpl implements Tag{
    private final String name;
    private final String state;

    public TagImpl(String name, String state) {
        this.name = name;
        this.state = state;
    }
    
    public TagImpl(String name) {
        this.name = name;
        this.state = "";
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
        result = prime * result + ((state == null) ? 0 : state.hashCode());
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
        TagImpl other = (TagImpl) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (state == null) {
            if (other.state != null)
                return false;
        } else if (!state.equals(other.state))
            return false;
        return true;
    }


}
