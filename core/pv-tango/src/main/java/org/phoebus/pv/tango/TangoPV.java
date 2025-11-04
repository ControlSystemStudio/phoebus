package org.phoebus.pv.tango;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.epics.util.stats.Range;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VEnum;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.tango.server.events.EventType;

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.EventProperties;
import fr.esrf.TangoApi.AttributeEventInfo;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.AttributeInfoEx;
import fr.esrf.TangoApi.CallBack;
import fr.esrf.TangoApi.CommandInfo;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoApi.events.EventData;
import fr.soleil.tango.clientapi.InsertExtractUtils;

/**
 * Generic Tango PV manage both attribute and command
 *
 * @author katy.saintin@cea.fr
 */
public class TangoPV extends PV {

    private static enum MetaData {
        DESC, LABEL, WRITE, EGU, HOPR, HIHI, HIGH, LOPR, LOW, LOLO, STAT
    };

    private static final String DOT = ".";

    private String device;
    private String entityName;
    private DeviceProxy deviceProxy = null;
    private AttributeInfo attributeInfo = null;
    private CommandInfo commandInfo = null;
    private Display display = null;
    private EnumDisplay enumDisplay = null;
    private Boolean entityFound = null;
    private MetaData metaData = null;
    private TangoCallBack cb = null;

    private static final int DEFAULT_PERIOD_REFRESHING = 1000;
    private int period = DEFAULT_PERIOD_REFRESHING;
    private static final String UNDEFINED = "Not specified";
    private static Map<String, Boolean> pollingMode = new HashMap<>();

    public TangoPV(String name, String baseName) {
        super(name);
        if (TangoPreferences.getInstance().isTangoDbEnable()) {
            // First remove .DESC field
            String fullDeviceName = baseName;
            // Test if it is a metadata
            metaData = getMetadata(fullDeviceName);
            if (metaData != null) {
                fullDeviceName = fullDeviceName.replace(DOT + metaData.toString(), "");
            }
            device = TangoDeviceHelper.getDeviceName(fullDeviceName);
            entityName = TangoDeviceHelper.getEntityName(fullDeviceName);

            initTangoEntity();
        } else {
            // No attribute info found
            VType initValue = VString.of("--", Alarm.disconnected(), Time.now());
            notifyListenersOfValue(initValue);
        }
    }

    private MetaData getMetadata(String fullDeviceName) {
        MetaData md = null;
        if (fullDeviceName != null && fullDeviceName.contains(DOT)) {
            MetaData[] values = MetaData.values();
            for (MetaData val : values) {
                if (fullDeviceName.endsWith(DOT + val.toString())) {
                    md = val;
                    break;
                }
            }
        }
        return md;
    }

    private boolean isMetaData() {
        return metaData != null && metaData != MetaData.WRITE && metaData != MetaData.STAT;
    }

