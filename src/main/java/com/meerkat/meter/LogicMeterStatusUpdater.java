package com.meerkat.meter;

import com.codahale.metrics.*;
import lombok.extern.log4j.Log4j;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by chengmingwang on 1/18/17.
 */
@Log4j
public class LogicMeterStatusUpdater extends ScheduledReporter {
    public LogicMeterStatusUpdater(MetricRegistry registry) {
        super(registry, LogicMeterStatusUpdater.class.getName(), new MetricFilter() {
            @Override
            public boolean matches(String name, Metric metric) {
                return metric instanceof LogicMeter;
            }
        }, TimeUnit.MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
        log.debug("LogicMeterStatusUpdater.report start");
        for( Gauge gaugesMeter: gauges.values()){
            LogicMeter logicMeter = (LogicMeter) gaugesMeter;
            logicMeter.refreshStatus();
        }
        log.debug("LogicMeterStatusUpdater.report end");
    }
}
