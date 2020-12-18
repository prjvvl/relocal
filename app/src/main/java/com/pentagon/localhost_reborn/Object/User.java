package com.pentagon.localhost_reborn.Object;

public class User {
    private String userName;
    private String email;
    private String userID;
    private String phone;
    private String imgUrl;

    public User() {
    }

    public User(String userName, String email, String userID, String phone, String imgUrl) {
        this.userName = userName;
        this.email = email;
        this.userID = userID;
        this.phone = phone;
        this.imgUrl = imgUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