    private void initTangoEntity() {
        VType initValue = null;
        AttributeInfoEx attributeInfoEx = null;
        String description = null;
        if (entityFound == null) {
            deviceProxy = TangoDeviceHelper.getDeviceProxy(device, true);
            if (deviceProxy == null) {
                entityFound = false;
            }
            // Init Attribute or command
            if (deviceProxy != null && entityName != null) {
                try {
                    // Test if it is an attribute
                    if (TangoAttributeHelper.isAttributeRunning(device, entityName)) {
                        attributeInfo = TangoAttributeHelper.getAttributeInfo(device, entityName);
                        entityFound = attributeInfo != null;
                        if (entityFound) {
                            description = attributeInfo.description;
                            if (description == null || description.trim().isEmpty()) {
                                description = attributeInfo.toString();
                            }
                            attributeInfoEx = TangoAttributeHelper.getAttributeInfoEx(device, entityName);
                            if (metaData != null && metaData == MetaData.STAT) {
                                enumDisplay = TangoPVUtil.getAttributeQualityEnumDisplay(attributeInfo);
                                display = ((AdvancedEnumDisplay) enumDisplay).getDisplay();

                            } else if (entityName.equalsIgnoreCase("State")) {
                                // Build State enumeration
                                enumDisplay = TangoPVUtil.getDevStateEnumDisplay(device);
                                display = ((AdvancedEnumDisplay) enumDisplay).getDisplay();
                            } else {
                                display = TangoPVUtil.buildDisplayFromAttributeInfo(attributeInfo, attributeInfoEx,
                                        description);
                                enumDisplay = TangoPVUtil.buildEnumDisplayFromAttributeInfo(attributeInfoEx, display);
                            }
                            // It is not a description, read attribute value
                            if (!isMetaData()) {
                                initValue = readValue();
                            }
                        } else {
                            // No attribute info found
                            initValue = VString.of("--", Alarm.disconnected(), Time.now());
                        }
                    } else if (TangoCommandHelper.doesCommandExist(device, entityName)) {
                        commandInfo = TangoCommandHelper.getCommandInfo(deviceProxy, entityName);
                        entityFound = commandInfo != null;
                        if (entityFound) {
                            description = commandInfo.out_type_desc;
                            if (description == null || description.trim().isEmpty()) {
                                description = commandInfo.toString();
                            }
                            // It is not a description, empty value
                            if (!isMetaData()) {
                                initValue = VString.of("--", Alarm.none(), Time.now());
                            }
                        } else {
                            // No command info found
                            initValue = VString.of("--", Alarm.disconnected(), Time.now());
                        }
                    } else {
                        entityFound = false;
                        initValue = VString.of("--", Alarm.disconnected(), Time.now());
                    }
                } catch (Exception e) {
                    entityFound = false;
                    String errorMessage = TangoExceptionHelper.getErrorMessage(e);
                    initValue = VString.of(errorMessage, Alarm.disconnected(), Time.now());
                }
            }
        }

        if (isMetaData() && entityFound) {
            // Value is fix no need subscription
            Double dVal = null;
            String numberVal = null;
            String sVal = null;
            switch (metaData) {
                case DESC:
                    // Init Display
                    // Value is fixe no need subscription
                    sVal = description;
                    break;

                case LABEL:
                    sVal = attributeInfo != null ? attributeInfo.label : null;
                    if (sVal == null || sVal.trim().isEmpty()) {
                        sVal = entityName;
                    }
                    break;

                case EGU:
                    sVal = attributeInfo != null ? attributeInfo.unit : null;
                    if (sVal == null || sVal.trim().isEmpty()) {
                        sVal = "";
                    }
                    break;

                case HOPR:
                    numberVal = attributeInfo != null ? attributeInfo.max_value : null;
                    dVal = getValue(numberVal);
                    break;

                case LOPR:
                    numberVal = attributeInfo != null ? attributeInfo.min_value : null;
                    dVal = getValue(numberVal);
                    break;

                case LOW:
                    numberVal = attributeInfoEx != null ? attributeInfoEx.alarms.min_warning : null;
                    dVal = getValue(numberVal);
                    break;

                case HIGH:
                    numberVal = attributeInfoEx != null ? attributeInfoEx.alarms.max_warning : null;
                    dVal = getValue(numberVal);
                    break;

                case LOLO:
                    numberVal = attributeInfo != null ? attributeInfo.min_alarm : null;
                    dVal = getValue(numberVal);
                    break;

                case HIHI:
                    numberVal = attributeInfo != null ? attributeInfo.max_alarm : null;
                    dVal = getValue(numberVal);
                    break;

                default:
                    break;
            }

            if (sVal != null) {
                initValue = VString.of(sVal, Alarm.none(), Time.now());
            } else if (dVal != null) {
                display = Display.of(Range.undefined(), Range.undefined(), Range.undefined(), Range.undefined(), "",
                        Display.defaultNumberFormat(), description);
                initValue = VDouble.of(dVal, Alarm.none(), Time.now(), display);
            }
        }

        if (initValue != null) {
            notifyListenersOfValue(initValue);
        }

        // Start monitoring event or polling
        if (attributeInfoEx != null && !isMetaData()) {
            // Check if event is activate on device
            Boolean polling = pollingMode.get(device);
            if (deviceProxy != null && polling == null) {
                CallBack dummyCb = new CallBack();
                try {
                    int id = deviceProxy.subscribe_event(EventType.PERIODIC_EVENT.getValue(), dummyCb, true);
                    polling = false;
                    deviceProxy.unsubscribe_event(id);
                } catch (Exception e) {
                    polling = true;
                    pollingMode.put(device, polling);
                }
            }

            AttributeEventInfo events = attributeInfoEx.events;
            EventProperties tangoObj = events.getTangoObj();
            String periodString = tangoObj.per_event != null ? tangoObj.per_event.period : null;
            EventType eventType = EventType.PERIODIC_EVENT;
            if (periodString != null && !periodString.isEmpty() && !periodString.equals(UNDEFINED)) {
                eventType = EventType.PERIODIC_EVENT;
                period = Double.valueOf(periodString).intValue();
            } else if (tangoObj.ch_event != null) {
                eventType = EventType.CHANGE_EVENT;
            }
            cb = new TangoCallBack(eventType);
        }
    }

    private Double getValue(String val) {
        Double dval = null;
        if (val != null && !val.trim().isEmpty()) {
            try {
                dval = Double.valueOf(val.trim());

            } catch (Exception e) {
                dval = null;
            }
        }
        return dval;
    }

    private class TangoCallBack extends CallBack {
        private static final long serialVersionUID = 1L;
        private int event_id;
        private Thread threadReader = null;
        private boolean start = false;

