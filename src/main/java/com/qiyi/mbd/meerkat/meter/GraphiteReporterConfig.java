package com.qiyi.mbd.meerkat.meter;

import org.aeonbits.owner.Config;

/**
 * Created by chengmingwang on 12/3/16.
 */
//@Config.Sources({
//        "classpath:config.properties"
//})
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
