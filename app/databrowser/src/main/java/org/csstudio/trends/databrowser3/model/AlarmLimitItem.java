package org.csstudio.trends.databrowser3.model;

import org.epics.util.stats.Range;
import org.epics.vtype.*;
import org.phoebus.archive.reader.appliance.ApplianceArchiveReaderConstants;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.pv.PV;


import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;


/**
 * Extension for the purpose of creating a plottable {@link ModelItem} representing alarm limits. Each sample
 * - historic and live - is based on the alarm limits (if present) of a {@link PlotSample}'s {@link VType}.
 */
public class AlarmLimitItem extends PVItem {

    private String limitType;
    private PVItem parentItem;

    public static AlarmLimitItem createAlarmLimitItem(final PVItem parentItem, final String limitType) throws IllegalArgumentException {
        if (parentItem == null) {
            throw new IllegalArgumentException("Parent item cannot be null");
        }
        PVSamples pvSamples = parentItem.getSamples();
        pvSamples.lockForWriting();
        PlotSample plotSample = pvSamples.get(0);
        if (plotSample == null) {
            throw new IllegalArgumentException("First element in samples cannot be null");
        }
        Display display = Display.displayOf(plotSample.getVType());
        if (display == null) {
            throw new IllegalArgumentException("Display of first sample is not available");
        }
        Range alarmRange = display.getAlarmRange();
        Range warningRange = display.getWarningRange();
        pvSamples.unlockForWriting();
        switch (limitType) {
            case ApplianceArchiveReaderConstants.LOLO:
                if (!Double.isFinite(alarmRange.getMinimum())) {
                    throw new IllegalArgumentException("LOLO alarm limit must be finite");
                }
                break;
            case ApplianceArchiveReaderConstants.LOW:
                if (!Double.isFinite(warningRange.getMinimum())) {
                    throw new IllegalArgumentException("LOW alarm limit must be finite");
                }
                break;
            case ApplianceArchiveReaderConstants.HIGH:
                if (!Double.isFinite(warningRange.getMaximum())) {
                    throw new IllegalArgumentException("HIGH alarm limit must be finite");
                }
                break;
            case ApplianceArchiveReaderConstants.HIHI:
                if (!Double.isFinite(alarmRange.getMaximum())) {
                    throw new IllegalArgumentException("HIHI alarm limit must be finite");
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown alarm limit type");
        }

        return new AlarmLimitItem(parentItem, limitType);
    }

    protected AlarmLimitItem(final PVItem parentItem, final String limitType) {
        super(parentItem.getName(), parentItem.getScanPeriod());
        this.limitType = limitType;
        this.parentItem = parentItem;
        PVSamples samplesFromArchiver = parentItem.getSamples();
        samplesFromArchiver.lockForWriting();
        List<VType> historicAlarmLimitValues = new ArrayList<>(samplesFromArchiver.size());
        VNumber vPreviousAlarmLimit = null;
        for (int i = 0; i < samplesFromArchiver.size(); i++) {
            VType vType = samplesFromArchiver.get(i).getVType();
            VType alarmLimitVType = getAlarmLimitVType(vType);
            VNumber alarmLimitVTypeNumber = (VNumber)alarmLimitVType;
            if(vPreviousAlarmLimit == null || !vPreviousAlarmLimit.getValue().equals(alarmLimitVTypeNumber.getValue())){
                historicAlarmLimitValues.add(alarmLimitVType);
            }
            vPreviousAlarmLimit = alarmLimitVTypeNumber;
        }
        samplesFromArchiver.unlockForWriting();
        getSamples().mergeArchivedData(parentItem.getName(), historicAlarmLimitValues);
        try {
            setName(parentItem.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private VType getAlarmLimitVType(VType archiverSample) {
        Display display = Display.displayOf(archiverSample);
        switch (limitType) {
            case ApplianceArchiveReaderConstants.LOLO:
                return VDouble.of(display.getAlarmRange().getMinimum(),
                        Alarm.none(), Time.timeOf(archiverSample), Display.none());
            case ApplianceArchiveReaderConstants.LOW:
                return VDouble.of(display.getWarningRange().getMinimum(),
                        Alarm.none(), Time.timeOf(archiverSample), Display.none());
            case ApplianceArchiveReaderConstants.HIGH:
                return VDouble.of(display.getWarningRange().getMaximum(),
                        Alarm.none(), Time.timeOf(archiverSample), Display.none());
            case ApplianceArchiveReaderConstants.HIHI:
                return VDouble.of(display.getAlarmRange().getMaximum(),
                        Alarm.none(), Time.timeOf(archiverSample), Display.none());
        }
        return null;
    }


    /**
     * Does not write anything to the plt file as alarm limits are not supposed to be displayed by default.
     * @param writer PrintWriter
     * @throws Exception
     */
    @Override
    public void write(final XMLStreamWriter writer) throws Exception {
    }

    @Override
    public void valueChanged(VType value){
        super.valueChanged(getAlarmLimitVType(value));
    }

}
