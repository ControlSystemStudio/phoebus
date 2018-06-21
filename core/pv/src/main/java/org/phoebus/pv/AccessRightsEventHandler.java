/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Cancellable;

/** Support for {@link Flowable} that sends <code>true</code> for write access
 *  @author Eric Berryman
 *  @author Kay Kasemir
 */
class AccessRightsEventHandler implements FlowableOnSubscribe<Boolean>
{
    private final PV pv;

    class Subscription implements Cancellable
    {
        private final FlowableEmitter<Boolean> emitter;

        public Subscription(final FlowableEmitter<Boolean> emitter)
        {
            this.emitter = emitter;
            pv.addSubscription(this);
        }

        public void update(final boolean readonly)
        {
            if (! (emitter.isCancelled()  ||  emitter.requested() <0))
                emitter.onNext(! readonly);
        }

        // Cancellable
        @Override
        public void cancel() throws Exception
        {
            pv.removeSubscription(this);
        }
    };

    public AccessRightsEventHandler(final PV pv)
    {
        this.pv = pv;
    }

    @Override
    public void subscribe(final FlowableEmitter<Boolean> emitter) throws Exception
    {
        emitter.setCancellable(new Subscription(emitter));
    }
}
