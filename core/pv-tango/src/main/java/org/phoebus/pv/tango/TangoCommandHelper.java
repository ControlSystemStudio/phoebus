package org.phoebus.pv.tango;

import java.util.ArrayList;
import java.util.List;

import org.tango.utils.TangoUtil;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.CommandInfo;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoApi.DeviceProxy;
import fr.esrf.TangoDs.TangoConst;
import fr.soleil.tango.clientapi.InsertExtractUtils;
import fr.soleil.tango.clientapi.util.TypeConversionUtil;



/**
 * 
 * This class provide useful static methods to insert and extract result from Command
 * 
 *  @author katy.saintin@cea.fr
 */
public class TangoCommandHelper {
    private static final String CANNOT_READ_COMMAND_INFO_ON = "Cannot read CommandInfo on ";
    private static final String CANNOT_READ_COMMAND_LIST_QUERY_ON = "Cannot read command_list_query on ";
    private static final String FAILED_TO_READ_COMMANDS_DEAD_DEVICE = "Failed to read commands from %s because device seems to be down.";
    private static final String FAILED_TO_READ_COMMANDS_UNKNOWN_REASON_DEVICE = "Failed to read commands from %s for an unknown reason concerning the device.";
    private static final String FAILED_TO_READ_COMMANDS_UNKNOWN_REASON = "Failed to read commands from %s for an unknown reason.";

    /**
     * Returns an <code>int</code> that represents the type of data a command can have as argument.
     * 
     * @param deviceName The device name.
     * @param commandName The command name.
     * @return An <code>int</code>.
     * @see TangoConst
     */
    public static int getCommandInType(String deviceName, String commandName) {
        // XXX Must not be TangoConstHelper.UNKNOW_TYPE.
        // TangoConstHelper.UNKNOW_TYPE = 3, which matches TangoConst.Tango_DEV_LONG
        int type = -1;
        CommandInfo info = getCommandInfo(deviceName, commandName);
        if (info != null) {
            type = info.in_type;
        }
        return type;
    }

    /**
     * Returns an <code>int</code> that represents the type of data a command can have as argument.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @return An <code>int</code>.
     * @see TangoConst
     */
    public static int getCommandInType(DeviceProxy proxy, String commandName) {
        // XXX Must not be TangoConstHelper.UNKNOW_TYPE.
        // TangoConstHelper.UNKNOW_TYPE = 3, which matches TangoConst.Tango_DEV_LONG
        return getCommandInType(proxy, commandName, -1/*TangoConstHelper.UNKNOW_TYPE*/);
    }

    /**
     * Returns an <code>int</code> that represents the type of data a command can have as argument.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @param defaultValue The default value to return if something went wrong.
     * @return An <code>int</code>.
     * @see TangoConst
     */
    public static int getCommandInType(DeviceProxy proxy, String commandName, int defaultValue) {
        int type = defaultValue;
        CommandInfo info = getCommandInfo(proxy, commandName);
        if (info != null) {
            type = info.in_type;
        }
        return type;
    }

    /**
     * Returns an <code>int</code> that represents the type of data a command can return.
     * 
     * @param deviceName The device name.
     * @param commandName The command name.
     * @return An <code>int</code>.
     * @see TangoConst
     */
    public static int getCommandOutType(String deviceName, String commandName) {
        // XXX Must not be TangoConstHelper.UNKNOW_TYPE.
        // TangoConstHelper.UNKNOW_TYPE = 3, which matches TangoConst.Tango_DEV_LONG
        int type = -1;
        CommandInfo info = getCommandInfo(deviceName, commandName);
        if (info != null) {
            type = info.out_type;
        }
        return type;
    }

    /**
     * Returns an <code>int</code> that represents the type of data a command can return.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @return An <code>int</code>.
     * @see TangoConst
     */
    public static int getCommandOutType(DeviceProxy proxy, String commandName) {
        // XXX Must not be TangoConstHelper.UNKNOW_TYPE.
        // TangoConstHelper.UNKNOW_TYPE = 3, which matches TangoConst.Tango_DEV_LONG
        return getCommandOutType(proxy, commandName, -1/*TangoConstHelper.UNKNOW_TYPE*/);
    }

