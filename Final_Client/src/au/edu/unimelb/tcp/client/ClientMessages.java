package au.edu.unimelb.tcp.client;

import org.json.simple.JSONObject;

public class ClientMessages {

    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoomRequest(String roomid) {
        JSONObject join = new JSONObject();
        join.put("type", "join");
        join.put("roomid", roomid);
        return join;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getListRequest() {
        JSONObject list = new JSONObject();
        list.put("type", "list");
        return list;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getWhoRequest() {
        JSONObject who = new JSONObject();
        who.put("type", "who");
        return who;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getCreateRoomRequest(String roomid) {
        JSONObject create_room = new JSONObject();
        create_room.put("type", "createroom");
        create_room.put("roomid", roomid);
        return create_room;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getDeleteRoomRequest(String roomid) {
        JSONObject delete = new JSONObject();
        delete.put("type", "deleteroom");
        delete.put("roomid", roomid);
        return delete;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getMessage(String content) {
        JSONObject message = new JSONObject();
        message.put("type", "message");
        message.put("content", content);
        return message;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getQuitRequest() {
        JSONObject quit = new JSONObject();
        quit.put("type", "quit");
        return quit;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getNewIdentityRequest(String token) {
        JSONObject newIdentity = new JSONObject();
        newIdentity.put("type", "newidentity");
        newIdentity.put("token", token);
        return newIdentity;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getMoveJoinRequest(String identity, String former,
            String roomid, String token, String name) {
        JSONObject movejoin = new JSONObject();
        movejoin.put("type", "movejoin");
        movejoin.put("identity", identity);
        movejoin.put("former", former);
        movejoin.put("roomid", roomid);
        movejoin.put("token", token);
        movejoin.put("name", name);
        return movejoin;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getServerPingRequest() {
        JSONObject serverPing = new JSONObject();
        serverPing.put("type", "serverping");
        return serverPing;
    }

}
