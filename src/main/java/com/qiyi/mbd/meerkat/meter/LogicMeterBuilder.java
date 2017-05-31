package com.qiyi.mbd.meerkat.meter;

import com.codahale.metrics.MetricRegistry;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by chengmingwang on 3/6/17.
 */
@AllArgsConstructor
@NoArgsConstructor
@Setter
public abstract class LogicMeterBuilder<T extends LogicMeter> {
    protected Class measuringObjectClass;
    protected String instanceID = null;
    protected MetricRegistry metricRegistry;

    abstract public T build();

    public String getMeterIdentity(){
        if(instanceID!=null){
            return measuringObjectClass.getName() + "-" + instanceID;
        } else {
            return measuringObjectClass.getName();
        }
    }

}
