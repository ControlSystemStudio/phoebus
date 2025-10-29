package org.phoebus.pv.tango;

import org.tango.utils.TangoUtil;

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrQuality;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.AttributeInfo;
import fr.esrf.TangoApi.AttributeInfoEx;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoApi.QualityUtilities;
import fr.esrf.TangoDs.TangoConst;

/**
 * 
 * This class provide useful static methods to insert or extract data from DeviceAttribute
 * 
 *  @author katy.saintin@cea.fr
 */


public class TangoAttributeHelper {
    private static final String CANNOT_SET_ATTRIBUTE_INFO_ON = "Cannot set AttributeInfo on ";
    private static final String CANNOT_READ_ATTRIBUTE_INFO_ON = "Cannot read AttributeInfo on ";
    private static final String CANNOT_READ_QUALITY_OF = "Cannot read quality of ";
    public static final String STATE = "state";
    public static final String STATUS = "status";

    /**
     * Returns whether an attribute name represents a state or a status
     * 
     * @param attributeName The attribute name (device name not included)
     * @return A <code>boolean</code> value
     */
    public static boolean isStateOrStatus(final String attributeName) {
        return isStateOrStatus(null, attributeName);
    }

    /**
     * Returns whether an attribute name represents a state or a status
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The attribute name (device name not included)
     * @return A <code>boolean</code> value
     */
    public static boolean isStateOrStatus(String deviceName, final String attributeName) {
        boolean stateOrStatus;
        if (STATE.equalsIgnoreCase(attributeName) || STATUS.equalsIgnoreCase(attributeName)) {
            stateOrStatus = true;
        } else if ((attributeName != null) && (deviceName != null)) {
            AttributeInfo info = getAttributeInfo(deviceName, attributeName);
            stateOrStatus = isScalarDevState(info);
        } else {
            stateOrStatus = false;
        }
        return stateOrStatus;
    }

    /**
     * Returns whether an attribute represents a state
     * 
     * @param info The attribute info
     * @return A <code>boolean</code> value
     */
    public static boolean isDevState(final AttributeInfo info) {
        boolean state;
        if (info == null) {
            state = false;
        } else {
            state = (info.data_type == TangoConst.Tango_DEV_STATE);
        }
        return state;
    }

    /**
     * Returns whether an attribute represents a state and is scalar
     * 
     * @param info The attribute info
     * @return A <code>boolean</code> value
     */
    public static boolean isScalarDevState(final AttributeInfo info) {
        boolean state;
        if (info == null) {
            state = false;
        } else {
            state = (info.data_type == TangoConst.Tango_DEV_STATE) && (info.data_format == AttrDataFormat.SCALAR);
        }
        return state;
    }

    /**
     * Returns an attribute's type
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return An <code>int</code>
     */
    public static int getAttributeType(String deviceName, String attributeName) {
        return getAttributeType(getAttributeInfo(deviceName, attributeName));
    }

    /**
     * Returns an attribute's type, based on its {@link AttributeInfo}
     * 
     * @param info The {@link AttributeInfo}
     * @return An <code>int</code>
     */
    public static int getAttributeType(AttributeInfo info) {
        int type = TangoConstHelper.UNKNOW_TYPE;
        if (info != null) {
            type = info.data_format.value();
        }
        return type;
    }

    /**
     * Returns an attribute's format
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return An <code>int</code>
     */
    public static int getAttributeFormat(String deviceName, String attributeName) {
        return getAttributeFormat(getAttributeInfo(deviceName, attributeName));
    }

    /**
     * Returns an attribute's format, based on its {@link AttributeInfo}
     * 
     * @param info The {@link AttributeInfo}
     * @return An <code>int</code>
     */
    public static int getAttributeFormat(AttributeInfo info) {
        int result = -1;
        if (info != null) {
            result = TangoConstHelper.getTangoFormat(info.data_type);
        }
        return result;
    }

    /**
     * Returns an attribute's label
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return A {@link String}
     */
    public static String getLabel(String deviceName, String attributeName) {
        return getLabel(getAttributeInfo(deviceName, attributeName));
    }

