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

### 基本使用

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

默认情况下，操作成功率每10秒更新一次，开启通断以后当成功率小于90%的时候进入熔断状态，熔断状态下调用execute()函数会返回null，熔断状态持续50秒，终端终止以后恢复正常访问。

### 定义熔断状态下的返回结果

当处于熔断或者命令执行失败的时候execute()函数会返回null，可以通过Override getFallback这个函数对失败／熔断情况下的返回值进行修改。例如我们希望在播放次数请求失败的情况下默认播放次数是47，可以这样实现：

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
    
    /**
     *
     * @param isFusing 当前是否处于熔断状态
     * @param e        run函数抛出的异常
     * @return 熔断或者请求出现错误的时候execute()函数的返回值
     */
    @Override
    protected Long getFallback(boolean isFusing, Exception e) {
        return 47l;
    }
}
```  








### 配置监控上报


#### 定义配置文件

首先定义一个接口，继承自GraphiteReporterConfig，通过这个接口定义配置文件的加载路径。配置文件路径的定义方法请参照 [owner 文档](http://owner.aeonbits.org), 下面是一个例子：


```java
@Config.Sources("classpath:config.properties")
public interface MyConfig extends GraphiteReporterConfig {
}
```


配置文件中需要定义下列内容：


配置项 | 含义
------------ | -------------
meter.reporter.enabled.hosts | 开启监控上报的服务器列表
meter.reporter.perfix | 上报使用的前缀
meter.reporter.carbon.host | grafana(carbon-cache) 的 IP 地址，用于存储监控数据
meter.reporter.carbon.port| grafana(carbon-cache) 的端口

下面这个例子是在192.168.0.0.1和192.168.0.0.2两台服务器上开启监控数据上报，上报监控指标的前缀是project_name.dc：
```
meter.reporter.enabled.hosts = 192.168.0.0.1,192.168.0.0.2
meter.reporter.perfix = project_name.dc
meter.reporter.carbon.host = hostname.graphite
```
由于相同机房的不同服务器对外部接口的访问情况一般比较类似，所以仅选取部分机器上报，也是为了节省资源。仅选择部分机器上报不影响熔断效果。


#### 初始化配置上报

在服务初始化的时候需要对监控上报进行设置。下面的例子中开启了监控数据向日志文件的打印，同时通过MyConfig指定的配置文件加载Graphite配置信息。

```
MeterCenter.INSTANCE
    .enableReporter(new EnablingLogReporter("org.apache.log4j.RollingFileAppender"))
    .enableReporter(new EnablingGraphiteReporter(MyConfig.class))
    .init();
```


#### 查看统计结果


统计结果会以熔断命令类名为进行分组。例如前面我们定义的 GetPlayCountCommand 类,package name 是 com.qiyi.mbd.test，那么在日志中的输出将会是这个样子：

```
type=GAUGE, name=com.qiyi.mbd.test.GetPlayCountCommand.normal-rate, value=0.0
type=GAUGE, name=com.qiyi.mbd.test.GetPlayCountCommand.success-rate, value=61.0
type=TIMER, name=com.qiyi.mbd.test.GetPlayCountCommand.time, count=25866500, min=0.0, max=0.001, mean=3.963926781047921E-5, stddev=1.951102156677818E-4, median=0.0, p75=0.0, p95=0.0, p98=0.001, p99=0.001, p999=0.001, mean_rate=649806.0831335272, m1=1665370.7316699813, m5=2315813.300713087, m15=2446572.324069477, rate_unit=events/second, duration_unit=milliseconds
```

监控项 | 含义
------------ | -------------
[classname].success-rate | 成功率
[classname].time.m1 | QPS
[classname].time.mean | 平均响应时间
[classname].normal-rate | 过去1分钟内处于正常访问（非熔断）的时间比例


如果配置了Graphite上报，可以看到下面的监控图


![image](https://github.com/qiyimbd/meerkat/blob/master/dashboard.png)
 
关于Graphite+Grafana的配置，可以参考文章：[使用graphite和grafana进行应用程序监控](https://segmentfault.com/a/1190000007540752) 
 
### 配置熔断的阀值和持续时间

首先创建一个接口，继承自FusingConfig，用于指定配置文件的加载路径，同时还可以设定配置文件的刷新时间，具体定义方法请参照 [owner 文档](http://owner.aeonbits.org)

```java
@Config.Sources("classpath:app_config.properties")
@Config.HotReload(
        value = 1, unit = java.util.concurrent.TimeUnit.MINUTES,
        type = Config.HotReloadType.ASYNC)
public interface APPFusingConfig extends FusingConfig {
}
```

创建查询Command的时候在构造函数中传入

```java
public class GetPlayCountCommand extends FusingCommand<Long> {

    private final Long videoID;

    public GetPlayCountCommand(Long videoID) {
        super( APPFusingConfig.class);
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


配置文件定义：


监控项 | 含义 | 默认值
------------ | ------------- | -------------
fusing.[CommandClassName].mode | 熔断模式：<br>FORCE_NORMAL－关闭熔断功能;<br> AUTO_FUSING－自动进入熔断模式;<br> FORCE_NORMAL－强制进行熔断 | FORCE_NORMAL
fusing.[CommandClassName].duration | 触发一次熔断以后持续的时间，支持ms,sec,min 单位。例如 10sec | 50sec
fusing.[CommandClassName].success_rate_threshold | 触发熔断的成功率阀值，降低到这个成功率以下将触发熔断，例如0.9表示成功率90% | 0.9

配置文件中的 CommandClassName 是每个操作类的名称，可以为每个操作单独设置上述参数。同时，这个配置文件支持动态加载，乐意通过修改fusing.[CommandClassName].mode 手工触发或者关闭熔断。

