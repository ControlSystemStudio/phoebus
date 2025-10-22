package org.phoebus.pv.tango;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;

import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.ListInteger;
import org.epics.util.stats.Range;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VByte;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLong;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VShort;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.tango.utils.ArrayUtils;

import fr.esrf.Tango.AttrQuality;
import fr.esrf.Tango.DevEncoded;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoApi.AttributeAlarmInfo;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.AttributeInfoEx;
import fr.esrf.TangoApi.DeviceAttribute;

/**
 * Generic Tango PV util
 *
 * @author katy.saintin@cea.fr
 */

public class TangoPVUtil {

    public static VType convertResultToVtype(boolean isArray, int[] sizes, int tangotype, Object aresult, Alarm alarm, long ltime, Display display,
            EnumDisplay enumDisplay) {
        VType resultType = null;
        Object result = aresult;
        if (result != null) {
            int tgType = TangoConstHelper.getTangoType(tangotype);
            String tangoFieldName = TangoConstHelper.getTangoFieldName(tangotype);
            if (tgType != TangoConstHelper.UNKNOW_TYPE && tangoFieldName != null) {
                int attFormat = TangoConstHelper.getAttributeTangoFormat(tangotype);
                boolean unsigned = false;
                boolean long64 = isLong64(tangoFieldName);
                Time time = Time.of(Instant.ofEpochMilli(ltime));
                ListInteger intSize = null;
                if(sizes != null) {
                    try {
                        result = ArrayUtils.from2DArrayToArray(aresult);
                        intSize = ArrayInteger.of(sizes);
                    }
                    catch (Exception e) {
                        intSize = null;
                    }
                }
                if (attFormat == TangoConstHelper.NUMERICAL_FORMAT) {
                    if (tangoFieldName.contains(TangoConstHelper.DOUBLE_NAME)) {
                        if (!isArray) {
                            resultType = VDouble.of((Double) result, alarm, time, display);
                        } else {
                            ArrayDouble arrayDouble = ArrayDouble.of((double[]) result);
                            if(intSize != null) {
                                resultType = VDoubleArray.of(arrayDouble, intSize, alarm, time, display);
                            }
                            else {
                                resultType = VDoubleArray.of(arrayDouble, alarm, time, display);
                            }
                        }
                    } else if (tangoFieldName.contains(TangoConstHelper.FLOAT_NAME)) {
                        if (!isArray) {
                            resultType = VFloat.of((Float) result, alarm, time, display);
                        } else {
                            ArrayFloat arrayFloat = ArrayFloat.of((float[]) result);
                            if(intSize != null) {
                                resultType = VFloatArray.of(arrayFloat, intSize, alarm, time, display);
                            }
                            else {
                                resultType = VFloatArray.of(arrayFloat, alarm, time, display);
                            }
                        }
                    } else if (tangoFieldName.contains(TangoConstHelper.INT_NAME)
                            || tangoFieldName.contains(TangoConstHelper.LONG_NAME)) {
                        resultType = convertToVIntType(result, isArray, intSize,alarm, time, display);
                    } else if (tangoFieldName.contains(TangoConstHelper.LONG_NAME)) {
                        if (!unsigned && !long64) {
                            resultType = convertToVIntType(result, isArray, intSize,alarm, time, display);
                        } else {
                            // Manage ULONG and ULONG64
                            if (!isArray) {
                                resultType = VLong.of((Long) result, alarm, time, display);
                            } else {
                                ArrayLong arrayLong = ArrayLong.of((long[]) result);
                                if(intSize != null) {
                                    resultType = VLongArray.of(arrayLong, intSize, alarm, time, display);
                                }
                                else {
                                    resultType = VLongArray.of(arrayLong, alarm, time, display);
                                }
                            }
                        }
                    } else if (tangoFieldName.contains(TangoConstHelper.SHORT_NAME)) {
                        unsigned = isUnsigned(tangoFieldName, TangoConstHelper.SHORT_NAME);
                        if (unsigned) {
                            resultType = convertToVIntType(result, isArray, intSize, alarm, time, display);
                        } else {
                            if (!isArray) {
                                resultType = VShort.of((Short) result, alarm, time, display);
                            } else {
                                ArrayShort arrayshort = ArrayShort.of((short[]) result);
                                if(intSize != null) {
                                    resultType = VShortArray.of(arrayshort, intSize, alarm, time, display);
                                }
                                else {
                                    resultType = VShortArray.of(arrayshort, alarm, time, display);
                                }
                            }
                        }
                    } else if (tangoFieldName.contains(TangoConstHelper.CHAR_NAME)) {
                        unsigned = isUnsigned(tangoFieldName, TangoConstHelper.CHAR_NAME);
                        if (unsigned) {
                            if (!isArray) {
                                resultType = VShort.of((Short) result, alarm, time, display);
                            } else {
                                ArrayShort arrayshort = ArrayShort.of((short[]) result);
                                if(intSize != null) {
                                    resultType = VShortArray.of(arrayshort, intSize,alarm, time, display);
                                }
                                else {
                                    resultType = VShortArray.of(arrayshort, alarm, time, display);
                                }
                            }
                        } else {
                            byte[] byteArrays = (byte[]) result;
                            if (byteArrays.length > 0) {
                                if (byteArrays.length > 1) {
                                    ArrayByte arraybyte = ArrayByte.of(byteArrays);
                                    resultType = VByteArray.of(arraybyte, alarm, time, display);
                                } else {
                                    resultType = VByte.of((Byte) byteArrays[0], alarm, time, display);
                                }
                            }
                        }
                    } else if (tangoFieldName.contains(TangoConstHelper.ENUM_NAME)) {
                        if (enumDisplay != null) {
                            resultType = VEnum.of((Short) result, enumDisplay, alarm, time);
                        } else {
                            resultType = VShort.of((Short) result, alarm, time, display);
                        }
                    } else if (tangoFieldName.contains(TangoConstHelper.ENCODED_NAME)) {
                        if (result instanceof DevEncoded) {
                            DevEncoded encoded = (DevEncoded) result;
                            ArrayByte arraybyte = ArrayByte.of(encoded.encoded_data);
                            resultType = VByteArray.of(arraybyte, alarm, time, display);
                        }
                    }
                } else if (attFormat == TangoConstHelper.BOOLEAN_FORMAT) {
                    if (!isArray) {
                        if(enumDisplay == null) {
                            enumDisplay = AdvancedEnumDisplay.of("0","1");
                        }
                        int val = (Boolean) result ? 1 : 0;
                        resultType = VEnum.of((short) val, enumDisplay, alarm, time);
                    } else {
                        boolean[] bArray = (boolean[]) result;
                        short[] shArray = new short[bArray.length];
                        for (int i = 0; i < shArray.length; i++) {
                            shArray[i] = bArray[i] ? (short) 1 : (short) 0;
                        }
                        ArrayShort arrayshort = ArrayShort.of(shArray);
                        if(intSize != null) {
                            resultType = VShortArray.of(arrayshort, intSize, alarm, time, display);
                        }
                        else {
                            resultType = VShortArray.of(arrayshort, alarm, time, display);
                        }
                    }
                } else {// Manage String
                    if (result instanceof DevState) {
                        DevState state = (DevState) result;
                        Alarm alarmState = alarm;
                        AlarmStatus status = AlarmStatus.DEVICE;
                        
                        switch (state.value()) {
                            case DevState._ALARM : 
                            case DevState._DISABLE : 
                               alarmState= Alarm.of(AlarmSeverity.MINOR, status, state.toString());
                               break;
                            case DevState._FAULT : 
                                alarmState= Alarm.of(AlarmSeverity.MAJOR, status, state.toString());
                                break;
                            case DevState._UNKNOWN : 
                                alarmState= Alarm.of(AlarmSeverity.MAJOR, AlarmStatus.UNDEFINED, state.toString());
                                break;
                        }
                        resultType = VString.of(state.toString(), alarmState, time);
                    } else {
                        resultType = VString.of(result.toString(), alarm, time);
                    }
                }
            }
        }
        return resultType;
    }

