package org.phoebus.pv.tango;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.tango.utils.TangoUtil;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.DevState;
import fr.esrf.Tango.DevVarLongStringArray;
import fr.esrf.TangoApi.Database;
import fr.esrf.TangoApi.DbDatum;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoApi.DeviceInfo;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoApi.DeviceProxyFactory;
import fr.esrf.TangoApi.StateUtilities;

public class TangoDeviceHelper {
    public static final String SLASH = "/";

    public static final String ANY = "*";
    private static final String ALL_DEVICES_QUERY = "select name from device where class='%s' order by name";
    private static final String SQL_SELECT = "DbMySqlSelect";
    private static final String CANNOT_GET_DEVICE_CLASS_OF = "Cannot get device class of ";
    private static final String CANNOT_GET_EXPORTED_DEVICES_OF_CLASS = "Cannot get exported devices of class ";
    private static final String CANNOT_GET_ALL_DEVICES_OF_CLASS = "Cannot get all devices of class ";
    private static final String CANNOT_READ_STATUS_ON = "Cannot read status on ";
    private static final String CANNOT_READ_STATE_ON = "Cannot read state on ";
    private static final String CANNOT_CREATE_DATABASE_DEVICE = "Cannot create database device";
    private static final String CANNOT_CREATE_DB_DATUM_ON_DEVICE_PROPERTY = "Cannot create DbDatum on device property";
    private static final String CANNOT_READ_PROPERTIES_OF_DEVICE = "Cannot read properties of device ";
    private static final String STATE = "State";
    private static final String NEW_LINE = ":\n";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String DEVICE = "Device '";
    private static final String IS_NOT_EXPORTED = "' is not exported";
    private static final String SEEMS_TO_BE_DOWN = "' seems to be down";
    private static final String UNEXPECTED_ERROR_ON_CLEAN_DEVICE = "Unexpected error on clean device";
    private static final String[] EMPTY = new String[0];
    private static Database database = null;
    private static Boolean nodatabase = null;
    
    private static final Object DATABASE_LOCK = new Object();
    private static final List<String> BAD_DEVICES = new ArrayList<>();
    protected static final int DEFAULT_TIMEOUT = 3000;

    // Once a device is dead, we prefer waiting 20s before checking its availability.
    // The following 2 fields are here for that purpose
    protected static final Map<String, DeadDeviceCheck> LAST_DEAD_TIME_MAP = new ConcurrentHashMap<>();

    // Map that stores the devices desired timeout (user defined)
    protected static final Map<String, Integer> TIME_OUT_MAP = new ConcurrentHashMap<>();

    /**
     * Returns the device class for given device.
     * 
     * @param deviceName The device name.
     * @return A {@link String}. May be <code>null</code>.
     */
    public static String getDeviceClass(String deviceName) {
        String className = null;
        try {
            String device = recoverDeviceRealName(deviceName);
            if (device != null) {
                Database db = getDatabase();
                className = db == null ? null : db.get_class_for_device(device);
            }
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder(CANNOT_GET_DEVICE_CLASS_OF);
            builder.append(deviceName);
            if (e instanceof DevFailed) {
                DevFailed df = (DevFailed) e;
                builder.append('\n').append(TangoExceptionHelper.getErrorMessage(df));
            }
        }
        return className;
    }

    /**
     * Returns the known exported devices of given class according to given {@link Database}.
     * 
     * @param className The class name.
     * @param db The {@link Database}.
     * @return A {@link String} array, never <code>null</code>.
     */
    public static String[] getExportedDevicesOfClass(String className, Database db) {
        String[] devices;
        try {
            if ((className == null) || className.isEmpty() || (db == null)) {
                devices = EMPTY;
            } else {
                devices = db.get_device_exported_for_class(className);
            }
        } catch (Exception e) {
            devices = EMPTY;
            StringBuilder builder = new StringBuilder(CANNOT_GET_EXPORTED_DEVICES_OF_CLASS);
            builder.append(className);
            if (e instanceof DevFailed) {
                DevFailed df = (DevFailed) e;
                builder.append('\n').append(TangoExceptionHelper.getErrorMessage(df));
            }
        }
        return devices;
    }

