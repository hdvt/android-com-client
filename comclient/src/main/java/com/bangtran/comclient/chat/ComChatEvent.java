package com.bangtran.comclient.chat;

import com.bangtran.comclient.chat.Conversation;
import com.bangtran.comclient.chat.Message;

public class ComChatEvent {
    private EventType eventType;
    private ObjectType objectType;
    private Object object;

    public ComChatEvent(EventType eventType, Object obj) {
        this.eventType = eventType;
        this.object = obj;
        this.objectType = ObjectType.fromObject(obj);
    }

    // getters
    public EventType getEventType() {
        return eventType;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public Object getObject() {
        return object;
    }

    // setters
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setObjectType(ObjectType objType) {
        this.objectType = objType;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public static enum EventType {
        INSERT(0),
        UPDATE(1),
        DELETE(2);

        public final short value;

        EventType(int var3) {
            this.value = (short)var3;
        }

        public short getValue() {
            return this.value;
        }
    }

    public enum ObjectType {
        CONVERSATION(0),
        MESSAGE(1);

        public final short value;

        ObjectType(int var3) {
            this.value = (short)var3;
        }

        public static ObjectType fromObject(Object obj) {
            if (obj instanceof Conversation) {
                return CONVERSATION;
            } else if (obj instanceof Message) {
                return MESSAGE;
            }else {
                throw new IllegalArgumentException("Invalid object type: ");
            }
        }

        public short getValue() {
            return this.value;
        }
    }
}
