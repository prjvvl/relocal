package com.pentagon.localhost_reborn.Object;

public class Message {
    private String id;
    private String from;
    private String to;
    private String message;
    private String time;
    private String date;

    public Message() {

    }

    public Message(String id, String from, String to, String message, String time, String date) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.message = message;
        this.time = time;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
