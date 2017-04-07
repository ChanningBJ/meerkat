package com.qiyi.mbd.common.meter;

import com.codahale.metrics.*;

/**
 * Created by chengmingwang on 2/22/17.
 */
public class PoolMeter extends RatioGauge implements LogicMeter {

    private MetricRegistry metricRegistry;

    @Override
    public void refreshStatus() {

    }

    public static class Builder extends LogicMeterBuilder<PoolMeter>{

        @Override
        public PoolMeter build() {
            PoolMeter poolMeter = new PoolMeter();
            poolMeter.instanceID = instanceID;
            poolMeter.cls = this.measuringObjectClass;
            poolMeter.metricRegistry = metricRegistry;

            poolMeter.successMeter = metricRegistry.meter(MetricRegistry.name(this.measuringObjectClass,instanceID,"success"));
            poolMeter.failureMeter = metricRegistry.meter(MetricRegistry.name(this.measuringObjectClass,instanceID,"failure"));
            poolMeter.timer = metricRegistry.timer(MetricRegistry.name(this.measuringObjectClass,instanceID, "time"));
            metricRegistry.register(MetricRegistry.name(this.measuringObjectClass,instanceID,"success-rate"),poolMeter);
            poolMeter.poolActiveSize = metricRegistry.counter(MetricRegistry.name(this.measuringObjectClass,instanceID,"pool-active-size"));

            return poolMeter;
        }
    }

    public enum Result{
        SUCCESS,
        FAILURE;
    }

    private Timer timer; //Pool 的等待时间
    private Meter successMeter; //从pool中获取数据成功
    private Meter failureMeter; //从pool中获取数据失败
    private Counter poolActiveSize;   //对象池的大小

    private Class cls;
//    private MetricRegistry metricRegistry;
    private String instanceID;

//    @Override
//    public void init(Class cls, String instanceID, MetricRegistry metricRegistry, Object extraParameters) {
//        this.instanceID = instanceID;
//        this.cls = cls;
//        this.metricRegistry = metricRegistry;
//        successMeter = metricRegistry.meter(MetricRegistry.name(cls,instanceID,"success"));
//        failureMeter = metricRegistry.meter(MetricRegistry.name(cls,instanceID,"failure"));
//        timer = metricRegistry.timer(MetricRegistry.name(cls,instanceID, "time"));
//        metricRegistry.register(MetricRegistry.name(cls,instanceID,"success-rate"),this);
//        this.poolActiveSize = metricRegistry.counter(MetricRegistry.name(cls,instanceID,"pool-active-size"));
//    }

    public void registMeter(String name, Metric metric){
        String meterName = MetricRegistry.name(cls, this.instanceID, name);
        try {
            metricRegistry.register(meterName, metric);
        } catch (IllegalArgumentException e){
            metricRegistry.remove(meterName);
            metricRegistry.register(meterName, metric);
        }
    }

    public Timer.Context beforeAcquire(){
        return this.timer.time();
    }

    @Deprecated
    public void afterAcquire(Timer.Context context, Result result, String subPoolName){
        this.afterAcquire(context,result);
    }

    public void afterAcquire(Timer.Context context, Result result){
        context.stop();
        switch (result){
            case SUCCESS:
                this.successMeter.mark();
                this.poolActiveSize.inc();
                break;
            case FAILURE: this.failureMeter.mark(); break;
        }
    }

    @Deprecated
    public void onRelease(String subPoolName){
        this.onRelease();
    }
    public void onRelease(){
        this.poolActiveSize.dec();
    }
    @Override
    protected Ratio getRatio() {
        return Ratio.of(this.successMeter.getOneMinuteRate(),
                this.failureMeter.getOneMinuteRate()+this.successMeter.getOneMinuteRate());
    }
}
