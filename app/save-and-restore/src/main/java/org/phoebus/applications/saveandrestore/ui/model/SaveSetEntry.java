package org.phoebus.applications.saveandrestore.ui.model;

import java.io.Serializable;
import java.util.Objects;

import lombok.Builder;
import lombok.Data;

/**
 *
 * <code>SaveSetEntry</code> represents a single entry in the save set. It provides all properties of a single PV that
 * are stored in the save set file: the name of the PV, the name of the readback pv, delta used for comparison of
 * values, and the flag indicating if the PV is readonly or readable and writable.
 *
 * @see Threshold
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
@Builder
@Data
public class SaveSetEntry implements Serializable {

    private static final long serialVersionUID = -4899009360011799916L;

    private String pvName;
    
    @Builder.Default
    private EpicsProvider epicsProvider = EpicsProvider.CA;

    
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(SaveSetEntry.class, pvName);
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
        if (obj == null || getClass() != obj.getClass())
            return false;
        SaveSetEntry other = (SaveSetEntry) obj;
        return Objects.equals(pvName, other.pvName);
    }

 
    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return pvName + "(" + epicsProvider.toString() + ")";
    }

}
