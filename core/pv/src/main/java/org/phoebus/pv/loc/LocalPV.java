/*******************************************************************************
 * Copyright (c) 2014-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.loc;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;

/** Local Process Variable
 *
 *  <p>Syntax:
 *  <ul>
 *  <li>loc://name(3.14), same as loc://name&lt;VDouble>(3.14)
 *  <li>loc://name("Fred"), same as loc://name&lt;VString>("Fred")
 *  <li>loc://name(1, 2, 3), same as loc://name&lt;VDoubleArray>(1, 2, 3)
 *  <li>loc://name&lt;VDoubleArray>(1), forces array type
 *  <li>loc://name("a", "b", "c"), same as loc://name&lt;VStringArray>("a", "b", "c")
 *  <li>loc://name&lt;VLong>(1e10), forces long integer data type
 *  <li>loc://name&lt;VEnum>(0, "a", "b", "c"), declares enumerated type with initial value and labels
 *  <li>loc://name&lt;VTable>, declares PV as table (initially empty)
 *  <li>loc://name&lt;VTable>("X", "Y"), declares PV as table with given column names (initially empty)
 *  </ul>
 *  @author Kay Kasemir, based on similar code in org.csstudio.utility.pv
 */
@SuppressWarnings("nls")
public class LocalPV extends PV
{
    private volatile Class<? extends VType> type;
    private final List<String> initial_value;

    protected LocalPV(final String actual_name, final Class<? extends VType> type, final List<String> initial_value) throws Exception
    {
        super(actual_name);
        this.type = type;
        this.initial_value = initial_value;

        // Set initial value
        notifyListenersOfValue(ValueHelper.getInitialValue(initial_value, type));
    }

    protected void checkInitializer(final Class<? extends VType> type, final List<String> initial_value)
    {
        if (type != this.type  ||  ! Objects.equals(initial_value, this.initial_value))
            logger.log(Level.WARNING, "PV " + getName() + " was initialized as " + formatInit(this.type, this.initial_value) +
                    " and is now requested as " +  formatInit(type, initial_value));
    }

    private String formatInit(final Class<? extends VType> type, final List<String> value)
    {
        final StringBuilder buf = new StringBuilder();
        buf.append('<').append(type.getSimpleName()).append('>');
        if (value != null)
        {
            buf.append('(');
            for (int i=0; i<value.size(); ++i)
            {
                if (i > 0)
                    buf.append(",");
                buf.append(value.get(i));
            }
            buf.append(')');
        }
        return buf.toString();
    }

    @Override
    public void write(final Object new_value) throws Exception
    {
        if (new_value == null)
            throw new Exception(getName() + " got null");

        try
        {
            final VType last_value = read();
            final boolean change_from_double = initial_value == null  &&
                                               last_value instanceof VDouble  &&
                                               ((VDouble)last_value).getAlarm().getSeverity() == ValueHelper.UDF.getSeverity();
            final VType value = ValueHelper.adapt(new_value, type, last_value, change_from_double);
            if (change_from_double  &&  ! type.isInstance(value))
            {
                final Class<? extends VType> new_type;
                if (value instanceof VDoubleArray)
                    new_type = VDoubleArray.class;
                else if (value instanceof VStringArray)
                    new_type = VStringArray.class;
                else
                    new_type = VString.class;
                logger.log(Level.WARNING, "PV " + getName() + " changed from " + type.getSimpleName() + " to " + new_type.getSimpleName());
                type = new_type;
            }
            notifyListenersOfValue(value);
        }
        catch (Exception ex)
        {
            if (new_value != null  &&  new_value.getClass().isArray())
                throw new Exception("Failed to write " + new_value.getClass().getSimpleName() + " to " + getName(), ex);
            throw new Exception("Failed to write '" + new_value + "' to " + this, ex);
        }
     }

    @Override
    protected void close()
    {
        super.close();
        LocalPVFactory.releasePV(this);
    }
}
