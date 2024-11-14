
package org.csstudio.display.builder.model;

import java.util.logging.Logger;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

@SuppressWarnings("nls")
public class AdvancedConverterApp implements AppDescriptor
{
	public static final Logger logger = Logger.getLogger(AdvancedConverterApp.class.getPackageName());
	public static final String NAME = "advancedConverter";
	public static final String DisplayName = "OPI Converter";


	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public AppInstance create() {
		BrowserConverter.displayBrowserConverter();
		return null;
	}
}

