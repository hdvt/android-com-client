package com.bangtran.comclient.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bangtran.comclient.ComCallback;
import com.bangtran.comclient.ComClient;
import com.bangtran.comclient.ComError;
import com.bangtran.comclient.utils.DateUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.List;
import java.util.Vector;

public class Conversation implements Serializable {
    private static final String TAG = Conversation.class.getSimpleName();

    private String id;
    private String name;
    private String creator;
    private List<User> participants;
    private int totalUnread;
    private long createdAt;
    private long updatedAt;

    private Message lastMsg;

    public Conversation() {
        participants = new Vector<>();
    }

    // getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCreator() {
        return creator;
    }

    public List<User> getParticipants() {
        return participants;
    }

    public String getText() {
        return lastMsg.getText();
    }

    public String getLastMsg() {
        return lastMsg.getText();
    }

    public int getLastMsgType() {
        return lastMsg.getType();
    }

    public Message.State getLastMsgState() {
        return lastMsg.getState();
    }

    public long getLastTimeNewMsg() {
        return lastMsg.getUpdatedAt();
    }

    public String getLastMsgId() {
        return lastMsg.getId();
    }

    public String getLastMsgSender() {
        return lastMsg.getSenderId();
    }

    public Message getLastMessage(Context appCtx) {
        return lastMsg;
    } // todo

    public void getLastMessages(ComClient client, int count, ComCallback<List<Message>> callback) {
        loadMessages(client, 0, Long.MAX_VALUE, count, callback);
    }

    public void getLocalMessages(ComClient client, int count, ComCallback<List<Message>> callback) {

    }

    public void getMessagesBefore(ComClient client, int seq, int count, ComCallback<List<Message>> callback) {
        loadMessages(client, 0, seq, count, callback);
    }

    private void loadMessages(ComClient client, long from, long to, int limit, ComCallback<List<Message>> callback) {
        ComClient.executor.execute(() -> {
            JSONObject packet = new JSONObject();
            try {
                packet.put("event", "chat_load_messages");
                JSONObject body = new JSONObject();
                body.put("conv_id", getId());
                body.put("greater_seq", from);
                body.put("smaller_seq", to);
                body.put("sort", "DESC");
                body.put("limit", limit);
                packet.put("body", body);
                client.sendMessage(packet, new ComCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        Log.d(TAG, "getLastMessages " + data.toString());
                        try {
                            List<Message> messages = new Vector<>();
                            JSONArray msgJson = data.getJSONArray("messages");
                            for (int i = msgJson.length() - 1; i >= 0; i--) {
                                messages.add(Message.parseFrom(msgJson.getJSONObject(i)));
                            }
                            for (int i = 0; i < messages.size(); i++) {
                                messages.get(i).updateState(client, Message.State.DELIVERED, new ComCallback<JSONObject>() {
                                    @Override
                                    public void onSuccess(@Nullable @org.jetbrains.annotations.Nullable JSONObject data) {
                                        Log.d(TAG, "update message state success");
                                    }

                                    @Override
                                    public void onError(ComError error) {
                                        Log.e(TAG, "update message state error " + error.getMessage());

                                    }
                                });
                            }
                            callback.onSuccess(messages);
                        } catch (JSONException | ParseException e) {
                            Log.e(TAG, e.toString());
                        }

                    }

                    @Override
                    public void onError(ComError error) {
                        Log.e(TAG, "getLastMessages error " + error.toString());
                    }
                });


            } catch (JSONException e) {
                Log.e(TAG, "getLastMessages error: " + e.getMessage());
            }
        });
    }

    public int getTotalUnread() {
        return totalUnread;
    }

    public long getCreateAt() {
        return createdAt;
    }

    public long getUpdateAt() {
        return updatedAt;
    }


    // setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setParticipants(List<User> participants) {
        this.participants = participants;
    }

    public void setTotalUnread(int totalUnread) {
        this.totalUnread = totalUnread;
    }

    public void setCreateAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdateAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setLastMessage(Message lastMessage) {
        this.lastMsg = lastMessage;
    }

    public void addParticipants(ComClient client, List<User> participants, ComCallback<List<User>> callback) {
        // TODO: 13/08/2021
    }

    public void sendMessage(ComClient client, Message message, ComCallback<JSONObject> callback) {
        ComClient.executor.execute(() -> {
            message.setConversationId(getId());
            message.setSenderId(client.getUserId());
            message.setState(Message.State.SENDING);
            client.getConnectionListener().onComChatEvent(client, new ComChatEvent(ComChatEvent.EventType.INSERT, message));
            JSONObject packet = new JSONObject();
            try {
                packet.put("event", "chat_message");
                JSONObject body = new JSONObject();
                body.put("conv_id", getId());
                body.put("msg_type", message.getType());
                switch (message.getType()) {
                    case Message.TYPE_TEXT: {
                        body.put("text", message.getText());
                        break;
                    }
                    case Message.TYPE_PHOTO: {
                        Bitmap bitmap = BitmapFactory.decodeFile(message.getFilePath());
                        float ratio = (float) bitmap.getHeight() / bitmap.getWidth();
                        if (bitmap.getHeight() > 1280 || bitmap.getWidth() > 1280) {
                            int resizeHeight = ratio >= 1 ? 1280 : Math.round(1280 * ratio);
                            int resizeWidth = ratio <= 1 ? 1280 : Math.round(1280 / ratio);
                            bitmap = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, true);
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        byte[] imageBytes = baos.toByteArray();
                        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                        body.put("image", encodedImage);
                        break;
                    }
                    case Message.TYPE_FILE: {
                        InputStream inputStream = null;
                        String encodedFile = "", lastVal;
                        try {
                            File file = new File(message.getFilePath());
                            message.setFileName(file.getName());
                            inputStream = new FileInputStream(file.getAbsolutePath());
                            byte[] buffer = new byte[10240];//specify the size to allow
                            int bytesRead;
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);

                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                output64.write(buffer, 0, bytesRead);
                            }
                            output64.close();
                            encodedFile = output.toString();
                            JSONObject fileObj = new JSONObject();
                            fileObj.put("name", message.getFileName());
                            fileObj.put("rawdata", encodedFile);
                            body.put("file", fileObj);
                        } catch (FileNotFoundException e1) {
                            e1.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                    case Message.TYPE_LOCATION: {
                        JSONObject location = new JSONObject();
                        location.put("longitude", message.getLongitude());
                        location.put("latitude", message.getLatitude());
                        body.put("location", location);
                        break;
                    }
                }
                ;
                packet.put("body", body);
                client.sendMessage(packet, new ComCallback<JSONObject>() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        Log.d(TAG, "sendMessage " + data.toString());
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onError(ComError error) {
                        Log.e(TAG, "sendMessage error " + error.toString());
                    }
                });


            } catch (JSONException e) {
                Log.e(TAG, "sendMessage error: " + e.getMessage());
            }
        });
    }

    public void delete(ComClient client, ComCallback<JSONObject> callback) {
        // TODO: 13/08/2021
    }

    public static Conversation parseFrom(JSONObject data) throws JSONException, ParseException {
        Conversation conv = new Conversation();
        conv.setId(data.getString("conv_id"));
        conv.setName(data.getString("name"));
        conv.setCreator(data.getString("creator"));
        conv.setTotalUnread(data.getInt("total_unread"));
        conv.setCreateAt(DateUtil.convertStringToMilliseconds(data.getString("created_at"), DateUtil.DATE_FORMAT_1));
        conv.setUpdateAt(DateUtil.convertStringToMilliseconds(data.getString("updated_at"), DateUtil.DATE_FORMAT_1));
        JSONArray participants = data.getJSONArray("participants");
        for (int i = 0; i < participants.length(); i++) {
            conv.participants.add(User.parseFrom(participants.getJSONObject(i)));
        }
        conv.lastMsg = Message.parseFrom(data.getJSONObject("last_msg"));
        return conv;
    }
}
