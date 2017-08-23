package com.meerkat.fusing;

import com.meerkat.meter.Period;
import org.aeonbits.owner.Config;
import org.aeonbits.owner.Reloadable;

/**
 * Created by chengmingwang on 1/18/17.
 */
//@Config.Sources({
//        "classpath:config.properties"
//})
@Config.HotReload(
        value = 1, unit = java.util.concurrent.TimeUnit.MINUTES,
        type = Config.HotReloadType.ASYNC)
public interface FusingConfig extends Reloadable {

    enum FUSING_MODE {
        AUTO_FUSING,            //自动进入熔断模式(默认)
        FORCE_NORMAL,    //关闭熔断功能
        FORCE_FUSING,    //强制熔断
        ;
    }

    String CONFIG_KEY_INSTANCE = "instance";

    @Key("fusing.${"+CONFIG_KEY_INSTANCE+"}.mode")
    @DefaultValue("FORCE_NORMAL")
    FusingMode fusingMode();

    /**
     * 触发一次熔断的持续时间
     * @return
     */
    @Key("fusing.${"+CONFIG_KEY_INSTANCE+"}.duration")
    @DefaultValue("50sec")
    Period fusingTime();

    /**
     * 触发熔断的阈值。例如0.9表示90%
     * @return
     */
    @Key("fusing.${"+CONFIG_KEY_INSTANCE+"}.success_rate_threshold")
    @DefaultValue("0.9")
    double fusingRatio();
}
