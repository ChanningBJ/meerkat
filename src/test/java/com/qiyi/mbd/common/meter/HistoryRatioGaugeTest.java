package com.qiyi.mbd.common.meter;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created by chengmingwang on 3/14/17.
 */
public class HistoryRatioGaugeTest {
    @Test
    public void getRatio() throws Exception {
        HistoryRatioGauge gauge = new HistoryRatioGauge(HistoryRatioGaugeTest.class, 10);
        Assert.assertEquals(gauge.getRatio().getValue(), Double.NaN);

        gauge.append(100l,100l);
        Assert.assertEquals(gauge.getRatio().getValue(), 100.0);
    }

}