    /**
     * Returns an <code>int</code> that represents the type of data a command can return.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @param defaultValue The default value to return if something went wrong.
     * @return An <code>int</code>.
     * @see TangoConst
     */
    public static int getCommandOutType(DeviceProxy proxy, String commandName, int defaultValue) {
        int type = defaultValue;
        CommandInfo info = getCommandInfo(proxy, commandName);
        if (info != null) {
            type = info.out_type;
        }
        return type;
    }

    /**
     * Returns the {@link CommandInfo} of a command.
     * 
     * @param deviceName The device name.
     * @param commandName The command name.
     * @return A {@link CommandInfo}.
     */
    public static CommandInfo getCommandInfo(String deviceName, String commandName) {
        return getCommandInfo(TangoDeviceHelper.getDeviceProxy(deviceName), commandName);
    }

    /**
     * Returns the {@link CommandInfo} of a command.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @return A {@link CommandInfo}.
     */
    public static CommandInfo getCommandInfo(DeviceProxy proxy, String commandName) {
        CommandInfo commandInfo = null;
        if ((proxy != null) && doesCommandExist(proxy, commandName)) {
            try {
                // JAVAAPI-603: synchronize accesses to DeviceProxy
                synchronized (proxy) {
                    commandInfo = proxy.command_query(commandName);
                }
            } catch (DevFailed e) {
                StringBuilder builder = new StringBuilder(CANNOT_READ_COMMAND_INFO_ON);
                builder.append(proxy.get_name()).append(TangoDeviceHelper.SLASH).append(commandName);
                System.err.println(builder.toString() + " " + TangoExceptionHelper.getErrorMessage(e));
            }
        }
        return commandInfo;
    }

    /**
     * Returns whether a command exists
     * 
     * @param deviceName The device name
     * @param commandName The command name
     * @return Whether the command exists
     */
    public static boolean doesCommandExist(String deviceName, String commandName) {
        boolean exist = false;
        if ((commandName != null) && (!commandName.isEmpty())) {
            String[] commandList = getCommandList(deviceName);
            if (commandList != null) {
                for (String element : commandList) {
                    if (commandName.trim().equalsIgnoreCase(element)) {
                        exist = true;
                        break;
                    }
                }
            }
        }
        return exist;
    }

    /**
     * Returns whether a command exists.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @return Whether the command exists.
     */
    public static boolean doesCommandExist(DeviceProxy proxy, String commandName) {
        boolean exist = false;
        if ((commandName != null) && (!commandName.isEmpty())) {
            String[] commandList = getCommandList(proxy);
            if (commandList != null) {
                for (String element : commandList) {
                    if (commandName.trim().equalsIgnoreCase(element)) {
                        exist = true;
                        break;
                    }
                }
            }
        }
        return exist;
    }

    /**
     * Returns the command list of a given device.
     * 
     * @param deviceName The device name.
     * @param lowerCase Whether to force all returned values as lower case.
     * @return A {@link String} array.
     */
    public static String[] getCommandList(String deviceName, boolean lowerCase) {
        DeviceProxy proxy = TangoDeviceHelper.getDeviceProxy(deviceName);
        String[] commands = getCommandList(proxy, lowerCase);
        if ((commands == null) && (deviceName != null)) {
            if (proxy == null) {
                if (TangoDeviceHelper.isDeadDevice(deviceName)) {
                    System.err.println(String.format(FAILED_TO_READ_COMMANDS_DEAD_DEVICE, deviceName));
                } else {
                    System.err.println(String.format(FAILED_TO_READ_COMMANDS_UNKNOWN_REASON_DEVICE, deviceName));
                }
            } else {
                System.err.println(String.format(FAILED_TO_READ_COMMANDS_UNKNOWN_REASON, deviceName));
            }
        }
        return commands;
    }

    /**
     * Returns the command list of a given device.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param lowerCase Whether to force all returned values as lower case.
     * @return A {@link String} array.
     */
    public static String[] getCommandList(DeviceProxy proxy, boolean lowerCase) {
        String[] commandList = null;
        if (proxy != null) {
            try {
                commandList = getCommandListWithError(proxy, lowerCase);
            } 
            catch (DevFailed e) {
                String errorMessage = CANNOT_READ_COMMAND_LIST_QUERY_ON + " " + proxy.get_name();
                System.err.println(errorMessage + " " + TangoExceptionHelper.getErrorMessage(e));
            }
        }
        return commandList;
    }

