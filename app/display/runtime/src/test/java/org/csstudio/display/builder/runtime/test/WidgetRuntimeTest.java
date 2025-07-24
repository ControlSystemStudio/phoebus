package org.csstudio.display.builder.runtime.test;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.actions.WritePVAction;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.script.PVUtil;
import org.junit.jupiter.api.Test;
import org.phoebus.pv.PVPool;
import org.phoebus.pv.loc.LocalPVFactory;

public class WidgetRuntimeTest {

    @Test
    public void testWriteAction()
    {
       //Test for Write action on default pv different from ca see PR
       //https://github.com/ControlSystemStudio/phoebus/pull/3412
       //Force default data source to loc://
       PVPool.default_type = LocalPVFactory.TYPE;
       String pv_name = "my_pv";
       try {
           RuntimePV pv = PVUtil.createPV(pv_name, 0);
           //First init value
           double initValue = 10;
           //VDouble val = VDouble.of(initValue, Alarm.none(), org.epics.vtype.Time.now(), org.epics.vtype.Display.none());
           pv.write(initValue);
           //PVUtil.writePV(pv_name, initValue, 0);
           double readValue = PVUtil.getDouble(pv);
           //Test in standard way
           assertTrue("Write succeed", readValue == initValue);
           
           //Test with WidgetRuntime (write Action)
           ActionButtonWidget widget = new ActionButtonWidget();
           widget.setPropertyValue(CommonWidgetProperties.propPVName.getName(), pv_name);
           //Add write action
           //Write new value
           double newValue = 20;
           
           List<ActionInfo> actionList = new ArrayList<ActionInfo>();
           ActionInfo writeAction = new WritePVAction("Write value", pv_name, String.valueOf(newValue));
           actionList.add(writeAction);
           ActionInfos actInfos = new ActionInfos(actionList, true);
           widget.setPropertyValue(CommonWidgetProperties.propActions.getName(), actInfos);
           
           //Create Widget Runtime
           WidgetRuntime<ActionButtonWidget> ofWidget = new WidgetRuntime<ActionButtonWidget>();
           ofWidget.initialize(widget);
           ofWidget.addPV(pv, true);
           ofWidget.start();
         
           ofWidget.writePV(pv_name, newValue);
           
           //Test the new value
           readValue = PVUtil.getDouble(pv);
           //Test if the new value is ok
           assertTrue("Write succeed", readValue == newValue);
       } catch (Exception e) {
           e.printStackTrace();
           fail(e);
       }
       
      
      
       
    }
    
}
