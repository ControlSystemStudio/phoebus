package org.csstudio.display.builder.runtime;

import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.vtype.VType;

public class BooleanExpToPropertyBinding extends ExpToPropertyBinding
{

    /** @param runtime {@link WidgetRuntime}
     *  @param name Property with name of PV
     *  @param p Property to which the value of the PV will be written
     *  @param need_write_access Does the PV need write access?
     */
    public <T> BooleanExpToPropertyBinding(final WidgetRuntime<?> runtime, final ExpressionInfo<?> expression, final WidgetProperty<Object> p, final boolean need_write_access)
    {
        super(runtime,
                expression,
                new RuntimePVListener() {

                    @Override
                    public void valueChanged(RuntimePV pv, VType value) {
                        if (VTypeUtil.getValueBoolean(value)) {
                            Object val = ((WidgetProperty<?>)expression.getPropVal()).getValue();
                            p.setValue(val);
                        }
                    }

                },
                p,
                need_write_access);

    }

}
