package com.qiyi.mbd.common.meter;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by chengmingwang on 6/11/2015.
 */
public class Period {
    int timeval;
    private static final String TAIL_MILLISECONDS = "ms";
    private static final String TAIL_SECONDS      = "sec";
    private static final String TAIL_MINTUES      = "min";
    private static final String TAIL_HOUR         = "hour";


    public Period(String timestr) throws Exception {
        String timeStrVal = StringUtils.remove(timestr," ").toLowerCase();
        if(timeStrVal.endsWith(TAIL_MILLISECONDS)){
            timeval = Integer.parseInt(StringUtils.substringBefore(timeStrVal,TAIL_MILLISECONDS));
        } else if (timeStrVal.endsWith(TAIL_SECONDS)){
            timeval = Integer.parseInt(StringUtils.substringBefore(timeStrVal,TAIL_SECONDS))*1000;
        } else if (timeStrVal.endsWith(TAIL_MINTUES)){
            String number = StringUtils.substringBefore(timeStrVal,TAIL_MINTUES);
            timeval = Integer.parseInt(number)*60*1000;
        } else if (timeStrVal.endsWith(TAIL_HOUR)){
            timeval = Integer.parseInt(StringUtils.substringBefore(timeStrVal,TAIL_HOUR))*60*60*1000;
        } else {
            throw new Exception(timestr + "time format error");
        }

    }

    public int getTimeSeconds(){
        return this.timeval/1000;
    }

    public int getTimeMilliseconds(){
        return this.timeval;
    }

    @Override
    public String toString() {
        return timeval+" Milliseconds";
    }
}