    /**
     * Returns the command list of a given device, throwing {@link DevFailed} in case of communication problems.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param lowerCase Whether to force all returned values as lower case.
     * @return A {@link String} array.
     * @throws DevFailed In case of communication problems.
     */
    public static String[] getCommandListWithError(DeviceProxy proxy, boolean lowerCase) throws DevFailed {
        String[] commandList = null;
        if (proxy != null) {
            CommandInfo[] commandInfoList;
            // JAVAAPI-603: synchronize accesses to DeviceProxy
            synchronized (proxy) {
                commandInfoList = proxy.command_list_query();
            }
            commandList = new String[commandInfoList.length];
            for (int i = 0; i < commandInfoList.length; i++) {
                commandList[i] = lowerCase ? commandInfoList[i].cmd_name.toLowerCase() : commandInfoList[i].cmd_name;
            }
        }
        return commandList;
    }

    /**
     * Returns the command list of a given device.
     * 
     * @param deviceName The device name.
     * @return A {@link String} array.
     */
    public static String[] getCommandList(String deviceName) {
        return getCommandList(deviceName, false);
    }

    /**
     * Returns the command list of a given device.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @return A {@link String} array.
     */
    public static String[] getCommandList(DeviceProxy proxy) {
        return getCommandList(proxy, false);
    }

    /**
     * Executes a command and returns its result.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @param arg The argument to give to the command.
     * @return The command result.
     * @throws DevFailed If something went wrong.
     */
    public static Object executeCommand(final DeviceProxy proxy, final String commandName, final Object arg)
            throws DevFailed {
        return executeCommand(proxy, commandName, getCommandInType(proxy, commandName),
                getCommandOutType(proxy, commandName), arg);
    }

    /**
     * Executes a command and returns its result.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @param expectedResultClass The expected result class.
     * @param arg The argument to give to the command.
     * @return The command result.
     * @throws DevFailed If something went wrong.
     */
    public static <T> T executeCommand(final DeviceProxy proxy, final String commandName,
            final Class<T> expectedResultClass, final Object arg) throws DevFailed {
        return executeCommand(proxy, commandName, getCommandInType(proxy, commandName),
                getCommandOutType(proxy, commandName), expectedResultClass, arg);
    }

    /**
     * Executes a command and returns its result.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @param commandInType The command input type, that can be obtained through <code>getCommandInType</code> methods.
     * @param commandOutType The command output type, that can be obtained through <code>getCommandOutType</code>
     *            methods.
     * @param arg The argument to give to the command.
     * @return The command result.
     * @throws DevFailed If something went wrong.
     * @see #getCommandInType(DeviceProxy, String)
     * @see #getCommandInType(String, String)
     * @see #getCommandInType(DeviceProxy, String, int)
     * @see #getCommandOutType(DeviceProxy, String)
     * @see #getCommandOutType(String, String)
     * @see #getCommandOutType(DeviceProxy, String, int)
     */
    public static Object executeCommand(DeviceProxy proxy, String commandName, int commandInType, int commandOutType,
            final Object arg) throws DevFailed {
        return executeCommand(proxy, commandName, null, commandInType, commandOutType, Object.class, arg);
    }

    /**
     * Executes a command and returns its result.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @param commandInType The command input type, that can be obtained through <code>getCommandInType</code> methods.
     * @param commandOutType The command output type, that can be obtained through <code>getCommandOutType</code>
     *            methods.
     * @param returnType The expected class of the command result.
     * @param arg The argument to give to the command.
     * @return The command result.
     * @throws DevFailed If something went wrong.
     * @see #getCommandInType(DeviceProxy, String)
     * @see #getCommandInType(String, String)
     * @see #getCommandInType(DeviceProxy, String, int)
     * @see #getCommandOutType(DeviceProxy, String)
     * @see #getCommandOutType(String, String)
     * @see #getCommandOutType(DeviceProxy, String, int)
     */
    public static <T> T executeCommand(DeviceProxy proxy, String commandName, int commandInType, int commandOutType,
            final Class<T> returnType, final Object arg) throws DevFailed {
        return executeCommand(proxy, commandName, null, commandInType, commandOutType, returnType, arg);
    }

