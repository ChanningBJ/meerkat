package com.qiyi.mbd.common.meter;

import com.google.common.base.Optional;
import com.qiyi.mbd.common.meter.fusing.FusingCommand;

/**
 * Created by chengmingwang on 3/24/17.
 */
public class Sample extends FusingCommand<Integer> {

    /**
     * 返回null或者抛出异常都会认为是执行失败
     */
    @Override
    protected Optional<Integer> run() {
        return null;
    }

    @Override
    protected Integer getFallback(boolean isFusing, Exception e) {
        return super.getFallback(isFusing, e);
    }
}
