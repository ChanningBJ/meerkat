package com.qiyi.mbd.common.meter;

import com.qiyi.mbd.common.meter.fusing.FusingConfig;
import org.aeonbits.owner.Config;

/**
 * Created by chengmingwang on 4/7/17.
 */
@Config.Sources("classpath:app_config.properties")
public interface APPFusingConfig extends FusingConfig {
}
