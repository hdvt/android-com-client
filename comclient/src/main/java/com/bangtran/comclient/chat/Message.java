package com.bangtran.comclient.chat;

import android.util.Log;

import com.bangtran.comclient.ComCallback;
import com.bangtran.comclient.ComClient;
import com.bangtran.comclient.ComError;
import com.bangtran.comclient.utils.DateUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.ParseException;

public class Message implements Serializable {
    private static final String TAG = Message.class.getSimpleName();

    public static final int TYPE_TEXT = 1;
    public static final int TYPE_PHOTO = 2;
    public static final int TYPE_FILE = 3;
    public static final int TYPE_LOCATION = 4;
    public static final int TYPE_CREATE_CONVERSATION = 5;
    public static final int TYPE_RENAME_CONVERSATION = 6;

    private String id;
    private int sequence;
    private String conversationId;
    private String senderId;
    private int type;
    private Message.State state;
    private String text;
    private String fileName;
    private String fileUrl;
    private String filePath;
    private String thumbnailUrl;
    private float imageRatio;
    private double latitude;
    private double longitude;
    private long createAt;
    private long updateAt;

    private JSONObject customData;

    public Message() {
    }

    public Message(String text) {
        this(TYPE_TEXT);
        this.text = text;
    }

    public Message(int type) {
        this.type = type;
        this.state = State.INITIALIZE;
        this.sequence = 0;
        this.createAt = DateUtil.getMillisecondsTime();
        this.updateAt = this.createAt;
    }

    // getters
    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public int getType() {
        return type;
    }

    public State getState() {
        return state;
    }

    public String getText() {
        return text;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public float getImageRatio() {
        return imageRatio;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getCreatedAt() {
        return createAt;
    }

    public long getUpdatedAt() {
        return updateAt;
    }

    public int getSequence() {
        return sequence;
    }

    public JSONObject getCustomData() {
        return customData;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    // setters
    public void setId(String id) {
        this.id = id;
    }

    public void setConversationId(String convId) {
        this.conversationId = convId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCreateAt(long createdAt) {
        this.createAt = createdAt;
    }

    public void setUpdateAt(long updatedAt) {
        this.updateAt = updatedAt;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setImageRatio(float imageRatio) {
        this.imageRatio = imageRatio;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void setCustomData(JSONObject customData) {
        this.customData = customData;
    }

    public void markAsRead(final ComClient client, ComCallback<JSONObject> callback) {
        updateState(client, State.READ, callback);
    }

    public void updateState(final ComClient client, Message.State state, ComCallback<JSONObject> callback) {
        ComClient.executor.execute(() -> {
            JSONObject packet = new JSONObject();
            try {
                packet.put("event", "chat_message_state");
                JSONObject body = new JSONObject();
                body.put("conv_id", getConversationId());
                body.put("msg_id", getId());
                body.put("state", state.getValue());
                packet.put("body", body);
                client.sendMessage(packet, new ComCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        Log.d(TAG, "updateState " + data.toString());
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onError(ComError error) {
                        Log.e(TAG, "updateState error " + error.toString());
                    }
                });


            } catch (JSONException e) {
                Log.e(TAG, "updateState error: " + e.getMessage());
            }
        });
    }

    public static Message parseFrom(JSONObject data) throws JSONException, ParseException {
        Message msg = new Message();
        msg.setConversationId(data.getString("conv_id"));
        msg.setId(data.getString("msg_id"));
        msg.setSenderId(data.getString("sender_id"));
        msg.setType(data.getInt("msg_type"));
        msg.setState(State.values()[data.getInt("msg_state")]);
        msg.setCreateAt(DateUtil.convertStringToMilliseconds(data.getString("created_at"), DateUtil.DATE_FORMAT_1));
        msg.setUpdateAt(DateUtil.convertStringToMilliseconds(data.getString("updated_at"), DateUtil.DATE_FORMAT_1));
        msg.setText(data.getString("text"));
        msg.setSequence(data.isNull("sequence") ? 0 : data.getInt("sequence"));
        if (!data.isNull("image")){
            JSONObject image = data.getJSONObject("image");
            msg.setFileUrl(image.getString("image_path"));
            msg.setThumbnailUrl(image.getString("thumbnail_path"));
            msg.setImageRatio((float)image.getDouble("ratio"));
        }
        if (!data.isNull("location")){
            JSONObject location = data.getJSONObject("location");
            msg.setLatitude(location.getDouble("latitude"));
            msg.setLongitude(location.getDouble("longitude"));
        }
        return msg;
    }

    public enum State {
        INITIALIZE(0),
        SENDING(1),
        SENT(2),
        DELIVERED(3),
        READ(4);

        public final short value;

        State(int var3) {
            this.value = (short) var3;
        }

        public short getValue() {
            return this.value;
        }
    }
}
