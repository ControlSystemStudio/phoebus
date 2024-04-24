package org.phoebus.pv.tga;

import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.EventProperties;
import fr.esrf.TangoApi.AttributeInfoEx;
import fr.esrf.TangoApi.AttributeProxy;
import fr.esrf.TangoApi.DeviceAttribute;
import org.epics.vtype.*;
import org.phoebus.pv.PV;
import org.tango.attribute.AttributeTangoType;
import org.tango.server.events.EventType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

;

public class TangoAttrContext {
    private static TangoAttrContext instance;
    private final ConcurrentHashMap<String, AttributeProxy> attributeProxys;
    private final ConcurrentHashMap<String, Integer> events;
    private final ConcurrentHashMap<String, AttributeTangoType> types;



    private TangoAttrContext() {
        events = new ConcurrentHashMap<>();
        attributeProxys = new ConcurrentHashMap<>();
        types = new ConcurrentHashMap<>();
    }

    public static synchronized TangoAttrContext getInstance() throws Exception {
        if (instance == null)
            instance = new TangoAttrContext();
        return instance;
    }

    public void subscribeAttributeEvent(String deviceName, String attributeName, String baseName, TangoAttr_PV pv) throws DevFailed {
        String name = deviceName +"/" + attributeName;
        AttributeProxy attributeProxy;
        if (attributeProxys.get(baseName) == null) {
            attributeProxy = new AttributeProxy(name);
            subscribeAttributeEvent(baseName, attributeProxy, pv);
            attributeProxys.put(baseName,attributeProxy);
        }else {
            //nothing to do
        }

    }

    private void subscribeAttributeEvent(String baseName, AttributeProxy attributeProxy, TangoAttr_PV pv) throws DevFailed {
        AttributeInfoEx attribute_info_ex;
        try {
            attribute_info_ex = attributeProxy.get_info_ex();
        } catch (DevFailed e) {
            throw new RuntimeException(e);
        }

        //obtain the type of attribute's value.
        AttributeTangoType type = AttributeTangoType.getTypeFromTango(attribute_info_ex.data_type);

        types.putIfAbsent(baseName, type);


        //obtain the tango event type.
        EventType eventType = EventType.CHANGE_EVENT;
        EventProperties tangoObj = attribute_info_ex.events.getTangoObj();
        if (tangoObj.ch_event.abs_change.equals("Not specified") && tangoObj.ch_event.rel_change.equals("Not specified"))
            eventType = EventType.PERIODIC_EVENT;

        //subscribe the tango event.
        int event_id;
        try {
            event_id = attributeProxy.subscribe_event(eventType.getValue(), pv.new TangoCallBack(type), new String[]{});
        } catch (DevFailed e) {
            throw new RuntimeException(e);
        }
        events.put(baseName, event_id);

    }

    public void unSubscribeAttributeEvent(String baseName) throws Exception {
        if (!attributeProxys.containsKey(baseName)){
            PV.logger.log(Level.WARNING, "Could not unsubscribe Tango attribute \"" + baseName
                    + "\" due to no Attribute Proxy.");
            throw new Exception("Tango attribute unsubscribe failed: no Attribute proxy connection.");
        }

        AttributeProxy attributeProxy = attributeProxys.get(baseName);
        Integer event_id = events.get(baseName);
        if (event_id == null){
            PV.logger.log(Level.WARNING, "Could not unsubscribe  Tango attribute \"" + baseName
                    + "\" due to no internal record of attribute");
            throw new Exception("Tango attribute unsubscribe failed: no attribute record.");
        }
        attributeProxy.getDeviceProxy().unsubscribe_event(event_id);
        attributeProxys.remove(baseName);
        events.remove(baseName);
        types.remove(baseName);
    }


    public void writeAttribute(String baseName, String attributeName, Object new_value) throws Exception {
        AttributeProxy attributeProxy = attributeProxys.get(baseName);
        AttributeTangoType type = types.get(baseName);
        if (type == null){
            PV.logger.log(Level.WARNING, "Could not find type of attribute :" + baseName);
            throw new Exception("Tango attribute write failed: attribute type not found.");
        }
        VType vType;
        String value;
        switch (type){
            case DEVBOOLEAN:
                vType = TangoTypeUtil.convert(new_value, VBoolean.class);
                value = TangoTypeUtil.ToString(vType);
                attributeProxy.write(new DeviceAttribute(attributeName, Boolean.parseBoolean(value)));
                break;
            case DEVLONG64:
            case DEVULONG64:
                vType = TangoTypeUtil.convert(new_value, VLong.class);
                value = TangoTypeUtil.ToString(vType);
                attributeProxy.write(new DeviceAttribute(attributeName, Long.parseLong(value)));
                break;
            case DEVSHORT:
            case DEVUSHORT:
                vType = TangoTypeUtil.convert(new_value, VShort.class);
                value = TangoTypeUtil.ToString(vType);
                attributeProxy.write(new DeviceAttribute(attributeName, Short.parseShort(value)));
                break;
            case DEVLONG:
            case DEVULONG:
                vType = TangoTypeUtil.convert(new_value, VInt.class);
                value = TangoTypeUtil.ToString(vType);
                attributeProxy.write(new DeviceAttribute(attributeName, Integer.parseInt(value)));
                break;
            case DEVFLOAT:
                vType = TangoTypeUtil.convert(new_value, VFloat.class);
                value = TangoTypeUtil.ToString(vType);
                attributeProxy.write(new DeviceAttribute(attributeName, Float.parseFloat(value)));
                break;
            case DEVDOUBLE:
                vType = TangoTypeUtil.convert(new_value, VDouble.class);
                value = TangoTypeUtil.ToString(vType);
                attributeProxy.write(new DeviceAttribute(attributeName, Double.parseDouble(value)));
                break;
            case DEVSTRING:
                vType = TangoTypeUtil.convert(new_value, VString.class);
                value = TangoTypeUtil.ToString(vType);
                attributeProxy.write(new DeviceAttribute(attributeName,  value));
                break;
            case DEVUCHAR:
                vType = TangoTypeUtil.convert(new_value, VByte.class);
                value = TangoTypeUtil.ToString(vType);
                attributeProxy.write(new DeviceAttribute(attributeName, Byte.parseByte(value)));
                break;
            default:
                throw new IllegalArgumentException("Value " + new_value + " cannot be converted.");
        }

    }

}
