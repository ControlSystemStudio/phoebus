/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import java.io.Closeable;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.phoebus.vtype.VType;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/** A 'PV' with Flow(able) API
 *
 *  <p>This class wraps a {@link PV} as a {@link Publisher} for use with RxJava.
 *  RxJava offers a 'reactive' API.
 *  Java 9 started to include the underlying reactive stream interfaces
 *  as {@link Flow}, but for the time being the RxJava code tries
 *  to stay compatible with previous Java versions and thus
 *  used its own {@link Publisher}.
 *  In the future we may directly implement the Java 9 {@link Flow.Publisher}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FlowablePV implements Publisher<VType>, Closeable
{
    private final PV pv;

    // "Rule" in the following refers to the rules from
    // https://github.com/reactive-streams/reactive-streams-jvm#reactive-streams
    private class PVSubscription implements Subscription, PVListener
    {
        private final AtomicBoolean subscribed = new AtomicBoolean();
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicLong requested = new AtomicLong();
        private Subscriber<? super VType> subscriber;

        public PVSubscription(final Subscriber<? super VType> subscriber)
        {
            this.subscriber = subscriber;
        }

        // Subscription
        @Override
        public void request(final long n)
        {
            // System.out.println("Request " + n);

            // Rule 6:
            // After the Subscription is cancelled,
            // additional Subscription.request(long n) MUST be NOPs.
            if (cancelled.get())
                return;

            // Rule 8:
            // While .. not cancelled, .. MUST signal onError
            // with a IllegalArgumentException if the argument is <= 0.
            if (n <= 0)
            {
                subscriber.onError(new IllegalArgumentException("Requested " + n));
                return;
            }

            requested.addAndGet(n);
            // Listen to PV on first request
            // Rule 2:
            // The Subscription MUST allow the Subscriber
            // to request() synchronously from within onNext or onSubscribe.
            //
            // Calling 'addListener()' can result in valueChanged()
            // and thus onNext() for the first sample.
            //
            // Good news: The case onNext() -> request() -> addListener()
            // is not possible.
            // Without the initial addListener(), onNext() won't be invoked.
            // The only possible scenario:
            // First request -> addListener -> onNext
            // If onNext now calls request again, subscribed is set
            // so we won't call onNext from within request -> addListener
            if (subscribed.getAndSet(true) == false)
                pv.addListener(this);
        }

        // Subscription
        @Override
        public void cancel()
        {
            // System.out.println("Cancel");
            if (cancelled.getAndSet(true) == true)
                return;
            if (subscribed.getAndSet(false) == true)
                pv.removeListener(this);
        }

        // PVListener
        @Override
        public void valueChanged(final VType value)
        {
            // Decrement count, except treating Long.MAXVALUE as "infinite"
            if (requested.updateAndGet(count -> count == Long.MAX_VALUE ? Long.MAX_VALUE : count-1) >= 0)
                subscriber.onNext(value);
            else
                pv.removeListener(this);
        }

        // PVListener
        @Override
        public void disconnected()
        {
            subscriber.onError(new Exception("disconnected"));
        }
    }

    public FlowablePV(final String name) throws Exception
    {
        pv = PVPool.getPV(name);
    }

    @Override
    public void subscribe(final Subscriber<? super VType> subscriber)
    {
        subscriber.onSubscribe(new PVSubscription(subscriber));
    }

    @Override
    public void close()
    {
        PVPool.releasePV(pv);
    }
}
