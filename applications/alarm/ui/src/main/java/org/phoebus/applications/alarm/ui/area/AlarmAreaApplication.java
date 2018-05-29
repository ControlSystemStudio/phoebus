package org.phoebus.applications.alarm.ui.area;

import java.net.URL;

import org.phoebus.framework.spi.AppDescriptor;

public class AlarmAreaApplication implements AppDescriptor
{
	 public static final String NAME = "alarm_area";
	    public static final String DISPLAY_NAME = "Alarm Area";

	    @Override
	    public String getName()
	    {
	        return NAME;
	    }

	    @Override
	    public String getDisplayName()
	    {
	        return DISPLAY_NAME;
	    }

	    @Override
	    public URL getIconURL()
	    {
	        //return AlarmUI.class.getResource("/icons/alarmarea.png");
	    	return null;
	    }

	    @Override
	    public AlarmAreaInstance create()
	    {
	        if (AlarmAreaInstance.INSTANCE == null)
	            AlarmAreaInstance.INSTANCE = new AlarmAreaInstance(this);
	        else
	            AlarmAreaInstance.INSTANCE.raise();
	        return AlarmAreaInstance.INSTANCE;
	    }
}