    /**
     * Returns an attribute's label, based on its {@link AttributeInfo}
     * 
     * @param info The {@link AttributeInfo}
     * @return A {@link String}
     */
    public static String getLabel(AttributeInfo info) {
        String label = null;
        if (info != null) {
            label = info.label;
        }
        return label;
    }

    /**
     * Returns whether an attribute is read only
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return A <code>boolean</code>
     */
    public static boolean isAttributeReadOnly(String deviceName, String attributeName) {
        return isAttributeReadOnly(getAttributeInfo(deviceName, attributeName));
    }

    /**
     * Returns whether an attribute is read only, based on its {@link AttributeInfo}
     * 
     * @param info The {@link AttributeInfo}
     * @return A <code>boolean</code>
     */
    public static boolean isAttributeReadOnly(AttributeInfo info) {
        boolean isReadOnly = true;
        if (info != null) {
            isReadOnly = (info.writable.value() == AttrWriteType._READ);
        }
        return isReadOnly;
    }

    /**
     * Reads an {@link AttributeInfoEx} for a given attribute and returns whether the attribute has unsigned values.
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return A <code>boolean</code>
     * @throws DevFailed In case of error when trying to read {@link AttributeInfoEx}
     */
    public static boolean isAttributeUnsigned(String deviceName, String attributeName) throws DevFailed {
        return isAttributeUnsigned(getAttributeInfoEx(deviceName, attributeName));
    }

    /**
     * Reads an {@link AttributeInfoEx} for a given attribute and returns whether the attribute has unsigned values.
     * 
     * @param device The {@link DeviceProxy} connected to the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return A <code>boolean</code>
     * @throws DevFailed In case of error when trying to read {@link AttributeInfoEx}
     */
    public static boolean isAttributeUnsigned(DeviceProxy device, String attributeName) throws DevFailed {
        return isAttributeUnsigned(getAttributeInfoEx(device, attributeName));
    }

    /**
     * Returns whether an attribute has unsigned values, based on its {@link AttributeInfo}
     * 
     * @param info The {@link AttributeInfo}
     * @return A <code>boolean</code>
     */
    public static boolean isAttributeUnsigned(AttributeInfo info) {
        boolean unsigned;
        if (info == null) {
            unsigned = false;
        } else {
            switch (info.data_type) {
                case TangoConst.Tango_DEV_UCHAR:
                case TangoConst.Tango_DEV_USHORT:
                case TangoConst.Tango_DEV_ULONG:
                case TangoConst.Tango_DEV_ULONG64:
                    unsigned = true;
                    break;
                default:
                    unsigned = false;
                    break;
            }
        }
        return unsigned;
    }

    /**
     * Sets an {@link AttributeInfo} in a given device
     * 
     * @param attributeInfo The {@link AttributeInfo} to set
     * @param deviceName The device name
     */
    public static void setAttributeInfo(AttributeInfo attributeInfo, String deviceName) {
        if (attributeInfo != null) {
            DeviceProxy proxy = TangoDeviceHelper.getDeviceProxy(deviceName);
            if (proxy != null) {
                try {
                    // JAVAAPI-603: synchronize accesses to DeviceProxy
                    synchronized (proxy) {
                        proxy.set_attribute_info(new AttributeInfo[] { attributeInfo });
                    }
                } catch (DevFailed e) {
                    StringBuilder builder = new StringBuilder(CANNOT_SET_ATTRIBUTE_INFO_ON);
                    builder.append(deviceName).append('/').append(attributeInfo.name);
                    System.err.println(builder.toString() + " " + TangoExceptionHelper.getErrorMessage(e));
                }
            }
        }
    }

    /**
     * Reads an {@link AttributeInfo} for a given attribute and returns it
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return An {@link AttributeInfo}
     */
    public static AttributeInfo getAttributeInfo(String deviceName, String attributeName) {
        AttributeInfo attributeInfo = null;
        DeviceProxy proxy = TangoDeviceHelper.getDeviceProxy(deviceName, false);
        if (isAttributeRunning(proxy, attributeName)) {
            try {
                // JAVAAPI-603: synchronize accesses to DeviceProxy
                synchronized (proxy) {
                    attributeInfo = proxy.get_attribute_info(attributeName);
                }
            } catch (DevFailed e) {
                StringBuilder builder = new StringBuilder(CANNOT_READ_ATTRIBUTE_INFO_ON);
                builder.append(deviceName).append('/').append(attributeName);
                System.err.println(builder.toString() + " " + TangoExceptionHelper.getErrorMessage(e));
            }
        }
        return attributeInfo;
    }

