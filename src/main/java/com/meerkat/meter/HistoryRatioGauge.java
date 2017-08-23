package com.meerkat.meter;

import com.codahale.metrics.RatioGauge;
import lombok.extern.log4j.Log4j;

import java.util.LinkedList;

/**
 * Created by chengmingwang on 3/9/17.
 */
@Log4j
public class HistoryRatioGauge extends RatioGauge {

    public HistoryRatioGauge(Class measuringObjectClass, int size) {
        this.measuringObjectClass = measuringObjectClass;
        this.history = new LinkedList<>();
        int realSize = 1;
        if(size>=realSize){
            realSize = size;
        } else {
            log.warn("Invalid HistoryRatioGauge size: "+size+", using 1 as default");
        }
        for(int k=0; k<realSize; k++) {
            this.history.add(new RatioNumber(0, 0));
        }
    }

    private static class RatioNumber {
        private volatile long numerator;
        private volatile long denominator;

        private RatioNumber(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }
    }

    private final LinkedList<RatioNumber> history;
    private final Class measuringObjectClass;

    public synchronized void append(long numerator, long denominator){
        RatioNumber ratioNumber = this.history.poll();
        ratioNumber.numerator = numerator;
        ratioNumber.denominator = denominator;
        this.history.add(ratioNumber);
//        StringBuilder sb = new StringBuilder();
//        for(RatioNumber number: this.history){
//            sb.append(number.numerator).append("-").append(number.denominator).append(" ");
//        }
//        log.info(this.measuringObjectClass.getSimpleName()+" "+this.toString()+" "+sb.toString());
    }

    public synchronized Ratio calculateRatio(){
        int n=0;
        int d=0;
//        StringBuilder sb = new StringBuilder();
        for(RatioNumber number: this.history){
            n+=number.numerator;
            d+=number.denominator;
//            sb.append(number.numerator).append("-").append(number.denominator).append(" ");
        }
//        log.info(this.measuringObjectClass.getSimpleName()+" "+this.toString()+" "+sb.toString()+" "+Ratio.of(n*100,d).getValue());
        return Ratio.of(n*100,d);
    }

    @Override
    protected Ratio getRatio() {
        return calculateRatio();
    }
}
