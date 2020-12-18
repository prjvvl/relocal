package com.pentagon.localhost_reborn.Object;

public class Product {
    private String productID;
    private String userID;
    private String serviceID;
    private String title;
    private String description;
    private String imgUri;
    private String likes;

    public Product() {

    }

    public Product(String productID, String userID, String serviceID, String title, String description, String imgUri, String likes) {
        this.productID = productID;
        this.userID = userID;
        this.serviceID = serviceID;
        this.title = title;
        this.description = description;
        this.imgUri = imgUri;
        this.likes = likes;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getServiceID() {
        return serviceID;
    }

    public void setServiceID(String serviceID) {
        this.serviceID = serviceID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImgUri() {
        return imgUri;
    }

    public void setImgUri(String imgUri) {
        this.imgUri = imgUri;
    }

    public String getLikes() {
        return likes;
    }

    public void setLikes(String likes) {
        this.likes = likes;
    }
}
