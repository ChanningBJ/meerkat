package com.qiyi.mbd.meerkat.meter;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import lombok.Getter;
import lombok.extern.log4j.Log4j;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by chengmingwang on 11/6/2016.
 */
@Log4j
@Getter
public enum MeterCenter {
    INSTANCE;

    //TODO: 屏蔽掉LogicMeter的report

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final ConcurrentHashMap<String, LogicMeter> meters = new ConcurrentHashMap<String, LogicMeter>();
    private LogicMeterStatusUpdater updater;
    private Graphite graphiteReporter = null;
    private ConcurrentHashMap<
            Class<? extends LogicMeter>,
            Class<? extends LogicMeterBuilder>
            > builderMap = new ConcurrentHashMap<>();
    private int updaterCyclesecond = 10;
    private int reporterCycleSecond = 60;

    private List<EnablingReporter> reporterList = new ArrayList<>();

    MeterCenter(){
        builderMap.put(OperationMeter.class, OperationMeter.Builder.class);
        builderMap.put(PoolMeter.class,PoolMeter.Builder.class);
    }

    /**
     * Initialize MeterCenter
     *
     * This method should be called before using any meter instance, will perform following operations:
     * 1. start meter updater task, will calculate the success rate every 10 seconds by default,
     * the time cycle can be changed by {@link #setUpdaterCycleSecond(int) setReporterCycleSecond} method
     * 2. initialize the reporter, will report all metrics every 60 seconds,
     * the time cycle can be changed by {@link #setReporterCycleSecond(int) setReporterCycleSecond} method,
     * the reporter can be enabled by {@link #enableReporter(EnablingReporter enablingReporter) enableReporter} method
     */
    public void init(){
        updater = new LogicMeterStatusUpdater(this.metricRegistry);
        updater.start(updaterCyclesecond, TimeUnit.SECONDS);  //默认10秒执行一次
        for(EnablingReporter enablingReporter: this.reporterList) {
            enablingReporter.invoke(this.metricRegistry, this.reporterCycleSecond, TimeUnit.SECONDS);
        }

    }

    /**
     *
     * @param updaterCycleMinute
     * @return
     */
    public MeterCenter setUpdaterCycleSecond(int updaterCycleMinute) {
        this.updaterCyclesecond = updaterCycleMinute;
        return this;
    }

    public MeterCenter setReporterCycleSecond(int reporterCycleSecond){
        this.reporterCycleSecond = reporterCycleSecond;
        return this;
    }

//    public final Graphite getGraphiteReporter(){
//        return this.graphiteReporter;
//    }


    public final MetricRegistry getMetricRegistry(){
        return this.metricRegistry;
    }

    public MeterCenter enableReporter(EnablingReporter enablingReporter){
        this.reporterList.add(enablingReporter);
        return this;
    }

    public void registerMeterBuilder(Class<? extends LogicMeter> meterClass, Class<? extends LogicMeterBuilder> builderClass){
        this.builderMap.put(meterClass,builderClass);
    }

    @Deprecated
    public <T extends LogicMeter> T createMeter(Class measuringObjectClass, Class<? extends T> meterClass) {
        return getOrCreateMeter(measuringObjectClass, "", meterClass, null);
    }

    public <T extends LogicMeter> T getOrCreateMeter(Class measuringObjectClass, Class<? extends T> meterClass) {
        return getOrCreateMeter(measuringObjectClass, "", meterClass, null);
    }

    @Deprecated
    public <T extends LogicMeter> T createMeter(Class measuringObjectClass,
                                                Class<? extends T> meterClass,
                                                LogicMeterBuilder<? extends T> meterBuilder) {
        return getOrCreateMeter(measuringObjectClass, null, meterClass, meterBuilder);
    }

    public <T extends LogicMeter> T getOrCreateMeter(Class measuringObjectClass,
                                                     Class<? extends T> meterClass,
                                                     LogicMeterBuilder<? extends T> meterBuilder) {
        return getOrCreateMeter(measuringObjectClass, null, meterClass, meterBuilder);
    }

    @Deprecated
    public <T extends LogicMeter> T createMeter(Class measuringObjectClass,
                                                String instanceID,
                                                Class<? extends T> meterClass
    ) {
        return getOrCreateMeter(measuringObjectClass, instanceID, meterClass, null);
    }

    public <T extends LogicMeter> T getOrCreateMeter(Class measuringObjectClass,
                                                     String instanceID,
                                                     Class<? extends T> meterClass
    ) {
        return getOrCreateMeter(measuringObjectClass, instanceID, meterClass, null);
    }

    public <T extends LogicMeter> T getOrCreateMeter(Class measuringObjectClass,
                                                     String instanceID,
                                                     Class<? extends T> meterClass,
                                                     LogicMeterBuilder<? extends T> meterBuilder
                                                ) {

        LogicMeterBuilder<? extends T> builder = meterBuilder;
        if(builder==null) { //如果参数中指定了builder,优先使用参数中的
            Class<? extends LogicMeterBuilder> builderClass = this.builderMap.get(meterClass);
            if(builderClass==null){
                throw new RuntimeException("Unsupported meter "+meterClass.getName()+"\nUse MeterCenter.INSTANCE.registerMeterBuilder to regist");
            }
            try {
                builder = builderClass.newInstance();
            } catch (InstantiationException e) {
                log.error(meterClass.getName(), e);
                return null;
            } catch (IllegalAccessException e) {
                log.error(meterClass.getName(), e);
                return null;
            }
        }

        builder.setInstanceID(instanceID);
        builder.setMeasuringObjectClass(measuringObjectClass);
        builder.setMetricRegistry(this.getMetricRegistry());

        String meterName = builder.getMeterIdentity();
        LogicMeter meter = this.meters.get(meterName);

        if (meter != null) {
            return (T) meter;
        }

        synchronized (this) {
            meter = this.meters.get(meterName);
            if(meter!=null){
                return (T) meter;
            }
            meter = builder.build();
            this.meters.put(meterName, meter);
            return (T) meter;
        }
    }

    static public void main(String[] args) throws UnknownHostException, SocketException {
        System.out.println("Your Host addr: " + InetAddress.getLocalHost().getHostAddress());  // often returns "127.0.0.1"
        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        for (; n.hasMoreElements();)
        {
            NetworkInterface e = n.nextElement();

            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements();)
            {
                InetAddress addr = a.nextElement();
                System.out.println("  " + addr.getHostAddress());
            }
        }
    }
}