    /**
     * Returns the known exported devices of given class.
     * 
     * @param className The class name.
     * @return A {@link String} array, never <code>null</code>.
     */
    public static String[] getExportedDevicesOfClass(String className) {
        return getExportedDevicesOfClass(className, getDatabase());
    }

    /**
     * Returns all known devices (exported or not) of given class according to given {@link Database}.
     * 
     * @param className The class name.
     * @param db The {@link Database}.
     * @return A {@link String} array, never <code>null</code>.
     */
    public static String[] getAllDevicesOfClass(String className, Database db) {
        String[] devices;
        try {
            if ((className == null) || className.isEmpty() || (db == null)) {
                devices = EMPTY;
            } else {
                DeviceData commandArgument = new DeviceData();
                commandArgument.insert(String.format(ALL_DEVICES_QUERY, className));
                DeviceData commandResult = db.command_inout(SQL_SELECT, commandArgument);
                if (commandResult == null) {
                    devices = EMPTY;
                } else {
                    DevVarLongStringArray array = commandResult.extractLongStringArray();
                    if ((array == null) || (array.svalue == null)) {
                        devices = EMPTY;
                    } else {
                        devices = array.svalue;
                    }
                }
            }
        } catch (Exception e) {
            devices = EMPTY;
            StringBuilder builder = new StringBuilder(CANNOT_GET_ALL_DEVICES_OF_CLASS);
            builder.append(className);
            if (e instanceof DevFailed) {
                DevFailed df = (DevFailed) e;
                builder.append('\n').append(TangoExceptionHelper.getErrorMessage(df));
            }
        }
        return devices;
    }

    /**
     * Returns all known devices (exported or not) of given class.
     * 
     * @param className The class name.
     * @return A {@link String} array, never <code>null</code>.
     */
    public static String[] getAllDevicesOfClass(String className) {
        return getAllDevicesOfClass(className, getDatabase());
    }

    /**
     * Recovers the full name of given device.
     * 
     * @param deviceName The device name or alias.
     * @return A {@link String}. May be <code>null</code>.
     * @throws DevFailed If a problem occurred.
     */
    protected static String recoverDeviceRealName(String deviceName) throws DevFailed {
        String device;
        if (deviceName == null) {
            device = null;
        } else {
            device = deviceName.trim();
            if (device.isEmpty()) {
                device = null;
            } else {
                device = TangoUtil.getfullNameForDevice(device);
            }
        }
        if (device != null) {
            device = device.toLowerCase();
        }
        return device;
    }

    /**
     * Recovers the status of given device.
     * 
     * @param deviceName The device name.
     * @return A {@link String}. Never <code>null</code>.
     */
    public static String getDeviceStatus(String deviceName) {
        String status = UNKNOWN;
        DeviceProxy proxy = getDeviceProxy(deviceName, false);
        if (proxy != null) {
            try {
                // JAVAAPI-603: synchronize accesses to DeviceProxy
                synchronized (proxy) {
                    status = proxy.status();
                }
            } catch (DevFailed e) {
                StringBuilder builder = new StringBuilder(CANNOT_READ_STATUS_ON);
                builder.append(deviceName).append('\n');
                builder.append(TangoExceptionHelper.getErrorMessage(e));
                status = UNKNOWN;
            }
        }
        return status;
    }

    /**
     * Recovers the state of given device.
     * 
     * @param deviceName The device name.
     * @return A {@link String}. Never <code>null</code>.
     */
    public static String getDeviceState(String deviceName) {
        String state = StateUtilities.getNameForState(DevState.UNKNOWN);
        try {
            state = getDeviceStateWithError(deviceName);
        } catch (DevFailed e) {
            state = StateUtilities.getNameForState(DevState.UNKNOWN);
            StringBuilder builder = new StringBuilder(CANNOT_READ_STATE_ON);
            builder.append(deviceName).append('\n');
            builder.append(TangoExceptionHelper.getErrorMessage(e));
        }
        return state;
    }

