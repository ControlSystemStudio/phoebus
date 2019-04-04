/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved
 * The contents of this file are subject to the terms of
 * the GNU General Public License Version 3 only ("GPL"
 * Version 3, or the "License"). You can obtain a copy of
 * the License at https://www.gnu.org/licenses/gpl-3.0.html
 * You may use, distribute and modify this code under the
 * terms of the GPL Version 3 license. See the License for
 * the specific language governing permissions and
 * limitations under the License.
 * When distributing the software, include this License
 * Header Notice in each file. If applicable, add the
 * following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying
 * information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propBorderAlarmSensitive;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPVName;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropPVValue;

import java.time.Instant;
import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.WidgetProperty;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
import org.epics.vtype.VType;

/** A base class for all widgets having a primary PV and value.
 *
 *  <p>Default WidgetRuntime will connect PV to "pv_name"
 *  and update "pv_value" with received updates.
 *
 *  @author Kay Kasemir
 *  @author claudiorosati, European Spallation Source ERIC
 */
@SuppressWarnings("nls")
public class PVWidget extends VisibleWidget
{
    /** Special value of runtimePropValue that indicates that there is no PV (empty PV name).
     *
     *  <p> Widget representation can detect this as a special case
     *  and for example show a general "OK" state.
     *
     *  <p>When the widget has a PV but becomes disconnected, the value will be <code>null</code>
     */
    public static final VType RUNTIME_VALUE_NO_PV = VString.of(Messages.ValueNoPV,
                                                               Alarm.of(AlarmSeverity.NONE, AlarmStatus.CLIENT, Messages.ValueNoPV),
                                                               Time.of(Instant.ofEpochSecond(0), 0, false));

    private volatile WidgetProperty<String> pv_name;
    private volatile WidgetProperty<VType>  pv_value;

    /** @param type Widget type. */
    public PVWidget(final String type)
    {
        super(type);
    }

    /** @param type Widget type.
     *  @param default_width Default widget width.
     *  @param default_height Default widget height.
     */
    public PVWidget(final String type, final int default_width, final int default_height)
    {
        super(type, default_width, default_height);
    }

    @Override
    protected void defineProperties (final List<WidgetProperty<?>> properties )
    {
        super.defineProperties(properties);
        properties.add(pv_name = propPVName.createProperty(this, ""));
        properties.add(pv_value = runtimePropPVValue.createProperty(this, null));
        properties.add(propBorderAlarmSensitive.createProperty(this, true));
    }

    @Override
    protected String getInitialTooltip()
    {
        // PV-based widgets shows the PV and value
        return "$(pv_name)\n$(pv_value)";
    }

    /** @return 'pv_name' property */
    public final WidgetProperty<String> propPVName()
    {
        return pv_name;
    }

    /** @return Runtime 'pv_value' property */
    public WidgetProperty<VType> runtimePropValue()
    {
        return pv_value;
    }
}
