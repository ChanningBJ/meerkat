
package com.qiyi.mbd.meerkat.meter;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import lombok.extern.log4j.Log4j;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.ArrayUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

/**
 * Created by chengmingwang on 2/28/17.
 */
@Log4j
public class EnablingGraphiteReporter implements EnablingReporter {

    private GraphiteReporterConfig config = null;

    public EnablingGraphiteReporter( Class<? extends GraphiteReporterConfig> configCls) {
         this.config = ConfigFactory.create(configCls);
    }
    public EnablingGraphiteReporter( GraphiteReporterConfig configCls) {
        this.config = configCls;
    }

    @Override
    public void invoke(MetricRegistry metricRegistry, long period, TimeUnit timeUnit) {
        if(config.enableHosts()==null){
            log.info("meter.reporter.enabled.hosts is missing in config file, GraphiteReporter disabled");
            return;
        }
        if (config.enablePerfix() == null) {
            log.info("meter.reporter.perfix is missing in config file, GraphiteReporter disabled");
            return;
        }
        if (config.carbonHost() == null) {
            log.info("meter.reporter.carbon.host is missing in config file, GraphiteReporter disabled");
            return;
        }
        Enumeration<NetworkInterface> n = null;
        try {
            n = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
            log.error("Failed get ip address, will not enable GraphiteReporter");
            log.info("Metrics report disabled");
            return;
        }
        for (; n.hasMoreElements(); ) {
            NetworkInterface e = n.nextElement();

            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements(); ) {
                InetAddress addr = a.nextElement();
                if (isMatchedReporterAddress(config.enableHosts(), addr.getHostAddress())) {
                    Graphite graphiteReporter = new Graphite(new InetSocketAddress(config.carbonHost(), config.carbonPort()));
                    String perfix = config.enablePerfix() + "." + addr.getHostAddress().replace(".", "-");
                    GraphiteReporter.forRegistry(metricRegistry)
                            .prefixedWith(perfix)
                            .convertRatesTo(TimeUnit.SECONDS)
                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                            .filter(MetricFilter.ALL)
                            .build(graphiteReporter)
                            .start(period, timeUnit);
                    log.info("Metrics report enabled with perfix: " + perfix);
                    return;
                } else {
                    log.info(addr.getHostAddress() + " against " + ArrayUtils.toString(config.enableHosts()));
                }
            }
        }
        log.info("Metrics report disabled");
        return;
    }

    private boolean isMatchedReporterAddress(final String[] enabledHosts, final String hostAddress) {
        for (String host : enabledHosts) {
            //检查第一个*，然后进行前缀匹配，不带通配符的，进行正常值比对
            int index = host.indexOf("*");
            if (index > -1) {
                String prifixAddress = host.substring(0, index).trim();
                if (hostAddress.startsWith(prifixAddress)) {
                    return true;
                }
            } else {
                if (host.trim().equalsIgnoreCase(hostAddress)) {
                    return true;
                }
            }
        }

        return false;
    }
}
