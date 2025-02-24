package org.jellyfin.androidtv.danmu.model;

public class AutoSkipModel {
    /**
     * id
     */
    private String id;

    /**
     * 片头开始跳过时间
     */
    private int tsTime;

    /**
     * 片头结束跳过时间
     */
    private int teTime;

    /**
     * 片尾开始跳过时间
     */
    private int wsTime;

    /**
     * 片尾结束跳过时间
     */
    private int weTime;

    /**
     * 写入时间
     */
    private long cTime = System.currentTimeMillis();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTsTime() {
        return tsTime;
    }

    public void setTsTime(int tsTime) {
        this.tsTime = tsTime;
    }

    public int getTeTime() {
        return teTime;
    }

    public void setTeTime(int teTime) {
        this.teTime = teTime;
    }

    public int getWsTime() {
        return wsTime;
    }

    public void setWsTime(int wsTime) {
        this.wsTime = wsTime;
    }

    public int getWeTime() {
        return weTime;
    }

    public void setWeTime(int weTime) {
        this.weTime = weTime;
    }

    public long getcTime() {
        return cTime;
    }

    public void setcTime(long cTime) {
        this.cTime = cTime;
    }
}
