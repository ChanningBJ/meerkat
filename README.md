# meerkat
meerkat 是一个服务监控以及服务降级基础组件，主要为了解决调用外部接口的时候进行成功率，响应时间，QPS指标的监控，同时在成功率下降到预设的阈值以下的时候自动切断外部接口的调用，外部接口成功率恢复后自动恢复请求。

## 主要功能

1. 监控操作的成功率以及响应时间指标
1. log文件和Grafhite两种监控指标上报方式，支持扩展其他的上报方式
1. （可选功能）成功率下降到预设的阈值以下触发熔断保护，暂定对外部接口的访问，成功率恢复以后自动恢复访问




## Maven 

```xml
<dependency>
    <groupId>com.github.qiyimbd</groupId>
    <artifactId>meerkat</artifactId>
    <version>1.0</version>
</dependency>
```



## 如何使用

### 配置监控上报

首先定义一个接口，继承自GraphiteReporterConfig，通过这个接口定义配置文件的加载路径。配置文件路径的定义方法请参照 [owner 文档](http://owner.aeonbits.org)




```java
@Config.Sources("classpath:config.properties")
public interface MyConfig extends GraphiteReporterConfig {
}
```




配置文件中需要定义下列内容：

配置项｜含义
------------ | -------------
meter.reporter.enabled.hosts|开启监控上报的服务器列表
