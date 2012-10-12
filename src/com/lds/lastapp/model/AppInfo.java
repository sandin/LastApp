package com.lds.lastapp.model;

/**
 * 应用程序信息
 */
public class AppInfo {
    private long id;
    
    public String applicationLabel; // index
    public String packageName; // index
    public long firstInstallTime;
    public long lastUpdateTime;
    
    private String applicationLabelPinYin; // 拼音
    private int weight;
    private int runCount;
    private long lastRunTime;
    
    private boolean fixed; // 锁定到快捷栏
    
    public AppInfo() {
        
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getApplicationLabel() {
        return applicationLabel;
    }

    public void setApplicationLabel(String applicationLabel) {
        this.applicationLabel = applicationLabel;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public long getFirstInstallTime() {
        return firstInstallTime;
    }

    public void setFirstInstallTime(long firstInstallTime) {
        this.firstInstallTime = firstInstallTime;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getApplicationLabelPinYin() {
        return applicationLabelPinYin;
    }

    public void setApplicationLabelPinYin(String applicationLabelPinYin) {
        this.applicationLabelPinYin = applicationLabelPinYin;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

    public long getLastRunTime() {
        return lastRunTime;
    }

    public void setLastRunTime(long lastRunTime) {
        this.lastRunTime = lastRunTime;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }
}
