package com.meerkat.meter;

import com.google.common.base.Optional;
import com.meerkat.fusing.FusingCommand;
import com.meerkat.fusing.FusingMeter;

/**
 * Created by chengmingwang on 6/10/17.
 */
public class MultiInstanceCommand extends FusingCommand<Integer> {

    private static final int successRateA = 100;   //实例A成功率100%
    private static final int successRateB = 60;    //实例B成功率60%
    private static int counterA = 0;
    private static int counterB = 0;

    private final String instance;

    public MultiInstanceCommand(String instance) {
        super(instance, APPFusingConfig.class);
        this.instance = instance;
    }

    @Override
    protected Optional<Integer> run() {
        if(this.instance.equals("A")){
            counterA += 1;
            if (counterA % 100 >= successRateA) {
                return null;
            } else {
                return Optional.fromNullable(1);
            }
        } else {
            counterB += 1;
            if (counterB % 100 >= successRateB) {
                return null;
            } else {
                return Optional.fromNullable(1);
            }
        }
    }

    public FusingMeter getMeter() {
        return this.meter;
    }
}