    /**
     * Executes a command and returns its result.
     * 
     * @param proxy The {@link DeviceProxy} connected to the device.
     * @param commandName The command name.
     * @param commandInData The {@link DeviceData} to use in order to set command argument. If <code>null</code>, a new
     *            one will be created.
     * @param commandInType The command input type, that can be obtained through <code>getCommandInType</code> methods.
     * @param commandOutType The command output type, that can be obtained through <code>getCommandOutType</code>
     *            methods.
     * @param returnType The expected class of the command result.
     * @param arg The argument to give to the command.
     * @return The command result.
     * @throws DevFailed If something went wrong.
     * @see #getCommandInType(DeviceProxy, String)
     * @see #getCommandInType(String, String)
     * @see #getCommandInType(DeviceProxy, String, int)
     * @see #getCommandOutType(DeviceProxy, String)
     * @see #getCommandOutType(String, String)
     * @see #getCommandOutType(DeviceProxy, String, int)
     */
    public static <T> T executeCommand(DeviceProxy proxy, String commandName, DeviceData commandInData,
            int commandInType, int commandOutType, final Class<T> returnType, final Object arg) throws DevFailed {
        Object commandResult = null;
        T result = null;
        if (proxy != null) {
            DeviceData output;
            if (commandInType == TangoConst.Tango_DEV_VOID) {
                // JAVAAPI-603: synchronize accesses to DeviceProxy
                synchronized (proxy) {
                    output = proxy.command_inout(commandName);
                }
            } else {
                DeviceData input = commandInData == null ? new DeviceData() : commandInData;
                InsertExtractUtils.insert(input, commandInType, arg);
                // JAVAAPI-603: synchronize accesses to DeviceProxy
                synchronized (proxy) {
                    output = proxy.command_inout(commandName, input);
                }
            }
            if ((commandOutType != TangoConst.Tango_DEV_VOID) && (output != null)) {
                commandResult = InsertExtractUtils.extract(output);
            }
            if (commandResult != null) {
                result = TypeConversionUtil.castToType(returnType, commandResult);
            }
        }
        return result;
    }
    
    public static String getTangoTypeForType(int type) {
        return TangoConst.Tango_CmdArgTypeName[type];
    }

    /**
     * Returns whether a command in/out type represents a scalar value.
     * 
     * @param commandInOutType The command in/out type.
     * @return A <code>boolean</code>.
     * @see #getCommandInType(DeviceProxy, String)
     * @see #getCommandInType(String, String)
     * @see #getCommandInType(DeviceProxy, String, int)
     * @see #getCommandOutType(DeviceProxy, String)
     * @see #getCommandOutType(String, String)
     * @see #getCommandOutType(DeviceProxy, String, int)
     */
    public static boolean isScalarType(int commandInOutType) {
        return TangoUtil.SCALARS.contains(commandInOutType);
    }

    /**
     * Extracts a {@link List} from a command value.
     * 
     * @param type The type of data that should be contained in the list.
     * @param value The command value.
     * @return A {@link List}.
     * @throws DevFailed If something went wrong. Thrown by {@link TypeConversionUtil#castToArray(Class, Object)}.
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> extractList(final Class<T> type, final Object value) throws DevFailed {
        List<T> result;
        if (value == null) {
            result = extractList((T[]) null);
        } else {
            Class<?> valueClass = value.getClass();
            if (type.equals(valueClass)) {
                result = extractList((T) value);
            } else if (valueClass.isArray() && type.equals(valueClass.getComponentType())) {
                result = extractList((T[]) value);
            } else {
                Object converted = TypeConversionUtil.castToArray(type, value);
                if (converted == null) {
                    result = extractList((T[]) null);
                } else if (type.equals(converted.getClass())) {
                    result = extractList((T) converted);
                } else {
                    result = extractList((T[]) converted);
                }
            }
        }
        return result;
    }

    /**
     * Converts an array to a list.
     * 
     * @param value The array.
     * @return A {@link List}.
     */
    public static <T> List<T> extractList(@SuppressWarnings("unchecked") final T... value) {
        final List<T> result = new ArrayList<T>();
        for (T val : value) {
            result.add(val);
        }
        return result;
    }

}
