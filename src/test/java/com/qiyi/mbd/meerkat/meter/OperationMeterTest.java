package com.qiyi.mbd.meerkat.meter;

import com.codahale.metrics.Timer;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by chengmingwang on 1/28/17.
 */
public class OperationMeterTest {

    @BeforeClass
    public static void setup(){
        MeterCenter.INSTANCE
//                .setUpdaterCycleSecond(10)
//                .setReporterCycleSecond(60)
                .enableReporter(new EnablingLogReporter("org.apache.log4j.ConsoleAppender"))
                .init();
    }

    @Test
    public void init() throws Exception {



        //创建一个操作的计数器
        OperationMeter meter = MeterCenter.INSTANCE.getOrCreateMeter(OperationMeterTest.class, OperationMeter.class);

        //模拟成功率60%
        for(int k=0; k<100; k++){
            Timer.Context context = meter.startOperation();
            if(k%10<6){
                meter.endOperation(context, OperationMeter.Result.SUCCESS);
            } else {
                meter.endOperation(context, OperationMeter.Result.FAILURE);
            }
        }

        //默认统计周期是10秒
        Thread.sleep(1000*10);

        //统计结果成功率60%
        Assert.assertEquals(60.0, meter.getUpdateCycleSuccessRatio());
        Assert.assertEquals(60.0, meter.getRatio().getValue());
        Assert.assertEquals(60.0, meter.getRatio().getValue());


        for(int k=0; k<100; k++){
            Timer.Context context = meter.startOperation();
            if(k%10<7){
                meter.endOperation(context, OperationMeter.Result.SUCCESS);
            } else {
                meter.endOperation(context, OperationMeter.Result.FAILURE);
            }
        }
        Thread.sleep(1000*10);
        Assert.assertEquals(70.0, meter.getUpdateCycleSuccessRatio());

    }

    @Test
    public void report() throws Exception {
        // 默认情况下:
        // 成功率每10秒更新一次, 熔断一次持续50秒
//        MeterCenter.INSTANCE
//                .setReporterCycleSecond(10*60)
//                .enableReporter(new EnablingLogReporter("org.apache.log4j.ConsoleAppender"))
//                .init();

        Command cmd = new Command();
        org.junit.Assert.assertEquals(cmd.getMeter().isFusing(), false);
        Command.successRate = 60;

        loopUntilStatusChanged(true,cmd);

        long time = loopUntilStatusChanged(false, cmd);  //熔断持续大约50秒
        org.junit.Assert.assertTrue(40<=time && time<=60);
        org.junit.Assert.assertEquals(cmd.getMeter().isFusing(), false);

        time = loopUntilStatusChanged(true, cmd);        //正常响应10秒左右,从新进入熔断
        org.junit.Assert.assertTrue(time<=20);
        org.junit.Assert.assertEquals(cmd.getMeter().isFusing(), true);

        time = loopUntilStatusChanged(false, cmd);  //熔断持续大约50秒
        org.junit.Assert.assertTrue(40<=time && time<=60);
        org.junit.Assert.assertEquals(cmd.getMeter().isFusing(), false);

        Command.successRate = 99;                   //成功率设置成99%, 运行30秒不会触发熔断
        long start = System.currentTimeMillis();
        while(true){
            cmd.execute();
            org.junit.Assert.assertFalse(cmd.getMeter().isFusing());
            long cur = System.currentTimeMillis();
            if(cur-start>=30*1000){
                break;
            }
        }
    }

    private long loopUntilStatusChanged(boolean fusingStatus, Command cmd){
        long start = System.currentTimeMillis();
        while(true){
            cmd.execute();
            if(cmd.getMeter().isFusing()==fusingStatus){
                long stop = System.currentTimeMillis();
                return (stop-start)/1000;
            }
            long cur = System.currentTimeMillis();
            if((cur-start)>3*60*1000){  //超过3分钟状态还没有改变,测试失败
                org.junit.Assert.assertTrue(false);
            }
        }
    }

}