package org.phoebus.pv.tango;

import fr.esrf.Tango.DevFailed;
import fr.esrf.TangoApi.CallBack;
import fr.esrf.TangoApi.DeviceAttribute;
import fr.esrf.TangoApi.events.EventData;
import org.epics.vtype.*;
import org.phoebus.pv.PV;
import org.tango.attribute.AttributeTangoType;

import java.time.Instant;
import java.util.logging.Level;


public class Tango_PV extends PV {

    private final String baseName;
    private  String deviceName;
    private String variableType;
    private  String memberVariable;

    public Tango_PV(String name, String baseName) throws Exception {
        super(name);
        this.baseName = baseName;
        parseRawName(baseName);
        if ("att".equals(variableType)){
            TangoContext.getInstance().subscribeAttributeEvent(deviceName, memberVariable, baseName,this);
        }else if ("com".equals(variableType)){
            TangoContext.getInstance().createTangoCommand(deviceName, memberVariable, baseName, this);
        }
    }

    /** Handles these types of tango names:
     *  <pre>
     *      att:/device_name/attribute_name --> att && device_name && attribute_name
     *      com:/device_name/command_name --> com && device_name && command_name
     *  </pre>
     */
    private void parseRawName(final String name) throws Exception {
        //String variableType, deviceName, memberVariable;
        String[] str= name.split(":/");

        if (name.length() <= 4 && str.length > 2)
            throw new Exception("Invalid input:" + name);

        //Locate tango attribute or command
        variableType = str[0];
        String prefix = str[1];

       int pos = prefix.lastIndexOf('/');
       if (pos <= 0)
           throw new Exception("Invalid input：" + name);
        //Locate device name
        deviceName = prefix.substring(0,pos);

        //Locate tango attribute or command name
        memberVariable = prefix.substring(pos+1);
    }

    /**
     * attributes and commands need to be closed separately.
     */
    @Override
    protected void close()
    {
        try
        {
            if ("att".equals(variableType)){
                TangoContext.getInstance().unSubscribeAttributeEvent(baseName);
            }else if ("com".equals(variableType)){
                TangoContext.getInstance().removeTangoCommand(baseName);
            }

        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failed to unsubscribe Tango Attribute/Command from base name " + baseName);
            ex.printStackTrace();
        }
    }


    @Override
    public void write(final Object new_value) throws Exception{
        if (new_value == null)
            throw new Exception(getName() + " got null");
        if ("att".equals(variableType)){
            TangoContext.getInstance().writeAttribute(baseName, memberVariable, new_value);
        }else if ("com".equals(variableType)){
            TangoContext.getInstance().executeTangoCommand(baseName, new_value, this);
        }
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

    class TangoCallBack extends CallBack {
        private final AttributeTangoType type;

        public TangoCallBack(AttributeTangoType type) {
            this.type = type;
        }

        @Override
        public void push_event(EventData evt) {
            try {
                VType value;
                DeviceAttribute attr_value = evt.attr_value;
                Time time = Time.of(Instant.ofEpochMilli(attr_value.getTime()));
                switch (type){
                    case DEVBOOLEAN:
                        value = VBoolean.of(attr_value.extractBoolean(), Alarm.none(), time);
                        notifyListenersOfValue(value);
                        break;
                    case DEVLONG64:
                        value = VLong.of(attr_value.extractLong64(), Alarm.none(), time, Display.none());
                        notifyListenersOfValue(value);
                        break;
                    case DEVULONG64:
                        value = VLong.of(attr_value.extractULong64(), Alarm.none(), time, Display.none());
                        notifyListenersOfValue(value);
                        break;
                    case DEVSHORT:
                        value = VShort.of(attr_value.extractShort(), Alarm.none(), time, Display.none());
                        notifyListenersOfValue(value);
                        break;
                    case DEVUSHORT:
                        value = VInt.of(attr_value.extractUShort(),Alarm.none(), time, Display.none());
                        notifyListenersOfValue(value);
                        break;
                    case DEVLONG:
                        value = VInt.of(attr_value.extractLong(), Alarm.none(), time, Display.none());
                        notifyListenersOfValue(value);
                        break;
                    case DEVULONG:
                        value = VLong.of(attr_value.extractULong(), Alarm.none(), time, Display.none());
                        notifyListenersOfValue(value);
                        break;
                    case DEVFLOAT:
                        value = VFloat.of(attr_value.extractFloat(), Alarm.none(), time, Display.none());
                        notifyListenersOfValue(value);
                        break;
                    case DEVDOUBLE:
                        value = VDouble.of(attr_value.extractDouble(), Alarm.none(), time, Display.none());
                        notifyListenersOfValue(value);
                        break;
                    case DEVSTRING:
                        value = VString.of(attr_value.extractString(), Alarm.none(), time);
                        notifyListenersOfValue(value);
                        break;
                    case DEVUCHAR:
                        value = VShort.of(attr_value.extractUChar(), Alarm.none(), time,Display.none());
                        notifyListenersOfValue(value);
                        break;
                    default:
                        throw new IllegalArgumentException("Value " + evt.attr_value + " cannot be converted.");
                }
            }catch (DevFailed e){
                throw new RuntimeException(e);
            }
        }
    }
}

