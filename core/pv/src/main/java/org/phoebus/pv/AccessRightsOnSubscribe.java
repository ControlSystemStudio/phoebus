/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import org.phoebus.vtype.VType;

import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.functions.Cancellable;

/** Support for {@link Flowable} that sends <code>true</code> for write access
 *
 *  @author Eric Berryman
 *  @author Kay Kasemir
 */
class AccessRightsEventOnSubscribe implements FlowableOnSubscribe<Boolean>
{
    private final PV pv;

    // TODO While this is still based on the PVListener,
    //      the access rights flow receives all value updates (and ignores but the first).
    private class FlowSubscription implements Cancellable, PVListener
    {
        private final FlowableEmitter<Boolean> emitter;
        private boolean first = true;

        public FlowSubscription(final FlowableEmitter<Boolean> emitter)
        {
            this.emitter = emitter;
            pv.addListener(this);
        }

        // PVListener
        @Override
        public void valueChanged(final VType value)
        {
            // Ignore value, just send initial write access info
            if (first)
            {
                first = false;
                System.out.println(pv + " is readonly: " + pv.isReadonly());
                permissionsChanged(pv.isReadonly());
            }
        }

        // PVListener
        @Override
        public void permissionsChanged(final boolean readonly)
        {
            if (! (emitter.isCancelled()  ||  emitter.requested() <0))
                emitter.onNext(! readonly);
        }

        // PVListener
        @Override
        public void disconnected()
        {
            // Not connected -> Can't write, read-only
            permissionsChanged(true);
        }

        // Cancellable
        @Override
        public void cancel() throws Exception
        {
            pv.removeListener(this);
        }
    };

    public AccessRightsEventOnSubscribe(final PV pv)
    {
        this.pv = pv;
    }

    @Override
    public void subscribe(final FlowableEmitter<Boolean> emitter) throws Exception
    {
        emitter.setCancellable(new FlowSubscription(emitter));
    }
}
