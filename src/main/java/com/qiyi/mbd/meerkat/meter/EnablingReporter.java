package com.qiyi.mbd.meerkat.meter;

import com.codahale.metrics.MetricRegistry;

/**
 * Created by chengmingwang on 2/28/17.
 */
public interface EnablingReporter {
    void invoke(MetricRegistry metricRegistry, long period, java.util.concurrent.TimeUnit timeUnit);
}
