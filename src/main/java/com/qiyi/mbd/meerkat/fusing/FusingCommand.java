package com.qiyi.mbd.meerkat.fusing;


import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.qiyi.mbd.meerkat.meter.MeterCenter;
import com.qiyi.mbd.meerkat.meter.OperationMeter;
import lombok.extern.log4j.Log4j;

/**
 * Created by chengmingwang on 1/17/17.
 */
@Log4j
public abstract class FusingCommand<T> {

    protected final FusingMeter meter;
    private Optional<T> result = Optional.absent();

    /**
     *
     * @param instanceName
     * @param configCls
     */
    public FusingCommand(String instanceName, Class<? extends FusingConfig> configCls) {

        this.meter = MeterCenter.INSTANCE.getOrCreateMeter(
                this.getClass(),
                instanceName,
                FusingMeter.class,
                new FusingMeter.Builder(configCls));
    }

    /**
     *
     * @param configCls
     */
    public FusingCommand(Class<? extends FusingConfig> configCls) {
        this.meter = MeterCenter.INSTANCE.getOrCreateMeter(
                this.getClass(),
                FusingMeter.class,
                new FusingMeter.Builder(configCls));
    }

    public FusingCommand(){
        this.meter = MeterCenter.INSTANCE.getOrCreateMeter(
                this.getClass(),
                FusingMeter.class,
                new FusingMeter.Builder(FusingConfig.class));
    }

    public FusingCommand(FusingMeter fusingMeter){
        this.meter = fusingMeter;
    }

    /**
     * 返回null或者抛出异常都会认为是执行失败
     * @return null-请求失败
     */
    abstract protected Optional<T> run();

    /**
     *
     * @param isFusing true: 处于熔断状态   false: 没有处于熔断状态,请求失败导致的调用Fallback
     * @param e        run() 抛出的异常,如果没有抛出异常或者处于熔断状态,这个参数会是null
     * @return 安全数据
     */
    protected T getFallback(boolean isFusing, Exception e){
        return null;
    }

    /**
     * 处于熔断状态或者请求失败,都会使用getFallback的返回结果
     * @return
     */
    public T execute(){
        if(this.meter.isFusing()){ //如果处于熔断状态,直接返回 fallback
            return this.getFallback(true, null);
        }
        Timer.Context context = this.meter.startOperation();
        try{
            this.result = this.run();
            if(this.result==null){
                this.meter.endOperation(context, OperationMeter.Result.FAILURE);
                this.result = Optional.fromNullable(this.getFallback(false, null));
                return result.orNull();
            } else {
                this.meter.endOperation(context, OperationMeter.Result.SUCCESS);
                return this.result.orNull();
            }
        } catch (Exception e){
            this.meter.endOperation(context, OperationMeter.Result.FAILURE);
            this.result = Optional.fromNullable(this.getFallback(false, e));
            return this.result.orNull();
        }
    }

    public Optional<T> getResult() {
        return result;
    }
}