    /**
     * Reads an {@link AttributeInfoEx} for a given attribute and returns it
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return An {@link AttributeInfoEx}
     * @throws DevFailed In case of error when trying to read {@link AttributeInfoEx}
     */
    public static AttributeInfoEx getAttributeInfoEx(String deviceName, String attributeName) throws DevFailed {
        return getAttributeInfoEx(TangoDeviceHelper.getDeviceProxy(deviceName, false), attributeName);
    }

    /**
     * Reads an {@link AttributeInfoEx} for a given attribute and returns it
     * 
     * @param device The {@link DeviceProxy} connected to the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return An {@link AttributeInfoEx}
     * @throws DevFailed In case of error when trying to read {@link AttributeInfoEx}
     */
    public static AttributeInfoEx getAttributeInfoEx(DeviceProxy device, String attributeName) throws DevFailed {
        AttributeInfoEx attributeInfoEx = null;
        if (isAttributeRunning(device, attributeName)) {
            if (device != null) {
                // JAVAAPI-603: synchronize accesses to DeviceProxy
                synchronized (device) {
                    attributeInfoEx = device.get_attribute_info_ex(attributeName);
                }
            }
        }
        return attributeInfoEx;
    }

    /**
     * Sets an {@link AttributeInfoEx} in a given device
     * 
     * @param attributeInfoEx The {@link AttributeInfoEx} to set
     * @param deviceName The device name
     * @throws DevFailed In case of error when trying to write {@link AttributeInfoEx}
     */
    public static void setAttributeInfoEx(AttributeInfoEx attributeInfoEx, String deviceName) throws DevFailed {
        if (attributeInfoEx != null) {
            DeviceProxy proxy = TangoDeviceHelper.getDeviceProxy(deviceName);
            if (proxy != null) {
                // JAVAAPI-603: synchronize accesses to DeviceProxy
                synchronized (proxy) {
                    proxy.set_attribute_info(new AttributeInfoEx[] { attributeInfoEx });
                }
            }
        }
    }

    /**
     * Returns whether a given attribute is currently running
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return A <code>boolean</code>
     */
    public static boolean isAttributeRunning(String deviceName, String attributeName) {
        return isAttributeRunning(deviceName, attributeName, false);
    }

    /**
     * Returns whether a given attribute is currently running
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @param testForDeviceRunning Whether to first test if device is running
     * @return A <code>boolean</code>
     */
    public static boolean isAttributeRunning(String deviceName, String attributeName, boolean testForDeviceRunning) {
        return isAttributeRunning(TangoDeviceHelper.getDeviceProxy(deviceName, testForDeviceRunning), attributeName);
    }

    /**
     * Returns whether a given attribute is currently running
     * 
     * @param proxy The {@link DeviceProxy} of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return A <code>boolean</code>
     */
    protected static boolean isAttributeRunning(DeviceProxy proxy, String attributeName) {
        boolean running = false;
        if ((proxy != null) && (attributeName != null) && (!attributeName.trim().isEmpty())) {
            attributeName = attributeName.trim();
            if (!attributeName.isEmpty()) {
                try {
                    if (isStateOrStatus(attributeName)) {
                        running = true;
                    } else {
                        String[] attributeList;
                        // JAVAAPI-603: synchronize accesses to DeviceProxy
                        synchronized (proxy) {
                            attributeList = proxy.get_attribute_list();
                        }
                        if (isNameInList(attributeName, attributeList)) {
                            running = checkAttribute(proxy, attributeName);
                        } else {
                            // check for attribute alias
                            attributeName = TangoUtil.getAttributeName(attributeName);
                            if (isStateOrStatus(attributeName)) {
                                running = true;
                            } else if (isNameInList(attributeName, attributeList)) {
                                running = checkAttribute(proxy, attributeName);
                            }
                        }
                    }
                } catch (Exception e) {
                    running = false;
                }
            }
        }
        return running;
    }

