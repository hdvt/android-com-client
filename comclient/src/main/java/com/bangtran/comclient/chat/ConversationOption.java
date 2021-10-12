package com.bangtran.comclient.chat;

public class ConversationOption {
    public String name = "";
    public boolean isGroup = false;
    public boolean isDistinct = false;

    public ConversationOption() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String var1) {
        this.name = var1;
    }

    public boolean isGroup() {
        return this.isGroup;
    }

    public void setGroup(boolean var1) {
        this.isGroup = var1;
    }

    public boolean isDistinct() {
        return this.isDistinct;
    }

    public void setDistinct(boolean var1) {
        this.isDistinct = var1;
    }
}

