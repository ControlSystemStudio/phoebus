package org.phoebus.pv.tgc;

import fr.esrf.Tango.DevFailed;
import fr.soleil.tango.clientapi.TangoCommand;
import org.epics.vtype.*;
import org.phoebus.pv.PV;
import org.phoebus.pv.tga.TangoTypeUtil;
import org.tango.command.CommandTangoType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class TangoCmdContext {

    private static TangoCmdContext instance;
    private final ConcurrentHashMap<String, TangoCommand> commands;

    private TangoCmdContext() {
        commands = new ConcurrentHashMap<>();
    }

    public static synchronized TangoCmdContext getInstance() throws Exception {
        if (instance == null)
            instance = new TangoCmdContext();
        return instance;
    }

    public void createTangoCommand(String deviceName, String commandName, String baseName, TangoCmd_PV pv) throws DevFailed {
        TangoCommand tangoCommand = commands.get(baseName);
        if ( tangoCommand == null ){
            tangoCommand = new TangoCommand(deviceName, commandName);
            commands.put(baseName, tangoCommand);
        }
        pv.StartCommand(tangoCommand.getCommandName());
    }

    public void removeTangoCommand(String baseName) throws Exception {
        TangoCommand tangoCommand = commands.get(baseName);
        if (tangoCommand == null){
            PV.logger.log(Level.WARNING, "Could not remove Tango command \"" + baseName
                    + "\" due to no internal record of command");
            throw new Exception("Tango command remove failed: no command record.");
        }
        commands.remove(baseName, tangoCommand);
    }

    public void executeTangoCommand(String baseName, Object new_value, TangoCmd_PV pv) throws Exception {
        TangoCommand tangoCommand = commands.get(baseName);
        if (tangoCommand == null){
            PV.logger.log(Level.WARNING, "Could not find Tango command \"" + baseName
                    + "\" due to no internal record of command");
            throw new Exception("Tango command execute failed: no command record.");
        }

        CommandTangoType typeFromTango = CommandTangoType.getTypeFromTango(tangoCommand.getArginType());
        Object res;
        VType value;
        switch (typeFromTango){
            case DEVBOOLEAN:
                res = tangoCommand.execute(Boolean.class, new_value);
                value = TangoTypeUtil.convert(res, VBoolean.class);
                pv.endCommand(value);
                break;
            case DEVSHORT:
                res = tangoCommand.execute(Short.class, new_value);
                value = TangoTypeUtil.convert(res, VShort.class);
                pv.endCommand(value);
                break;
            case DEVLONG64:
                res = tangoCommand.execute(Long.class, new_value);
                value = TangoTypeUtil.convert(res, VLong.class);
                pv.endCommand(value);
                break;
            case DEVFLOAT:
                res = tangoCommand.execute(Float.class, new_value);
                value = TangoTypeUtil.convert(res, VFloat.class);
                pv.endCommand(value);
                break;
            case DEVDOUBLE:
                res = tangoCommand.execute(Double.class,new_value);
                value = TangoTypeUtil.convert(res, VDouble.class);
                pv.endCommand(value);
                break;
            case DEVSTRING:
                res = tangoCommand.execute(String.class,new_value);
                value = TangoTypeUtil.convert(res, VString.class);
                pv.endCommand(value);
                break;
            case DEVLONG:
                res = tangoCommand.execute(Integer.class,new_value);
                value = TangoTypeUtil.convert(res, VInt.class);
                pv.endCommand(value);
                break;
            case DEVUCHAR:
                res = tangoCommand.execute(Byte.class,new_value);
                value = TangoTypeUtil.convert(res, VByte.class);
                pv.endCommand(value);
                break;
            default:
                throw new IllegalArgumentException("Value " + new_value + " cannot be converted.");
        }



    }



}
