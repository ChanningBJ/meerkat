package com.meerkat.meter;

import com.google.common.base.Optional;
import com.meerkat.fusing.FusingCommand;
import com.meerkat.fusing.FusingMeter;

import static com.meerkat.meter.Command.STATUS.*;

/**
 * 请求成功返回1,请求失败返回2,熔断状态返回3
 */
public class Command extends FusingCommand<Integer> {

    enum STATUS {
        NORMAL_SUCCESS(1),
        NORMAL_FAILURE(2),
        FUSING(3);

        private int id;

        STATUS(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    static int successRate = 100;
    static int counter = 0;

    public Command() {
        super(APPFusingConfig.class);
    }

    @Override
    protected Optional<Integer> run() {
        counter += 1;
        if (counter % 100 > successRate) {
            return null;
        } else {
            return Optional.fromNullable(NORMAL_SUCCESS.getId());
        }
    }

    @Override
    protected Integer getFallback(boolean isFusing, Exception e) {
        if (isFusing) {
            return FUSING.getId();
        } else {
            return NORMAL_FAILURE.getId();
        }
    }

    public FusingMeter getMeter() {
        return this.meter;
    }
}
