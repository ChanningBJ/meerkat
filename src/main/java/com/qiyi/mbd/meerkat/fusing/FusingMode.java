package com.qiyi.mbd.meerkat.fusing;

import lombok.extern.log4j.Log4j;

/**
 * Created by chengmingwang on 4/19/17.
 * ######################################
 * # 0 - 手工设置为正常模式,会直接请求外部接口获取数据,这种状态不会自动触发安全模式
 * # 1 - 手工安全模式,通过手工修改该接口使用安全模式,需要通过手工撤销
 * ######################################
 * # 2 - 自动安全模式,通过监控外部接口的触发安全模式,进入安全模式后过一段时间以后会返回正常模式
 * ######################################
 */

@Log4j
public class FusingMode {
    public enum FUSING_MODE {
        AUTO_FUSING,            //自动进入熔断模式(默认)
        FORCE_NORMAL,    //关闭熔断功能
        FORCE_FUSING,    //强制熔断
        ;
    }

    private FUSING_MODE mode;

    public FUSING_MODE getMode() {
        return mode;
    }

    public FusingMode(String cfg) {
        if("0".equals(cfg)){
            mode = FUSING_MODE.FORCE_NORMAL;
        } else if ("1".equals(cfg)){
            mode = FUSING_MODE.FORCE_FUSING;
        } else if ("2".equals(cfg)){
            mode = FUSING_MODE.AUTO_FUSING;
        } else {
            try {
                mode = FUSING_MODE.valueOf(cfg);
            } catch (Exception e){
                mode = FUSING_MODE.FORCE_NORMAL;
            }
        }
    }
}
