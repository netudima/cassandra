/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Snapshot;
import org.apache.cassandra.utils.ReflectionUtils;

/**
 * An alternative to Dropwizard Histogram which implements the same kind of API.
 * it has more efficent counting operations and consumes less memory.
 * The counter logic is implemented using {@link ThreadLocalMetrics} functionality.
 *
 * NOTE: Dropwizard Histogram is a concrete class and there is no an interface for Dropwizard Histogram logic,
 *   so we have to create an alternative API hierarchy.
 */
public class ThreadLocalHistogram extends com.codahale.metrics.Histogram implements Histogram
{
    private static final Logger logger = LoggerFactory.getLogger(ThreadLocalHistogram.class);
    private final Reservoir reservoir;
    private final int countMetricId;

    /**
     * Creates a new {@link ThreadLocalHistogram} with the given reservoir.
     *
     * @param reservoir the reservoir to create a histogram from
     */
    public ThreadLocalHistogram(Reservoir reservoir)
    {
        super(reservoir);
        try
        {
            this.reservoir = reservoir;
            this.countMetricId = ThreadLocalMetrics.allocateMetricId();
            ThreadLocalMetrics.destroyWhenUnreachable(this, countMetricId);
            ReflectionUtils.setFieldToNull(this, "count"); // reduce metrics memory footprint
        }
        catch (Throwable e)
        {
            logger.error("fail", e);
            throw e;
        }
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    public void update(int value)
    {
        try
        {
            update((long) value);
        }
        catch (Throwable e)
        {
            logger.error("fail", e);
            throw e;
        }
    }

    /**
     * Adds a recorded value.
     *
     * @param value the length of the value
     */
    public void update(long value)
    {
        try
        {
            ThreadLocalMetrics.add(countMetricId, 1);
            reservoir.update(value);
        }
        catch (Throwable e)
        {
            logger.error("fail", e);
            throw e;
        }
    }

    /**
     * Returns the number of values recorded.
     *
     * @return the number of values recorded
     */
    @Override
    public long getCount()
    {
        try
        {
            return ThreadLocalMetrics.getCount(countMetricId);
        }
        catch (Throwable e)
        {
            logger.error("fail", e);
            throw e;
        }
    }

    public void reset()
    {
        try
        {
            ThreadLocalMetrics.getCountAndReset(countMetricId);
        }
        catch (Throwable e)
        {
             logger.error("fail", e);
            throw e;
        }
    }

    @Override
    public Snapshot getSnapshot()
    {
        try
        {
        return reservoir.getSnapshot();
        }
        catch (Throwable e)
        {
            logger.error("fail", e);
            throw e;
        }
    }
}
