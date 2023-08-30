package org.phoebus.logbook;

/**
 * A default implementation for {@link Tag}
 * 
 * @author Kunal Shroff
 *
 */
public class TagImpl implements Tag {
    private final String name;
    private final String state;

    private TagImpl(String name, String state) {
        this.name = name;
        this.state = state;
    }

    private TagImpl(String name) {
        this.name = name;
        this.state = "Active";
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    /**
     * Create a new instance of TagImpl with tagName name
     * 
     * @param name - tag name
     * @return {@link Tag} with provided name
     */
    public static final Tag of(String name) {
        return new TagImpl(name);
    }

    /**
     * Create a new instance of the TagImpl with tagName name and tagState state
     * 
     * @param name - tag name
     * @param state - tag state
     * @return {@link Tag} with given name and state
     */
    public static final Tag of(String name, String state) {
        return new TagImpl(name, state);
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
