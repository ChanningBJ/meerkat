package com.qiyi.mbd.common.meter;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by chengmingwang on 2/28/17.
 */
public class EnablingLogReporter implements EnablingReporter {
    private String loggername;

    public EnablingLogReporter(String loggername) {
        this.loggername = loggername;
    }

    @Override
    public void invoke(MetricRegistry metricRegistry, long period, TimeUnit timeUnit) {
        Slf4jReporter.forRegistry(metricRegistry)
                .outputTo(LoggerFactory.getLogger(loggername))
                .convertRatesTo(java.util.concurrent.TimeUnit.SECONDS)
                .convertDurationsTo(java.util.concurrent.TimeUnit.MILLISECONDS)
                .build().start(period, timeUnit);
    }
}
