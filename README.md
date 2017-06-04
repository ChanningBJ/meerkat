# meerkat
meerkat 是一个服务监控以及服务降级基础组件，主要为了解决调用外部接口的时候进行成功率，响应时间，QPS指标的监控，同时在成功率下降到预设的阈值以下的时候自动切断外部接口的调用，外部接口成功率恢复后自动恢复请求。

## 主要功能

1. 监控操作的成功率以及响应时间指标
1. log文件和Grafhite两种监控指标上报方式，支持扩展其他的上报方式
1. （可选功能）成功率下降到预设的阈值以下触发熔断保护，暂定对外部接口的访问，成功率恢复以后自动恢复访问

## Why meerkat


## Maven 

```xml
<dependency>
    <groupId>com.github.qiyimbd</groupId>
    <artifactId>meerkat</artifactId>
    <version>1.0</version>
</dependency>
```



## 如何使用

### 定义操作

假设我们的服务中需要从HTTP接口查询一个节目的播放次数，为了防止这个HTTP接口大量超时影响我们自身服务的质量，可以定义一个查询Command：

```java
public class GetPlayCountCommand extends FusingCommand<Long> {

    private final Long videoID;

    public GetPlayCountCommand(Long videoID) {
        this.videoID = videoID;
    }
        
    protected Optional<Long> run() {
        Long result = 0l;
        // 调用HTTP接口获取视频的播放次数信息
        // 如果调用失败，返回 null 或者抛出异常，会将这次操作记录为失败
        // 如果ID非法，返回 Optional.absent(),会将这次操作记录为成功
        return Optional.fromNullable(result);
    }
}
```  

执行查询：

```java
//获取视频ID为123的视频的播放次数
GetPlayCountCommand command = new GetPlayCountCommand(123l);
Long result = command.execute(); // 执行查询操作，如果执行失败或者处于熔断状态，返回 null 
```

### 配置监控上报

首先定义一个接口，继承自GraphiteReporterConfig，通过这个接口定义配置文件的加载路径。配置文件路径的定义方法请参照 [owner 文档](http://owner.aeonbits.org)




```java
@Config.Sources("classpath:config.properties")
public interface MyConfig extends GraphiteReporterConfig {
}
```




配置文件中需要定义下列内容：


配置项 | 含义
------------ | -------------
meter.reporter.enabled.hosts | 开启监控上报的服务器列表
meter.reporter.perfix ｜上报使用的前缀
meter.reporter.carbon.host | grafana(carbon-cache) 的 IP 地址，用于存储监控数据
meter.reporter.carbon.port| grafana(carbon-cache) 的端口