    /**
     * Returns whether a {@link String} is contained in A {@link String} array, ignoring case.
     * 
     * @param name The {@link String}.
     * @param nameList The {@link String} array.
     * @return A <code>boolean</code>.
     */
    protected static boolean isNameInList(String name, String... nameList) {
        boolean contains = false;
        if (nameList != null) {
            for (String element : nameList) {
                if (name.equalsIgnoreCase(element)) {
                    contains = true;
                    break;
                }
            }
        }
        return contains;
    }

    /**
     * Tries to obtain an {@link AttributeInfo} for an attribute and checks whether that info is not null.
     * 
     * @param proxy The {@link DeviceProxy} that knows the attributes. Must not be <code>null</code>.
     * @param attributeName The attribute name.
     * @return A <code>boolean</code>.
     * @throws DevFailed If there was a connection problem.
     */
    protected static boolean checkAttribute(DeviceProxy proxy, String attributeName) throws DevFailed {
        boolean attributeOk;
        // JAVAAPI-603: synchronize accesses to DeviceProxy
        synchronized (proxy) {
            attributeOk = (proxy.get_attribute_info(attributeName) != null);
        }
        return attributeOk;
    }

    /**
     * Reads an attribute quality and returns it as an <code>int</code>
     * 
     * @param attribute The {@link DeviceAttribute} associated with the expected attribute
     * @return An <code>int</code>
     * @throws DevFailed If a problem occurred while trying to access the corresponding attribute
     */
    public static int getAttributeQuality(DeviceAttribute attribute) throws DevFailed {
        int result = AttrQuality._ATTR_INVALID;
        if (attribute != null) {
            AttrQuality quality = attribute.getQuality();
            if (quality != null) {
                result = quality.value();
            }
        }
        return result;
    }

    /**
     * Reads an attribute quality and returns it as a {@link String}
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return A {@link String}
     */
    public static String getAttributeStringQuality(String deviceName, String attributeName) {
        String quality = QualityUtilities.getNameForQuality(AttrQuality.ATTR_INVALID);
        try {
            quality = getAttributeStringQuality(getDeviceAttribute(deviceName, attributeName));
        } catch (DevFailed e) {
            StringBuilder builder = new StringBuilder(CANNOT_READ_QUALITY_OF);
            builder.append(deviceName).append('/').append(attributeName);
            System.err.println(builder.toString() + " " + TangoExceptionHelper.getErrorMessage(e));
        }
        return quality;
    }

    /**
     * Reads an attribute quality and returns it as an <code>int</code>
     * 
     * @param deviceName The name of the device that contains the concerned attribute
     * @param attributeName The name of the attribute
     * @return an <code>int</code>
     */
    public static int getAttributeQuality(String deviceName, String attributeName) {
        int quality = AttrQuality._ATTR_INVALID;
        DeviceProxy proxy = TangoDeviceHelper.getDeviceProxy(deviceName, false);
        if (isAttributeRunning(proxy, attributeName)) {
            try {
                quality = getAttributeQuality(getDeviceAttribute(proxy, attributeName));
            } catch (DevFailed e) {
                StringBuilder builder = new StringBuilder(CANNOT_READ_QUALITY_OF);
                builder.append(deviceName).append('/').append(attributeName);
                System.err.println(builder.toString() + " " + TangoExceptionHelper.getErrorMessage(e));
            }
        }
        return quality;
    }

    /**
     * Reads an attribute quality and returns it as a {@link String}
     * 
     * @param attribute The {@link DeviceAttribute} associated with the expected attribute
     * @return A {@link String}
     * @throws DevFailed If a problem occurred while trying to access the corresponding attribute
     */
    public static String getAttributeStringQuality(DeviceAttribute attribute) throws DevFailed {
        String quality = QualityUtilities.getNameForQuality(AttrQuality.ATTR_INVALID);
        if (attribute != null) {
            AttrQuality attrQuality = attribute.getQuality();
            if (attrQuality != null) {
                quality = QualityUtilities.getNameForQuality(attrQuality);
            }
        }
        return quality;
    }