    private static VType convertToVIntType(Object result, boolean isArray,  ListInteger intSize, Alarm alarm, Time time, Display display) {
        VType vIntResult = null;
        if (result != null) {
            if (!isArray) {
                vIntResult = VInt.of((Integer) result, alarm, time, display);
            } else {
                ArrayInteger arrayInt = ArrayInteger.of((int[]) result);
                if(intSize != null) {
                    vIntResult = VIntArray.of(arrayInt, intSize, alarm, time, display);
                }
                else {
                    vIntResult = VIntArray.of(arrayInt, alarm, time, display);
                }
            }
        }
        return vIntResult;
    }

    protected static Alarm buildAlarmFromAttribute(DeviceAttribute devAttr) {
        AlarmSeverity sev = AlarmSeverity.INVALID;
        AlarmStatus stat = AlarmStatus.UNDEFINED;
        String alarmMessage = "Disconnected";
        if (devAttr != null) {
            try {
                int attributeQuality = TangoAttributeHelper.getAttributeQuality(devAttr);
                alarmMessage = TangoAttributeHelper.getAttributeStringQuality(devAttr);
                switch (attributeQuality) {
                    case AttrQuality._ATTR_VALID:
                    case AttrQuality._ATTR_CHANGING:
                        sev = AlarmSeverity.NONE;
                        stat = AlarmStatus.NONE;
                        break;
                    case AttrQuality._ATTR_WARNING:
                        sev = AlarmSeverity.MINOR;
                        stat = AlarmStatus.RECORD;
                        break;
                    case AttrQuality._ATTR_ALARM:
                        sev = AlarmSeverity.MAJOR;
                        stat = AlarmStatus.RECORD;
                        break;
                }
            } catch (Exception e) {
                alarmMessage = TangoExceptionHelper.getErrorMessage(e);
            }
        }
        return Alarm.of(sev, stat, alarmMessage);
    }

