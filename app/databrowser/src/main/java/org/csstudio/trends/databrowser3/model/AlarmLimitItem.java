package org.csstudio.trends.databrowser3.model;

import org.epics.vtype.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Extension for the purpose of creating a plottable {@link ModelItem} representing alarm limits. Each sample
 * - historic and live - is based on the alarm limits (if present) of a {@link PlotSample}'s {@link VType}.
 */
public class AlarmLimitItem extends PVItem {

    private final LimitType limitType;

    /**
     * Creates an {@link AlarmLimitItem} based on a {@link PVItem} and the {@link Display} information in each sample.
     * @param parentItem The {@link PVItem} from which to construct an {@link AlarmLimitItem}
     * @param limitType One of the limit types defined in {@link LimitType}
     */
    public AlarmLimitItem(final PVItem parentItem, final LimitType limitType){
        super(parentItem.getName(), parentItem.getScanPeriod());
        this.limitType = limitType;
        PVSamples samplesFromArchiver = parentItem.getSamples();
        samplesFromArchiver.lockForWriting();
        List<VType> historicAlarmLimitValues = new ArrayList<>(samplesFromArchiver.size());
        VNumber vPreviousAlarmLimit = null;
        for (int i = 0; i < samplesFromArchiver.size(); i++) {
            VType vType = samplesFromArchiver.get(i).getVType();
            Optional<VType> alarmLimitVType = getAlarmLimitVType(vType);
            if(alarmLimitVType.isPresent()) {
                VNumber alarmLimitVTypeNumber = (VNumber)alarmLimitVType.get();
                if(vPreviousAlarmLimit == null || !vPreviousAlarmLimit.getValue().equals(alarmLimitVTypeNumber.getValue())){
                    historicAlarmLimitValues.add(alarmLimitVType.get());
                }
                vPreviousAlarmLimit = alarmLimitVTypeNumber;
            }
        }
        samplesFromArchiver.unlockForWriting();
        getSamples().mergeArchivedData(parentItem.getName(), historicAlarmLimitValues);
        setDisplayName(parentItem.getName() + "_" + limitType);
    }

    /**
     * Computes a {@link VType} from the {@link Display} of an archived or live {@link VType} value.
     * @param value Archived or live {@link VType} value.
     * @return {@link Optional#empty()} if the {@link Display} of the {@link VType} values is {@link Double#NEGATIVE_INFINITY}
     * or {@link Double#POSITIVE_INFINITY}. Otherwise, a {@link VDouble} where the value is the alarm limit (as
     * specified in {@link #limitType}), no alarm and same {@link Time} as the {@link VType} value.
      */
    protected Optional<VType> getAlarmLimitVType(VType value) {
        Display display = Display.displayOf(value);
        double doubleValue = switch (limitType) {
            case LOLO -> display.getAlarmRange().getMinimum();
            case LOW -> display.getWarningRange().getMinimum();
            case HIGH -> display.getWarningRange().getMaximum();
            case HIHI -> display.getAlarmRange().getMaximum();
        };
        return Double.isFinite(doubleValue) ? Optional.of(VDouble.of(doubleValue, Alarm.none(), Time.timeOf(value), Display.none())) :
                Optional.empty();
    }

    /**
     * Handles new live data.
     * <p>
     *     If an alarm limit is not defined, the live data will use Double.POSITIVE_INFINITE or
     *     Double.NEGATIVE_INFITE. As we do not want to plot that, such live values are ignored.
     * </p>
     * <p>
     *     Note that command line tools caget and pvget return zero for an undefined alarm limit,
     *     while the PV libs in Phoebus treat this differently.
     * </p>
     * @param value A live value added to the {@link ModelItem}
     */
    @Override
    public void valueChanged(VType value){
        getAlarmLimitVType(value).ifPresent(super::valueChanged);
    }

    /**
     * Helper enum...
     */
    public enum LimitType {
        LOLO,
        LOW,
        HIGH,
        HIHI
    }
}