    /**
     * Creates a {@link DeviceAttribute} for a given attribute complete name
     * 
     * @param attributeCompleteName The attribute complete name
     * @return A {@link DeviceAttribute}
     * @throws DevFailed If a problem occurred while trying to access the corresponding attribute
     */
    public static DeviceAttribute getDeviceAttribute(String attributeCompleteName) throws DevFailed {
        DeviceAttribute attribute = null;
        if (attributeCompleteName != null) {
            int index = attributeCompleteName.lastIndexOf('/');
            if (index > -1) {
                attribute = getDeviceAttribute(attributeCompleteName.substring(0, index),
                        attributeCompleteName.substring(index + 1));
            }
        }
        return attribute;
    }

    /**
     * Creates a {@link DeviceAttribute} for a given device name attribute name
     * 
     * @param deviceName The device name
     * @param attributeName The attribute name
     * @return A {@link DeviceAttribute}
     * @throws DevFailed If a problem occurred while trying to access the corresponding attribute
     */
    public static DeviceAttribute getDeviceAttribute(String deviceName, String attributeName) throws DevFailed {
        return getDeviceAttribute(TangoDeviceHelper.getDeviceProxy(deviceName, false), attributeName);
    }

    /**
     * Creates a {@link DeviceAttribute} for a given {@link DeviceProxy} attribute name
     * 
     * @param device The {@link DeviceProxy}
     * @param attributeName The attribute name
     * @return A {@link DeviceAttribute}
     * @throws DevFailed If a problem occurred while trying to access the corresponding attribute
     */
    public static DeviceAttribute getDeviceAttribute(DeviceProxy device, String attributeName) throws DevFailed {
        DeviceAttribute result = null;
        if (device != null) {
            // JAVAAPI-603: synchronize accesses to DeviceProxy
            synchronized (device) {
                result = device.read_attribute(attributeName);
            }
        }
        return result;
    }

    /**
     * Converts a {@link String} value into a <code>short</code> value, based on an enum attribute's possible values.
     * 
     * @param value The {@link String} value.
     * @param ex The {@link AttributeInfoEx} that knows the possible values.
     * @param defaultValue The default value to return if the conversion could not be done.
     * @return A <code>short</code> value, that may be written in the attribute.<br />
     */
    public static short toEnumValue(String value, AttributeInfoEx ex, short defaultValue) {
        short index = defaultValue;
        if ((ex != null) && (ex.data_type == TangoConst.Tango_DEV_ENUM) && (value != null)) {
            String[] possibleValues = ex.enum_label;
            if (possibleValues != null) {
                index = 0;
                for (String possibleValue : possibleValues) {
                    if (value.equalsIgnoreCase(possibleValue)) {
                        break;
                    } else {
                        index++;
                    }
                }
                if (index == possibleValues.length) {
                    index = defaultValue;
                }
            }
        }
        return index;
    }

    /**
     * Converts a {@link String} value into a {@link Short} value, based on an enum attribute's possible values.
     * 
     * @param value The {@link String} value.
     * @param ex The {@link AttributeInfoEx} that knows the possible values.
     * @return A {@link Short} value, that can be written in the attribute.
     */
    public static Short toEnumValue(String value, AttributeInfoEx ex) {
        short index = toEnumValue(value, ex, (short) -1);
        return index > -1 ? Short.valueOf(index) : null;
    }

    /**
     * Converts a {@link Short} value into a {@link String} value, based on an enum attribute's possible values.
     * 
     * @param value The {@link Short} value.
     * @param ex The {@link AttributeInfoEx} that knows the possible values.
     * @return A {@link String} value: the enum matching label.
     */
    public static String toEnumLabel(short value, AttributeInfoEx ex) {
        String result = null;
        if ((ex != null) && (ex.data_type == TangoConst.Tango_DEV_ENUM) && (value > -1)) {
            String[] possibleValues = ex.enum_label;
            if (possibleValues != null) {
                int index = value;
                if (index < possibleValues.length) {
                    result = possibleValues[index];
                }
            }
        }
        return result;
    }

}
