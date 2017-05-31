package com.qiyi.mbd.meerkat.meter;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.extern.log4j.Log4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by chengmingwang on 10/6/2016.
 */
@Log4j
public class OperationMeter extends HistoryRatioGauge implements LogicMeter{

    public enum Result{
        SUCCESS,
        FAILURE;
    }
    private final AtomicLong success = new AtomicLong(0);
    private final AtomicLong failure   = new AtomicLong(0);
    private final AtomicDouble successRate = new AtomicDouble(1);
//    private Ratio ratio = Ratio.of(100,1);   // 默认成功率为100%
    private Timer timer;
    private String name;

    public static class Builder extends LogicMeterBuilder<OperationMeter>{

        @Override
        public OperationMeter build() {
            return new OperationMeter(
                    this.metricRegistry,
                    this.getMeterIdentity(),
                    this.measuringObjectClass,
                    this.instanceID
            );
        }
    }

    protected OperationMeter(MetricRegistry metricRegistry, String meterIdentity, Class measuringObjectClass, String instanceID){
        super(measuringObjectClass, MeterCenter.INSTANCE.getReporterCycleSecond()/MeterCenter.INSTANCE.getUpdaterCyclesecond());
        this.name = meterIdentity;
        String successRateName = MetricRegistry.name(measuringObjectClass, instanceID, "success-rate");
        metricRegistry.register(successRateName, this);
        this.timer = metricRegistry.timer(MetricRegistry.name(measuringObjectClass,instanceID, "time"));

    }
//
//    public void init(Class cls, String instanceID, MetricRegistry metricRegistry, Object extraParameters) {
//        name = cls.getName()+"."+instanceID;
//        String successRateName = MetricRegistry.name(cls, instanceID, "success-rate");
//        metricRegistry.register(successRateName, this);
//        timer = metricRegistry.timer(MetricRegistry.name(cls,instanceID, "time"));
//    }


    public void refreshStatus(){
        long successCount = this.success.getAndSet(0);
        long failureCount = this.failure.getAndSet(0);

        this.append(successCount, successCount+failureCount);

        if( successCount + failureCount != 0) {
            this.successRate.set(1.0*successCount/(successCount+failureCount));
            log.debug(name + " >> SUCC:"+successCount+" FAIL:"+failureCount+" RATIO:"+this.successRate.get());
        } else {
            this.successRate.set(1);
            log.debug(name + " >> SUCC:"+successCount+" FAIL:"+failureCount+" RATIO:"+this.successRate.get());
        }
        log.debug(name + " >> SUCC:"+successCount+" FAIL:"+failureCount+" RATIO:"+this.successRate.get());
    }

    public double getUpdateCycleSuccessRatio(){
        return 100*this.successRate.get();
    }

    public Timer.Context startOperation(){
        return timer.time();
    }

    /**
     *
     * @param context startOperation()的返回值
     * @param result  操作成功/失败
     * @param unit    返回的操作耗时的时间单位
     * @return 操作耗时
     */
    public long endOperation(Timer.Context context, Result result, TimeUnit unit){
        switch (result){
            case SUCCESS: this.success.incrementAndGet(); break;
            case FAILURE: this.failure.incrementAndGet(); break;
        }
        return unit.convert(context.stop(), TimeUnit.NANOSECONDS);
    }

    public void endOperation(Timer.Context context, Result result){
        context.stop();
        switch (result){
            case SUCCESS: this.success.incrementAndGet(); break;
            case FAILURE: this.failure.incrementAndGet(); break;
        }
    }

}
