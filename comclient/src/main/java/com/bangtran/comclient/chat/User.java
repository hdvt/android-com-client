package com.bangtran.comclient.chat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class User implements Serializable {
    public String userId;
    public String name;
    public String avatarUrl;
    public String role;

    public User(String var1) {
        this.userId = var1;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setUserId(String var1) {
        this.userId = var1;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String var1) {
        this.name = var1;
    }

    public String getAvatarUrl() {
        return this.avatarUrl;
    }

    public void setAvatarUrl(String var1) {
        this.avatarUrl = var1;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String var1) {
        this.role = var1;
    }

    public boolean equals(Object var1) {
        if (var1 instanceof User) {
            User var2 = (User)var1;
            if (this.userId != null && var2.getUserId() != null) {
                return this.userId.equals(var2.getUserId());
            }
        }

        return false;
    }

    public static User parseFrom(JSONObject data) throws JSONException {
        User user = new User(data.getString("user_id"));
        user.setName(data.isNull("name") == true ? "" : data.getString("name"));
        return user;
    }
}

