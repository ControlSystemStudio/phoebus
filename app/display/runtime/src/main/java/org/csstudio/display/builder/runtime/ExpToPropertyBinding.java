package org.csstudio.display.builder.runtime;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.rules.RuleInfo.ExpressionInfo;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;

public abstract class ExpToPropertyBinding
{
    private final WidgetRuntime<?> runtime;
    private final ExpressionInfo<?> expression;
    private final AtomicReference<RuntimePV> pv_ref = new AtomicReference<>();
    private final boolean need_write_access;

    private final RuntimePVListener listener;

    /**
     * @param runtime           {@link WidgetRuntime}
     * @param name              Property with name of PV
     * @param property          Property to which the value of the PV will be
     *                          written
     * @param need_write_access Does the PV need write access?
     */
    public <T> ExpToPropertyBinding(final WidgetRuntime<?> runtime, final ExpressionInfo<?> expression,
            final RuntimePVListener listener, final WidgetProperty<T> property, final boolean need_write_access)
    {
        this.runtime = runtime;
        this.expression = expression;
        this.listener = listener;
        this.need_write_access = need_write_access;

        // Fetching the PV name will resolve macros,
        // i.e. set the name property and thus notify listeners
        // -> Do that once in 'connect()' before registering listener
        connect();
    }

    /** @return PV or <code>null</code> */
    public RuntimePV getPV()
    {
        return pv_ref.get();
    }

    private void connect()
    {
        final String pv_name = expression.getExp();
        if (pv_name.isEmpty())
        {
            listener.valueChanged(null, PVWidget.RUNTIME_VALUE_NO_PV);
            return;
        }
        logger.log(Level.FINE,  "Connecting {0} {1}", new Object[] { runtime.widget, pv_name });
        final RuntimePV pv;
        try
        {
            pv = PVFactory.getPV(pv_name);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot connect to PV " + pv_name, ex);
            return;
        }
        pv.addListener(listener);
        runtime.addPV(pv, need_write_access);
        pv_ref.set(pv);
    }

    private void disconnect()
    {
        final RuntimePV pv = pv_ref.getAndSet(null);
        if (pv == null)
            return;
        pv.removeListener(listener);
        PVFactory.releasePV(pv);
        runtime.removePV(pv);
    }

    public void dispose()
    {
        disconnect();
    }
}
