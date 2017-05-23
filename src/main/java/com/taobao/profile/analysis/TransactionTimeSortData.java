package com.taobao.profile.analysis;

import java.util.List;

/**
 * Created by lanpishu on 5/23/17.
 */
public class TransactionTimeSortData implements Comparable {

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public List<Long> getMethodIdList() {
        return methodIdList;
    }

    public void setMethodIdList(List<Long> methodIdList) {
        this.methodIdList = methodIdList;
    }

    private long threadId;

    private long time;

    private List<Long> methodIdList;

    public TransactionTimeSortData(long threadId, long time, List<Long> methodIdList) {
        this.threadId = threadId;
        this.time = time;
        this.methodIdList = methodIdList;
    }

    @Override
    public int compareTo(Object transactionTimeSortData) {
        return -(int)(time - ((TransactionTimeSortData)transactionTimeSortData).time);
    }

}
