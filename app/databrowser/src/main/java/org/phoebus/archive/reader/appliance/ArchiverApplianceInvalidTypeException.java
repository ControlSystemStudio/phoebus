package org.phoebus.archive.reader.appliance;

import edu.stanford.slac.archiverappliance.PB.EPICSEvent.PayloadType;

/**
 *
 * <code>ArchiverApplianceInvalidTypeException</code> is thrown when optimized or statistical data is requested for
 * ad data type that does not support such operation (e.g. string data or waveforms).
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public class ArchiverApplianceInvalidTypeException extends ArchiverApplianceException {

    private static final long serialVersionUID = 9135411767906251819L;

    private final PayloadType type;
    private final String pv;

    /**
     * Constructs a new exception.
     *
     * @param message the message of the exception
     */
    public ArchiverApplianceInvalidTypeException(String message, String pv, PayloadType type) {
        super(message);
        this.pv = pv;
        this.type = type;
    }

    /**
     * @return the type of the PV that triggered this exception
     */
    public PayloadType getType() {
        return type;
    }

    /**
     * @return the name of the PV that triggered this exception
     */
    public String getPVName() {
        return pv;
    }
}