        public TangoCallBack(EventType eventType) {
            Boolean polling = pollingMode.get(device);
            if (!polling) {
                try {
                    event_id = deviceProxy.subscribe_event(entityName, eventType.getValue(), this, new String[] {});
                } catch (Exception e) {
                    String message = TangoExceptionHelper.getErrorMessage(e);
                    System.err.println("Error on subscription " + message);
                }
            }

            else if (!start) {
                start = true;
                threadReader = new Thread(getName() + " reader") {
                    public void run() {
                        while (start) {
                            VType value = readValue();
                            if (value != null) {
                                notifyListenersOfValue(value);
                            }

                            try {
                                Thread.sleep(period);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    };
                };
                threadReader.start();
            }
        }

        @Override
        public void push_event(EventData evt) {
            VType value = readValue();
            if (value != null) {
                notifyListenersOfValue(value);
            }
        }

        public void unsubscribe_event() {
            if (threadReader != null) {
                start = false;
                threadReader = null;
            } else if (deviceProxy != null) {
                try {
                    deviceProxy.unsubscribe_event(event_id);
                } catch (Exception e) {
                    String message = TangoExceptionHelper.getErrorMessage(e);
                    System.err.println("Error on unsubscription " + message);
                }
            }
        }
    }

    private VType readValue() {
        VType rValue = null;
        if (entityFound && attributeInfo != null) {
            try {
                DeviceAttribute deviceAttribute = TangoAttributeHelper.getDeviceAttribute(deviceProxy, entityName);
                int type = deviceAttribute.getType();
                AttrDataFormat dataFormat = deviceAttribute.getDataFormat();
                Alarm alarm = TangoPVUtil.buildAlarmFromAttribute(deviceAttribute);
                long time = deviceAttribute.getTime();
                if (metaData != null && metaData == MetaData.STAT) {
                    // Read Quality of attribute
                    int attributeQuality = TangoAttributeHelper.getAttributeQuality(deviceAttribute);
                    Time t = Time.of(Instant.ofEpochMilli(time));
                    rValue = VEnum.of(attributeQuality, enumDisplay, alarm, t);
                } else {
                    Object result = null;
                    if (metaData != null && metaData == MetaData.WRITE) {
                        result = InsertExtractUtils.extractWrite(deviceAttribute, attributeInfo.writable, dataFormat);
                    } else {
                        result = InsertExtractUtils.extractRead(deviceAttribute, dataFormat);
                    }

                    boolean isArray = dataFormat != null
                            && (dataFormat == AttrDataFormat.SPECTRUM || dataFormat == AttrDataFormat.IMAGE);
                    int[] sizes = null;
                    if (dataFormat == AttrDataFormat.IMAGE) {
                        sizes = new int[] { deviceAttribute.getDimX(), deviceAttribute.getDimY() };
                    }
                    rValue = TangoPVUtil.convertResultToVtype(isArray, sizes, type, result, alarm, time, display,
                            enumDisplay);
                }
            } catch (Exception e) {
                String errorMessage = TangoExceptionHelper.getErrorMessage(e);
                rValue = VString.of(errorMessage, Alarm.disconnected(), Time.now());
            }
        }
        return rValue;
    }

    private VType executeCommand(Object newValue) {
        VType rValue = null;
        if (entityFound && commandInfo != null) {
            try {
                Time time = Time.now();
                Object executeCommand = TangoCommandHelper.executeCommand(deviceProxy, entityName, newValue);
                Alarm alarm = Alarm.none();
                if (executeCommand != null) {
                    int out_type = commandInfo.out_type;
                    String tangoTypeForType = TangoCommandHelper.getTangoTypeForType(out_type);
                    boolean isArray = tangoTypeForType.toUpperCase().endsWith(TangoConstHelper.ARRAY_NAME);
                    rValue = TangoPVUtil.convertResultToVtype(isArray, null, out_type, executeCommand, alarm,
                            time.getTimestamp().toEpochMilli(), display, enumDisplay);
                } else {
                    rValue = VString.of("OK", alarm, time);
                }
            } catch (Exception e) {
                String errorMessage = TangoExceptionHelper.getErrorMessage(e);
                rValue = VString.of(errorMessage, Alarm.disconnected(), Time.now());
            }

        }
        return rValue;
    }

    @Override
    public void write(Object new_value) throws Exception {
        if (!isMetaData() && entityFound) {
            VType rValue = null;
            if (attributeInfo != null) {
                DeviceAttribute deviceAttribute = TangoAttributeHelper.getDeviceAttribute(deviceProxy, entityName);
                InsertExtractUtils.insert(deviceAttribute, new_value);
                deviceProxy.write_attribute(deviceAttribute);
                rValue = readValue();
            } else if (commandInfo != null) {
                rValue = executeCommand(new_value);
            }
            if (rValue != null) {
                notifyListenersOfValue(rValue);
            }
        }
    }

    @Override
    public boolean isReadonly() {
        boolean isRO = true;
        if (!isMetaData() && metaData != MetaData.STAT) {
            if (attributeInfo != null) {
                AttrWriteType writable = attributeInfo.writable;
                isRO = writable == AttrWriteType.READ;
            } else if (commandInfo != null) {
                isRO = false;
            }
        }
        return isRO;
    }

    @Override
    protected void close() {
        deviceProxy = null;
        commandInfo = null;
        attributeInfo = null;
        entityFound = null;
        metaData = null;
        period = DEFAULT_PERIOD_REFRESHING;
        if (cb != null) {
            cb.unsubscribe_event();
            cb = null;
        }
        TangoPVFactory.releasePV(this);
        super.close();
    }
}
