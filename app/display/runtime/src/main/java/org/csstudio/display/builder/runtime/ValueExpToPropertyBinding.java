package org.csstudio.display.builder.runtime;

import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;
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
    public ValueExpToPropertyBinding(final WidgetRuntime<?> runtime, final ExpressionInfo<?> expression, final WidgetProperty<Object> p, final boolean need_write_access)
    {
        super(runtime,
                expression,
                new RuntimePVListener()
                {
                    @Override
                    public void valueChanged(RuntimePV pv, VType value) {
                        p.setValue(value);
                    }

                },
                p,
                need_write_access);
    }

}
