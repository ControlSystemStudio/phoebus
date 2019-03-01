/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */


package org.phoebus.applications.saveandrestore;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;

public class SaveAndRestoreApplication implements AppDescriptor {
	
	public static final String NAME = "Save And Restore";

	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String getDisplayName() {
		return NAME;
	}

	@Override
	public AppInstance create() {
		
		return new SaveAndRestoreAppInstance(this);
	}

}
