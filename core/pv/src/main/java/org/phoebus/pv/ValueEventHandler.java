/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import org.epics.vtype.VType;

import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Cancellable;

/** Support for {@link Flowable} that sends PV value updates
 *  @author Eric Berryman
 *  @author Kay Kasemir
 */
class ValueEventHandler implements FlowableOnSubscribe<VType>
{
    private final PV pv;

    class Subscription implements Cancellable
    {
        private final FlowableEmitter<VType> emitter;

        public Subscription(final FlowableEmitter<VType> emitter)
        {
            this.emitter = emitter;
            pv.addSubscription(this);
        }

        public void update(final VType value)
        {
            if (! (emitter.isCancelled()  ||  emitter.requested() <0))
                emitter.onNext(value);
        }

        // Cancellable
        @Override
        public void cancel() throws Exception
        {
            pv.removeSubscription(this);
        }
    };

    public ValueEventHandler(final PV pv)
    {
        this.pv = pv;
    }

    @Override
    public void subscribe(final FlowableEmitter<VType> emitter) throws Exception
    {
        emitter.setCancellable(new Subscription(emitter));
    }
}
