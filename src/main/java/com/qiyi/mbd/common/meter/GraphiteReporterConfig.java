package com.qiyi.mbd.common.meter;

import org.aeonbits.owner.Config;

/**
 * Created by chengmingwang on 12/3/16.
 */
//@Config.LoadPolicy(Config.LoadType.MERGE)
//@Config.Sources({
//        ConfigCenter.CONFIG_SOURCE_FILEPATH_V1,
//        ConfigCenter.CONFIG_SOURCE_FILEPATH_V2,
//        ConfigCenter.CONFIG_SOURCE_FILEPATH_V3,
//        ConfigCenter.CONFIG_SOURCE_FILEPATH_V4,
//        ConfigCenter.CONFIG_SOURCE_FILEPATH,
//        ConfigCenter.CONFIG_SOURCE_CLASSPATH,
//        ConfigCenter.CONFIG_SOURCE_CLASSPATH_V1,
//})
//@Config.HotReload(value = 2, unit = java.util.concurrent.TimeUnit.MINUTES)
public interface GraphiteReporterConfig extends Config {
    @Config.Key("meter.reporter.enabled.hosts")
    String[] enableHosts();

    @Config.Key("meter.reporter.perfix")
    String enablePerfix();

    @Config.Key("meter.reporter.carbon.host")
    String carbonHost();

    @Config.Key("meter.reporter.carbon.port")
    @DefaultValue("2003")
    int carbonPort();

}
