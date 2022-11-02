package org.phoebus.pv.tgc;

import org.epics.vtype.VType;
import org.phoebus.pv.PV;

import java.util.logging.Level;

public class TangoCmd_PV extends PV {
    private final String baseName;
    private  String device;
    private String command;
    public TangoCmd_PV(String name, String baseName) throws Exception {
        super(name);
        this.baseName = baseName;
        parseRawName(baseName);
        TangoCmdContext.getInstance().createTangoCommand(device, command, baseName, this);
    }

    private void parseRawName(final String name) throws Exception {
        int pos = name.lastIndexOf('/');
        if (pos <= 0)
            throw new Exception("Invalid input：" + name);
        //Locate device name
        device = name.substring(0,pos);
        //Locate tango command
        command = name.substring(pos+1);
    }

    @Override
    protected void close()
    {
        try
        {
            TangoCmdContext.getInstance().removeTangoCommand(baseName);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failed to unsubscribe Tango Command from base name " + baseName);
            ex.printStackTrace();
        }
    }

    @Override
    public void write(final Object new_value) throws Exception{
        if (new_value == null)
            throw new Exception(getName() + " got null");
        TangoCmdContext.getInstance().executeTangoCommand(baseName, new_value, this);
    }




    public void StartCommand(final String commandName) {
        notifyListenersOfValue(VType.toVType(commandName));
    }

    /**
     Return the result after the command is executed。
     */
    public void endCommand(final VType value) {
        notifyListenersOfValue(value);
    }

}
