/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.VDouble;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;

import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Cancellable;

/** Support for {@link Flowable} that sends PV value updates
 *
 *  @author Eric Berryman
 *  @author Kay Kasemir
 */
class ValueEventOnSubscribe implements FlowableOnSubscribe<VType>
{
    private final PV pv;

    private class FlowSubscription implements Cancellable, PVListener
    {
        private final FlowableEmitter<VType> emitter;

        public FlowSubscription(final FlowableEmitter<VType> emitter)
        {
            this.emitter = emitter;
            pv.addListener(this);
        }

        // PVListener
        @Override
        public void valueChanged(final VType value)
        {
            if (! (emitter.isCancelled()  ||  emitter.requested() <0))
                emitter.onNext(value);
        }

        // PVListener
        @Override
        public void disconnected()
        {
            final VType disconnected = VDouble.create(Double.NaN, ValueFactory.newAlarm(AlarmSeverity.UNDEFINED, PV.DISCONNECTED), ValueFactory.timeNow(), ValueFactory.displayNone());
            valueChanged(disconnected);
        }

        // Cancellable
        @Override
        public void cancel() throws Exception
        {
            pv.removeListener(this);
        }
    };

    public ValueEventOnSubscribe(final PV pv)
    {
        this.pv = pv;
    }

    @Override
    public void subscribe(final FlowableEmitter<VType> emitter) throws Exception
    {
        emitter.setCancellable(new FlowSubscription(emitter));
    }
}
