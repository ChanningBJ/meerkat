package com.qiyi.mbd.common.meter.fusing;

import com.codahale.metrics.MetricRegistry;
import com.qiyi.mbd.common.meter.HistoryRatioGauge;
import com.qiyi.mbd.common.meter.LogicMeterBuilder;
import com.qiyi.mbd.common.meter.MeterCenter;
import com.qiyi.mbd.common.meter.OperationMeter;
import lombok.extern.log4j.Log4j;
import org.aeonbits.owner.ConfigFactory;
import org.aeonbits.owner.event.ReloadEvent;
import org.aeonbits.owner.event.ReloadListener;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by chengmingwang on 1/18/17.
 */
@Log4j
public class FusingMeter extends OperationMeter {

    private FusingConfig fusingConfig;
    private AtomicLong timestamp4FusingStart = new AtomicLong(0);  //熔断开始的毫秒时间戳,如果是0表示没有熔断

    private Class measuringObjectClass;
    private final String instanceID;
    private final String configInstanceName;
    private StatusChangedHandler statusChangeHandler = null;
    private AtomicInteger normalCounter = new AtomicInteger(0);
    private AtomicInteger fusingCounter = new AtomicInteger(0);
    private HistoryRatioGauge historyRatioGauge;


    public static class Builder extends LogicMeterBuilder<FusingMeter> {

        private final Class<? extends FusingConfig> configCls;

        public Builder(Class<? extends FusingConfig> configCls) {
            this.configCls = configCls;
        }

        @Override
        public FusingMeter build() {
            return new FusingMeter(
                    this.metricRegistry,
                    this.getMeterIdentity(),
                    this.measuringObjectClass,
                    this.instanceID,
                    this.configCls
            );
        }
    }

    protected FusingMeter(MetricRegistry metricRegistry,
                          String meterIdentity,
                          Class measuringObjectClass,
                          String instanceID,
                          final Class<? extends FusingConfig> configCls
    ) {
        super(metricRegistry, meterIdentity, measuringObjectClass, instanceID);
        this.historyRatioGauge = new HistoryRatioGauge(
                measuringObjectClass,
                MeterCenter.INSTANCE.getReporterCycleSecond()/MeterCenter.INSTANCE.getUpdaterCyclesecond()
        );
        metricRegistry.register(MetricRegistry.name(measuringObjectClass, instanceID, "normal-rate"),
                this.historyRatioGauge
        );
        if(instanceID==null) {
            this.configInstanceName = measuringObjectClass.getSimpleName();
        } else {
            this.configInstanceName = measuringObjectClass.getSimpleName()+"."+instanceID;
        }
        this.measuringObjectClass = measuringObjectClass;
        this.instanceID = instanceID;

        HashMap map = new HashMap();
        map.put(FusingConfig.CONFIG_KEY_INSTANCE, configInstanceName);
        fusingConfig = ConfigFactory.create(configCls, map);
        fusingConfig.addReloadListener(new ReloadListener() {
            @Override
            public void reloadPerformed(ReloadEvent reloadEvent) {
                log.info(
                        configCls.getSimpleName()+"("+configInstanceName+") reloaded\n"+
                                "OLD "+reloadEvent.getOldProperties().toString()+"\n"+
                                "NEW "+reloadEvent.getNewProperties().toString());
                logFusingConfig();
            }
        });
        log.info(this.getClass().getName()+"("+configInstanceName+") initialized");
        this.logFusingConfig();
    }

    private void logFusingConfig(){
        log.info("fusing."+configInstanceName+".mode = "+fusingConfig.fusingMode().name());
        log.info("fusing."+configInstanceName+".duration = "+fusingConfig.fusingTime().toString());
        log.info("fusing."+configInstanceName+".success_rate_threshold = "+fusingConfig.fusingRatio());
    }

    public void setStatusChangedHandler(StatusChangedHandler handler){
        this.statusChangeHandler = handler;
    }

    @Override
    public void refreshStatus() {
        super.refreshStatus();

        switch(fusingConfig.fusingMode()){
            case AUTO_FUSING:
                if(timestamp4FusingStart.get()==0){ //没有熔断,检查是否需要进入熔断状态
                    if(this.getUpdateCycleSuccessRatio()<fusingConfig.fusingRatio()*100){ //达到熔断阈值
                        if(this.statusChangeHandler!=null){
                            this.statusChangeHandler.beforeFusingStart();
                        }
                        timestamp4FusingStart.set(System.currentTimeMillis());
                        log.info(MetricRegistry.name(this.measuringObjectClass, this.instanceID, "fusing")
                                +" Fusing triggered, success rate = "+this.getUpdateCycleSuccessRatio());
                    }
                } else { //处于熔断状态, 检查是否超过了熔断时间
                    long now = System.currentTimeMillis();
                    if(now-timestamp4FusingStart.get()>=fusingConfig.fusingTime().getTimeMilliseconds()){ //熔断时间超过了预设的时间段
                        if(this.statusChangeHandler!=null){
                            this.statusChangeHandler.beforeFusingEnd();
                        }
                        timestamp4FusingStart.set(0); //关闭熔断, 会恢复正常访问
                        log.info(MetricRegistry.name(this.measuringObjectClass, this.instanceID, "fusing")+" Fusing switched off");
                    }
                }
                break;
            case FORCE_NORMAL:
                timestamp4FusingStart.set(0);
                break;
            case FORCE_FUSING:
                timestamp4FusingStart.set(Long.MAX_VALUE);
                break;
        }

        if(timestamp4FusingStart.get()==0){
            this.normalCounter.incrementAndGet();
            this.historyRatioGauge.append(1,1);
        } else {
            this.fusingCounter.incrementAndGet();
            this.historyRatioGauge.append(0,1);
        }
    }

    /**
     * 判断是否处于熔断状态
     * @return True-处于熔断状态  False-没有处于熔断状态、
     */
    public boolean isFusing(){
        return timestamp4FusingStart.get()>0;
    }

    /**
     * Created by chengmingwang on 2/14/17.
     */
    public interface StatusChangedHandler {

        /**
         * 开始熔断以后调用这个函数
         */
        void beforeFusingStart();

        /**
         * 熔断结束的前一刻调用这个函数
         */
        void beforeFusingEnd();

    }
}
