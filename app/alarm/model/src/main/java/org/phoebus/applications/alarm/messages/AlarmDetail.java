package org.phoebus.applications.alarm.messages;

public class AlarmDetail {

    private String title;
    private String details;
    private int delay;

    public AlarmDetail() {
        super();
    }

    public AlarmDetail(String title, String action) {
        super();
        this.title = title;
        this.details = action;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public String toString() {
        return "AlarmDetail [title=" + title + ", details=" + details + ", delay=" + delay + "]";
    }

}