    /**
     * Recovers the state for given device, throwing encountered {@link DevFailed} if any.
     * 
     * @param deviceName The device name.
     * @return A {@link String}. Never <code>null</code>.
     * @throws DevFailed If a problem occurred.
     */
    public static String getDeviceStateWithError(String deviceName) throws DevFailed {
        String state = StateUtilities.getNameForState(DevState.UNKNOWN);
        DeviceProxy proxy = getDeviceProxy(deviceName, false);
        if (proxy != null) {
            DeviceData dData;
            // JAVAAPI-603: synchronize accesses to DeviceProxy
            synchronized (proxy) {
                dData = proxy.command_inout(STATE);
            }
            if (dData != null) {
                DevState dState = dData.extractDevState();
                state = StateUtilities.getNameForState(dState);
            }
        }
        return state;
    }

    /**
     * Returns the known {@link Database}.
     * 
     * @return A {@link Database}. May be <code>null</code>.
     */
    public static Database getDatabase() {
        // double check with lock to ensure thread safety
        if (database == null) {
            try {
                synchronized (DATABASE_LOCK) {
                    if (database == null) {
                        try {
                            database = new Database();
                        } catch (Exception e) {
                            StringBuilder builder = new StringBuilder(CANNOT_CREATE_DATABASE_DEVICE);
                            if (e instanceof DevFailed) {
                                builder.append(NEW_LINE);
                                builder.append(TangoExceptionHelper.getErrorMessage(e));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return database;
    }

    /**
     * Returns the known timeout, expressed in milliseconds, for given device.
     * 
     * @param device The device name.
     * @return An <code>int</code>.
     */
    public static int getTimeOutInMilliseconds(String device) {
        int timeOut;
        device = recoverDeviceName(device);
        if (device == null) {
            timeOut = DEFAULT_TIMEOUT;
        } else {
            Integer tmp = TIME_OUT_MAP.get(device);
            timeOut = tmp == null ? DEFAULT_TIMEOUT : tmp.intValue();
        }
        return timeOut;
    }

    /**
     * Sets the timeout, expressed in milliseconds, for given device.
     * 
     * @param device The device name.
     * @param timeOut The timeout to set. If <code>timeOut &lt; 1</code>, then default timeout (i.e. 3s) is applied.
     */
    public static void setTimeOutInMilliseconds(String device, int timeOut) {
        device = recoverDeviceName(device);
        if (device != null) {
            if (timeOut < 1) {
                timeOut = DEFAULT_TIMEOUT;
            }
            TIME_OUT_MAP.put(device, Integer.valueOf(timeOut));
            getDeviceProxy(device, false);
        }
    }

    /**
     * Returns the {@link DeadDeviceCheck} for given device.
     * 
     * @param device The device name.
     * @return A {@link DeadDeviceCheck}. Never <code>null</code>.
     */
    protected static DeadDeviceCheck getDeadDeviceCheck(String device) {
        DeadDeviceCheck deviceCheck = LAST_DEAD_TIME_MAP.get(device);
        if (deviceCheck == null) {
            deviceCheck = new DeadDeviceCheck();
            DeadDeviceCheck tmp = LAST_DEAD_TIME_MAP.putIfAbsent(device, deviceCheck);
            if (tmp != null) {
                deviceCheck = tmp;
            }
        }
        return deviceCheck;
    }

    /**
     * Returns the {@link DeviceProxy} for given device, managing known timeout.
     * 
     * @param device The device name.
     * @return A {@link DeviceProxy}.
     * @throws DevFailed If a problem occurred.
     */
    protected static DeviceProxy getDeviceProxyWithTimeOut(String device) throws DevFailed {
        DeviceProxy proxy;
        if (device == null) {
            proxy = null;
        } else {
            int timeOut = getTimeOutInMilliseconds(device);
            // We use a DeadDeviceCheck to try to limit the access failures: PROBLEM-1820 and PROBLEM-1715.
            DeadDeviceCheck deviceCheck = getDeadDeviceCheck(device);
            // We take a synchronized access to limit the number of access attempt at the same time.
            synchronized (deviceCheck) {
                if ((System.currentTimeMillis() - deviceCheck.getLastCheckedDate() < deviceCheck.getTimeOutMultiplier()
                        * timeOut)) {
                    // If the availability check comes too early, consider DeviceProxy as dead --> null.
                    proxy = null;
                } else {
                    // The availability check is not too early: really try to get DeviceProxy.
                    try {
                        proxy = DeviceProxyFactory.get(device);
                        if (proxy != null) {
                            // JAVAAPI-603: synchronize accesses to DeviceProxy
                            synchronized (proxy) {
                                if (timeOut != proxy.get_timeout_millis()) {
                                    proxy.set_timeout_millis(timeOut);
                                }
                            }
                        }
                        // Consider device as back available
                        if (deviceCheck.getLastCheckedDate() != 0) {
                            deviceCheck.reset();
                        }
                    } catch (DevFailed df) {
                        // Consider device as dead
                        proxy = null;
                        // Updates last device check date, which increases the timeout multiplier.
                        deviceCheck.updateLastCheckedDate(System.currentTimeMillis());
                        throw df;
                    }
                }
            }
        }
        return proxy;
    }

    /**
     * Returns the {@link DeviceProxy} for given device, potentially testing whether it is running.
     * 
     * @param key The device name.
     * @param testIfRunning Whether to do the tests for running device.
     * @return A {@link DeviceProxy}. May be <code>null</code>.
     */
    public static DeviceProxy getDeviceProxy(String key, boolean testIfRunning) {
        DeviceProxy proxy = null;
        if ((key != null) && (!key.trim().isEmpty())) {
            if ((!testIfRunning) || isDeviceRunning(key)) {
                String device = key.toLowerCase();
                Exception error = null;
                try {
                    proxy = getDeviceProxyWithTimeOut(device);
                } catch (Exception e) {
                    proxy = null;
                    error = e;
                }
                if (proxy == null) {
                    cleanDevice(device, getDownDeviceErrorMessage(device), error);
                } else {
                    setBackToGood(device);
                }
            }
        }
        return proxy;
    }

    /**
     * Returns the {@link DeviceProxy} for given device, testing whether it is running.
     * 
     * @param key The device name.
     * @return A {@link DeviceProxy}. May be <code>null</code>.
     */
    public static DeviceProxy getDeviceProxy(String key) {
        return getDeviceProxy(key, true);
    }

    /**
     * Consider given device as running (i.e. not dead).
     * 
     * @param key The device name.
     */
    protected static void setBackToGood(String key) {
        if (key != null) {
            String device = key.toLowerCase();
            synchronized (BAD_DEVICES) {
                BAD_DEVICES.remove(device);
            }
            LAST_DEAD_TIME_MAP.remove(device);
        }
    }

    /**
     * Returns whether given device is dead (i.e. not running).
     * 
     * @param key The device name.
     * @return A <code>boolean</code>.
     */
    public static boolean isDeadDevice(String key) {
        boolean bad = false;
        if (key != null) {
            String device = key.toLowerCase();
            synchronized (BAD_DEVICES) {
                bad = BAD_DEVICES.contains(device);
            }
        }
        return bad;
    }

    /**
     * Recovers the full name of given device.
     * 
     * @param deviceAliasOrName The device name or alias.
     * @return A {@link String}. <code>null</code> if <code>deviceAliasOrName</code> is <code>null</code>.
     */
    public static String recoverDeviceName(final String deviceAliasOrName) {
        String deviceName;
        try {
            deviceName = recoverDeviceRealName(deviceAliasOrName);
            if (deviceName == null) {
                deviceName = deviceAliasOrName;
            }
        } catch (DevFailed df) {
            deviceName = deviceAliasOrName;
        }
        return deviceName;
    }

    /**
     * Recovers the device name for given entity (attribute or command).
     * 
     * @param fullEntityName The entity full name.
     * @return A {@link String}. May be <code>null</code>.
     */
    public static String getDeviceName(String fullEntityName) {
        String deviceName = null;
        if (fullEntityName != null) {
            fullEntityName = fullEntityName.trim();
            if (!fullEntityName.isEmpty()) {
                try {
                    // TangoUtil attribute methods are in fact compatible with command names
                    deviceName = TangoUtil
                            .getfullDeviceNameForAttribute(TangoUtil.getfullAttributeNameForAttribute(fullEntityName));
                } catch (Exception e) {
                    deviceName = null;
                }
            }
        }
        return deviceName;
    }

    /**
     * Recovers the entity (attribute or command) name for given entity full name.
     * 
     * @param fullEntityName The entity full name.
     * @return A {@link String}. May be <code>null</code>.
     */
    public static String getEntityName(String fullEntityName) {
        String entityName = fullEntityName;
        if (entityName != null) {
            entityName = entityName.trim();
            if (!entityName.isEmpty()) {
                String tmp = entityName;
                try {
                    // TangoUtil.getAttributeName is in fact compatible with command names
                    entityName = TangoUtil.getAttributeName(tmp);
                } catch (Exception e) {
                    entityName = tmp;
                }
            }
        }
        return entityName;
    }

    /**
     * Returns whether given device exists.
     * 
     * @param deviceName The device name.
     * @return A <code>boolean</code>.
     */
    public static boolean deviceExists(String deviceName) {
        boolean exists;
        Database database = getDatabase();
        String device;
        try {
            device = recoverDeviceRealName(deviceName);
        } catch (Exception e) {
            device = null;
        }
        if ((database != null) && (device != null)) {
            try {
                DeviceInfo deviceInfo = database.get_device_info(device);
                exists = (deviceInfo != null);
            } catch (DevFailed e) {
                exists = false;
            }
        } else {
            exists = false;
        }
        return exists;
    }
    
    /**
     * Returns the known {@link Database}.
     * 
     * @return A {@link Database}. May be <code>null</code>.
     */
    public static boolean isNoDataBase() {
        if(nodatabase == null) {
            nodatabase = getDatabase() == null;
        }
        return nodatabase;
    }



    /**
     * Returns whether given device is running.
     * 
     * @param deviceName The device name.
     * @return A <code>boolean</code>.
     */
    public static boolean isDeviceRunning(String deviceName) {
        boolean running;
        if ((deviceName != null) && (!deviceName.trim().isEmpty())) {
            String device = deviceName.toLowerCase();
            try {
                boolean exported;
                if (isNoDataBase()) {
                    exported = true;
                } else {
                    Database database = getDatabase();
                    String tmp;
                    try {
                        tmp = recoverDeviceRealName(deviceName);
                    } catch (Exception e) {
                        tmp = null;
                    }
                    device = tmp;
                    if ((database == null) || (device == null)) {
                        exported = false;
                    } else {
                        DeviceInfo deviceInfo = database.get_device_info(device);
                        // If the device is exported
                        exported = deviceInfo.exported;
                    }
                }
                if (exported) {
                    try {
                        final DeviceProxy proxy = getDeviceProxyWithTimeOut(device);
                        if (proxy == null) {
                            running = false;
                            cleanDevice(device, getDownDeviceErrorMessage(device), null);
                        } else {
                            // DO NOT CALL our getDeviceState() method
                            // because it will call isDeviceRunning()
                            // JAVAAPI-603: synchronize accesses to DeviceProxy
                            synchronized (proxy) {
                                proxy.ping();
                            }
                            running = true;
                            setBackToGood(device);
                        }
                    } catch (Exception e) {
                        // ZOMBIE devices -> MANTIS 0021917
                        // database.unexport_device(deviceName);
                        // Reconnexion bug in jacorb 21887 note 68002
                        // deviceProxyTable.remove(deviceName.toLowerCase());
                        running = false;
                        cleanDevice(device, getDownDeviceErrorMessage(device), /*e*/null);
                    }
                } else {
                    running = false;
                    cleanDevice(device, DEVICE + device + IS_NOT_EXPORTED, null);
                }
            } catch (Exception e) {
                cleanDevice(device, DEVICE + device + IS_NOT_EXPORTED, e);
                running = false;
            }
        } else {
            running = false;
        }
        return running;
    }

    /**
     * Returns the error message that indicates given device is down.
     * 
     * @param device The device name.
     * @return A {@link String}.
     */
    protected static String getDownDeviceErrorMessage(String device) {
        return DEVICE + device + SEEMS_TO_BE_DOWN;
    }

    /**
     * Completely forgets given device, indicated potentially encountered error.
     * <p>
     * This method is useful in case of network connection problems.
     * </p>
     * 
     * @param deviceNameToLowerCase The device name, already lower cased.
     * @param errorMessage The error message.
     * @param error The error.
     */
    protected static void cleanDevice(String deviceNameToLowerCase, String errorMessage, Throwable error) {
        // No need to take lock on DEVICE_LOCK as calls to this method are surrounded with this lock
        if ((deviceNameToLowerCase != null) && (!deviceNameToLowerCase.trim().isEmpty())) {
            boolean canLog = false;
            // Tango NullPointerException workaround (Mantis 25468 & 25743)
            try {
                DeviceProxyFactory.remove(deviceNameToLowerCase);
            } catch (Exception e) {
                logError(e, UNEXPECTED_ERROR_ON_CLEAN_DEVICE);
            }
            synchronized (BAD_DEVICES) {
                if (!BAD_DEVICES.contains(deviceNameToLowerCase)) {
                    BAD_DEVICES.add(deviceNameToLowerCase);
                    canLog = true;
                }
            }
            if (canLog) {
                logError(error, errorMessage);
            }
            DeadDeviceCheck deviceCheck = getDeadDeviceCheck(deviceNameToLowerCase);
            synchronized (deviceCheck) {
                if (deviceCheck.getLastCheckedDate() == 0) {
                    deviceCheck.updateLastCheckedDate(System.currentTimeMillis());
                }
            }
        }
    }

    /**
     * Traces an error message in logs.
     * 
     * @param error The error.
     * @param errorMessage The error message.
     */
    protected static void logError(Throwable error, String errorMessage) {
        if (error == null) {
            System.err.println(errorMessage);
        } else if (error instanceof DevFailed) {
            System.err.println(errorMessage + '\n' + TangoExceptionHelper.getErrorMessage(error));
        } else {
            System.err.println(errorMessage + " " + error.getMessage());
        }
    }

    /**
     * Returns the type (scalar, array or unknown) of given device propeperty.
     * 
     * @param deviceName The device name.
     * @param propertyName The property name.
     * @return An <code>int</code>. Can be any of these:
     *         <ul>
     *         <li>{@link TangoConstHelper#SCALAR_TYPE}</li>
     *         <li>{@link TangoConstHelper#ARRAY_TYPE}</li>
     *         <li>{@link TangoConstHelper#UNKNOW_TYPE}</li>
     *         </ul>
     */
    public static int getPropertyType(String deviceName, String propertyName) {
        int type = TangoConstHelper.UNKNOW_TYPE;
        DbDatum dbDatum = createDbDatum(deviceName, propertyName);
        if (dbDatum != null) {
            if (dbDatum.size() == 1) {
                type = TangoConstHelper.SCALAR_TYPE;
            }
            if (dbDatum.size() > 1) {
                type = TangoConstHelper.ARRAY_TYPE;
            }
        }
        return type;
    }

    /**
     * Returns whether given device has given property.
     * 
     * @param deviceName The device name.
     * @param propertyName The property name.
     * @return A <code>boolean</code>.
     */
    public static boolean hasProperty(String deviceName, String propertyName) {
        return getOriginalPropertyName(deviceName, propertyName, true) != null;
    }

    /**
     * Recovers the case sensitive property name for given device.
     * 
     * @param deviceName The device name.
     * @param propertyName The property name (case insensitive).
     * @param nullIfNonExisting Whether to return <code>null</code> if such property doesn't exist.
     * @return A {@link String}: the property name respecting the original case.
     */
    public static String getOriginalPropertyName(String deviceName, String propertyName, boolean nullIfNonExisting) {
        String name = nullIfNonExisting ? null : propertyName;
        if ((deviceName != null) && (!deviceName.isEmpty()) && (propertyName != null) && (!propertyName.isEmpty())) {
            try {
                String device = recoverDeviceRealName(deviceName);
                if (device != null) {
                    Database db = getDatabase();
                    String[] properties = db == null ? null : db.get_device_property_list(deviceName, ANY);
                    if ((properties != null) && (properties.length > 0)) {
                        for (String property : properties) {
                            if ((property != null) && (propertyName.equalsIgnoreCase(property))) {
                                name = property;
                                break;
                            }
                        }
                    }
                }
            } catch (DevFailed df) {
                logError(df, CANNOT_READ_PROPERTIES_OF_DEVICE + deviceName);
            }
        }
        return name;
    }

    /**
     * Recovers given device property.
     * 
     * @param deviceName The device name.
     * @param propertyName The property name.
     * @return A {@link DbDatum}.
     */
    public static DbDatum createDbDatum(String deviceName, String propertyName) {
        DbDatum dbDatum = null;
        if ((deviceName != null) && (!deviceName.isEmpty()) && (propertyName != null) && (!propertyName.isEmpty())) {
            try {
                String device = recoverDeviceRealName(deviceName);
                if (device != null) {
                    Database db = getDatabase();
                    dbDatum = db == null ? null : db.get_device_property(device, propertyName);
                }
            } catch (DevFailed df) {
                logError(df, CANNOT_CREATE_DB_DATUM_ON_DEVICE_PROPERTY);
            }
        }
        return dbDatum;
    }

    // ///////////// //
    // Inner classes //
    // ///////////// //

    /**
     * A class to manage dead devices.
     * <p>
     * It stores the last device check date (the last time there was an attempt to access to the device) and the value
     * by which to multiply the timeout to obtain the delay before next allowed check.
     * </p>
     * 
     * @author GIRARDOT
     */
    protected static class DeadDeviceCheck {
        private volatile long lastCheckedDate;
        private volatile int timeOutMultiplier;

        public DeadDeviceCheck() {
            reset();
        }

        /**
         * Updates the last checked date, increasing the multiply value by one.
         * 
         * @param date The last checked date.
         */
        public void updateLastCheckedDate(long date) {
            lastCheckedDate = date;
            timeOutMultiplier = Math.min(timeOutMultiplier + 1, 7);
        }

        /**
         * Returns the last checked date (the date at which the last access attempt happened).
         * 
         * @return A <code>long</code>.
         */
        public long getLastCheckedDate() {
            return lastCheckedDate;
        }

        /**
         * Returns the value by which to multiply device timeout in order to obtain the time to wait before next allowed
         * device check.
         * 
         * @return An <code>int</code>.
         */
        public int getTimeOutMultiplier() {
            return timeOutMultiplier;
        }

        /**
         * Resets this {@link DeadDeviceCheck}. Call it when you successfully access to the device.
         */
        public void reset() {
            lastCheckedDate = 0;
            timeOutMultiplier = 1;
        }
    }

}