    protected static Display buildDisplayFromAttributeInfo(AttributeInfo info, AttributeInfoEx infoEx, String description) {
        Display display = null;
        if (info != null && description != null) {
            String unit = info.unit != null ? info.unit : "";
            String format = info.format; // TODO Define precision
            String max_alarm = info.max_alarm;
            String min_alarm = info.min_alarm;
            String min_value = info.min_value;
            String max_value = info.max_value;
            AttributeAlarmInfo alarmInfo = infoEx != null ? infoEx.alarms : null ;
            String min_warning = alarmInfo != null ? alarmInfo.min_warning : null;
            String max_warning = alarmInfo != null ? alarmInfo.max_warning : null;

            Range displayRange = null;
            Range alarmRange = null;
            Range warningRange = null;
            Range controlRange = null;
            NumberFormat numberFormat = Display.defaultNumberFormat();

            //Set Control Range and display Range
            Double minValue = Double.NaN;
            Double maxValue = Double.NaN;
            if (min_value != null && !min_value.trim().isEmpty()) {
                try {
                    minValue = Double.valueOf(min_value.trim());
                    maxValue = Double.POSITIVE_INFINITY;
                } catch (Exception e) {
                    minValue = Double.NaN;
                }
            }

            if (max_value != null && !max_value.trim().isEmpty()) {
                try {
                    maxValue = Double.valueOf(max_value.trim());
                    if (minValue.isNaN()) {
                        minValue = Double.NEGATIVE_INFINITY;
                    }
                } catch (Exception e) {
                    if (!minValue.isNaN()) {
                        maxValue = Double.POSITIVE_INFINITY;
                    } else {
                        maxValue = Double.NaN;
                    }
                }
            }
            
            if(!minValue.isNaN() && !maxValue.isNaN()) {
                displayRange = Range.of(minValue, maxValue);
                controlRange = Range.of(minValue, maxValue);
            }
            else {
                displayRange = Range.undefined();
                controlRange = Range.undefined();
            }
            
            //Set alarm Range Range
            Double minAlarm = Double.NaN;
            Double maxAlarm = Double.NaN;
            if (min_alarm != null && !min_alarm.trim().isEmpty()) {
                try {
                    minAlarm = Double.valueOf(min_alarm.trim());
                    maxAlarm = Double.POSITIVE_INFINITY;
                } catch (Exception e) {
                    minAlarm = Double.NaN;
                }
            }

            if (max_alarm != null && !max_alarm.trim().isEmpty()) {
                try {
                    maxAlarm = Double.valueOf(max_alarm.trim());
                    if (minAlarm.isNaN()) {
                        minAlarm = Double.NEGATIVE_INFINITY;
                    }
                } catch (Exception e) {
                    if (!minAlarm.isNaN()) {
                        maxAlarm = Double.POSITIVE_INFINITY;
                    } else {
                        maxAlarm = Double.NaN;
                    }
                }
            }
            
            if(!minAlarm.isNaN() && !maxAlarm.isNaN()) {
                alarmRange = Range.of(minAlarm, maxAlarm);
            }
            else {
                alarmRange = Range.undefined();
            }
            
            //Set warning Range
            Double minWarning = Double.NaN;
            Double maxWarning = Double.NaN;
            if (min_warning != null && !min_warning.trim().isEmpty()) {
                try {
                    minWarning = Double.valueOf(min_warning.trim());
                    maxWarning = Double.POSITIVE_INFINITY;
                } catch (Exception e) {
                    minWarning = Double.NaN;
                }
            }

            if (max_warning != null && !max_warning.trim().isEmpty()) {
                try {
                    maxWarning = Double.valueOf(max_warning.trim());
                    if (minWarning.isNaN()) {
                        minWarning = Double.NEGATIVE_INFINITY;
                    }
                } catch (Exception e) {
                    if (!minAlarm.isNaN()) {
                        maxWarning = Double.POSITIVE_INFINITY;
                    } else {
                        maxWarning = Double.NaN;
                    }
                }
            }
            
            if(!minWarning.isNaN() && !maxWarning.isNaN()) {
                warningRange = Range.of(minWarning, maxWarning);
            }
            else {
                warningRange = Range.undefined();
            }
            
            if(format != null && format.trim().startsWith("%") && format.contains(".")) {
                //remplace last f or e or d
                String formatString = format.trim().replaceFirst("%", "");
                boolean scientifique = formatString.toLowerCase().endsWith("e");
                formatString = formatString.replace("e", "");
                formatString = formatString.replace("f", "");
                formatString = formatString.replace("d", "");
                int nbDigit = 0;
                
                try {
                    int indexOf = formatString.indexOf(".");
                    String nbDigitString = formatString.substring(indexOf+1);
                    nbDigit = Double.valueOf(nbDigitString).intValue();
                }
                catch (Exception e) {
                    nbDigit = 0;
                }
                if(!scientifique) {
                    numberFormat = NumberFormats.precisionFormat(nbDigit);
                }
                else {
                    StringBuilder sb = new StringBuilder("0.");
                    for (int i = 0; i < nbDigit; i++) {
                        sb.append("0");
                    }
                    sb.append("E0");
                    numberFormat = new DecimalFormat(sb.toString());
                }
            }
            
            display = Display.of(displayRange, alarmRange, warningRange, controlRange, unit, numberFormat, description);

        }

        if(display == null) {
            display = Display.none();
        }
        
        return display;
    }

    protected static EnumDisplay buildEnumDisplayFromAttributeInfo(AttributeInfoEx infoEx, Display display) {
        EnumDisplay enumDisplay = null;
        if (infoEx != null && display != null) {
            String[] enum_label = infoEx.enum_label;
            if (enum_label != null && enum_label.length > 0) {
                enumDisplay = AdvancedEnumDisplay.of(display, enum_label);
            }
        }
        return enumDisplay;
    }

    private static boolean isUnsigned(String fieldName, String tangoType) {
        return fieldName != null && tangoType != null && fieldName.contains("U" + tangoType);
    }

    private static boolean isLong64(String fieldName) {
        return fieldName != null && fieldName.contains(TangoConstHelper.LONG_NAME + "64");
    }

}
