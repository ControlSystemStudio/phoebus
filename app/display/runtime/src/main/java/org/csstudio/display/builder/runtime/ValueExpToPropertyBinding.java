package org.csstudio.display.builder.runtime;

import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.BooleanWidgetProperty;
import org.csstudio.display.builder.model.properties.DoubleWidgetProperty;
import org.csstudio.display.builder.model.properties.IntegerWidgetProperty;
import org.csstudio.display.builder.model.properties.LongWidgetProperty;
import org.csstudio.display.builder.model.properties.StringWidgetProperty;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.vtype.VType;

public class ValueExpToPropertyBinding extends ExpToPropertyBinding
{

    /**
     * 
     * @param runtime
     * @param expression
     * @param p
     * @param need_write_access
     */
    @SuppressWarnings({ "unchecked" })
    public ValueExpToPropertyBinding(final WidgetRuntime<?> runtime, final ExpressionInfo<?> expression, final WidgetProperty p, final boolean need_write_access)
    {
        super(runtime,
                expression,
                new RuntimePVListener()
                {
                    @Override
                    public void valueChanged(RuntimePV pv, VType value) {
                        if (p instanceof BooleanWidgetProperty)
                        {
                            p.setValue(VTypeUtil.getValueBoolean(value));
                        }
                        else if (p instanceof StringWidgetProperty)
                        {
                            p.setValue(VTypeUtil.getValueString(value, true));
                        }
                        else if (p instanceof IntegerWidgetProperty
                                || p instanceof LongWidgetProperty
                                || p instanceof DoubleWidgetProperty)
                        {
                            p.setValue(VTypeUtil.getValueNumber(value));
                        }
                    }

                },
                p,
                need_write_access);
    }
    

}
