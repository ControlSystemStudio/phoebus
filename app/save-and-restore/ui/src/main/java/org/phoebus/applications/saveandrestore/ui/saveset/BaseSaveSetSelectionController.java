/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact Information: Facility for Rare Isotope Beam,
 *                      Michigan State University,
 *                      East Lansing, MI 48824-1321
 *                      http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.saveset;

import org.phoebus.applications.saveandrestore.model.Node;

/**
 * Abstract base class for {@link Node} selection dialog controller
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public abstract class BaseSaveSetSelectionController implements ISelectedNodeProvider {
    protected boolean isDisabledSavesetSelection;

    protected void disableSavesetSelection() {
        isDisabledSavesetSelection = true;
    }
}
