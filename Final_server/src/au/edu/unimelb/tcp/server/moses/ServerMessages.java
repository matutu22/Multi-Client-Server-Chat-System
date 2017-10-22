package au.edu.unimelb.tcp.server.moses;

import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ServerMessages {

    @SuppressWarnings("unchecked")
    public static JSONObject getLockRoomRequest(String serverid,
            String roomid) {
        JSONObject lockRoomReq = new JSONObject();
        lockRoomReq.put("type", "lockroomid");
        lockRoomReq.put("serverid", serverid);
        lockRoomReq.put("roomid", roomid);
        return lockRoomReq;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getLockRoomResponse(String serverid,
            String roomid, boolean approval) {
        JSONObject lockRoomRes = new JSONObject();
        lockRoomRes.put("type", "lockroomid");
        lockRoomRes.put("serverid", serverid);
        lockRoomRes.put("roomid", roomid);
        lockRoomRes.put("locked", Boolean.toString(approval));
        return lockRoomRes;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getCreateRoomResponse(String roomid,
            boolean approval) {
        JSONObject createRoomRes = new JSONObject();
        createRoomRes.put("type", "createroom");
        createRoomRes.put("roomid", roomid);
        createRoomRes.put("approved", Boolean.toString(approval));
        return createRoomRes;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getReleaseRoomNotification(String serverid,
            String roomid, boolean approval) {
        JSONObject releaseRoomNotify = new JSONObject();
        releaseRoomNotify.put("type", "releaseroomid");
        releaseRoomNotify.put("serverid", serverid);
        releaseRoomNotify.put("roomid", roomid);
        releaseRoomNotify.put("approved", Boolean.toString(approval));
        return releaseRoomNotify;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getRoomChangeNotification(String identity,
            String formerRoom, String newRoom, String name) {
        JSONObject roomChangeNotification = new JSONObject();
        roomChangeNotification.put("type", "roomchange");
        roomChangeNotification.put("identity", identity);
        roomChangeNotification.put("former", formerRoom);
        roomChangeNotification.put("roomid", newRoom);
        roomChangeNotification.put("name", name);
        return roomChangeNotification;
    }

/*    @SuppressWarnings("unchecked")
    public static JSONObject getJoinRoomResponse(String identity,
            String former, String roomidentity) {
        JSONObject joinRoom = new JSONObject();
        joinRoom.put("type", "roomchange");
        joinRoom.put("identity", identity);
        joinRoom.put("former", former);
        joinRoom.put("roomid", roomidentity);
        return joinRoom;
    }
    */

    @SuppressWarnings("unchecked")
    public static JSONObject getLockIdentityRequest(String serverid,
            String userid) {
        JSONObject lockIdentityReq = new JSONObject();
        lockIdentityReq.put("type", "lockidentity");
        lockIdentityReq.put("serverid", serverid);
        lockIdentityReq.put("identity", userid);
        return lockIdentityReq;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getLockIdentityResponse(String serverid,
            String userid, boolean approval) {
        JSONObject lockIdentityRes = new JSONObject();
        lockIdentityRes.put("type", "lockidentity");
        lockIdentityRes.put("serverid", serverid);
        lockIdentityRes.put("identity", userid);
        lockIdentityRes.put("locked", Boolean.toString(approval));
        return lockIdentityRes;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getNewIdentityResponse(boolean approval,String identity, String name) {
        JSONObject newIdentityRes = new JSONObject();
        newIdentityRes.put("type", "newidentity");
        newIdentityRes.put("approved", Boolean.toString(approval));
        newIdentityRes.put("identity", identity);
        newIdentityRes.put("name", name);
        return newIdentityRes;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getReleaseIdentityNotification(String serverid,
            String userid) {
        JSONObject releaseIdentityNotify = new JSONObject();
        releaseIdentityNotify.put("type", "releaseidentity");
        releaseIdentityNotify.put("serverid", serverid);
        releaseIdentityNotify.put("identity", userid);
        return releaseIdentityNotify;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getListChatroomsResponse(Set<String> localrooms,
            Set<String> remoterooms) {
        JSONObject listRoomsResponse = new JSONObject();
        listRoomsResponse.put("type", "roomlist");
        JSONArray chatroomList = new JSONArray();
        for (String room : localrooms) {
            chatroomList.add(room);
        }
        for (String room : remoterooms) {
            chatroomList.add(room);
        }
        listRoomsResponse.put("rooms", chatroomList);
        return listRoomsResponse;

    }

    @SuppressWarnings("unchecked")
    public static JSONObject getRoomContents(LocalChatroomInfo room) {
        JSONObject roomContents = new JSONObject();
        roomContents.put("type", "roomcontents");
        roomContents.put("roomid", room.getChatroomid());
        JSONArray userList = new JSONArray();
        JSONArray nameList = new JSONArray();
        for (UserInfo user : room.getMembers()) {
            userList.add(user.getIdentity());
            nameList.add(user.getName());
        }
        roomContents.put("identities", userList);
        roomContents.put("names", nameList);
        roomContents.put("owner", room.getOwner().getIdentity());
        roomContents.put("ownerName", room.getOwner().getName());
        return roomContents;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getRouteResponse(String roomidentity,
            String serverAddress, int clientsPort) {
        JSONObject route = new JSONObject();
        route.put("type", "route");
        route.put("roomid", roomidentity);
        route.put("host", serverAddress);
        route.put("port", String.valueOf(clientsPort));
        return route;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getServerChangeResponse(boolean approval,
            String serverid) {
        JSONObject serverChangeRes = new JSONObject();
        serverChangeRes.put("type", "serverchange");
        serverChangeRes.put("approved", Boolean.toString(approval));
        serverChangeRes.put("serverid", serverid);
        return serverChangeRes;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getDeleteRoomResponse(String roomid,
            boolean approval) {
        JSONObject deleteRoomRes = new JSONObject();
        deleteRoomRes.put("type", "deleteroom");
        deleteRoomRes.put("roomid", roomid);
        deleteRoomRes.put("approved", Boolean.toString(approval));
        return deleteRoomRes;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getDeleteRoomServerNotice(String roomid,
            String serverid) {
        JSONObject deleteRoomServerNotice = new JSONObject();
        deleteRoomServerNotice.put("type", "deleteroom");
        deleteRoomServerNotice.put("serverid", serverid);
        deleteRoomServerNotice.put("roomid", roomid);
        return deleteRoomServerNotice;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject newserver(String serverid, String serverip, String clientport, String serverport) {
        JSONObject newserver = new JSONObject();
        newserver.put("type", "newserver");
        newserver.put("serverid", serverid);
        newserver.put("serverip", serverip);
        newserver.put("clientport", clientport);
        newserver.put("serverport", serverport);

        return newserver;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject sendserverinfo(String serveridentity, Map<String,LocalChatroomInfo> localchatroominfo){
    	JSONObject sendserverinfo = new JSONObject();
    	sendserverinfo.put("type", "sendserverinfo");
    	sendserverinfo.put("serverid",serveridentity);
 
    	JSONArray chatroomlist = new JSONArray();
    	Set<String> rooms = localchatroominfo.keySet();
    	for(String room : rooms){
    		chatroomlist.add(room);
        }
    	sendserverinfo.put("chatrooms", chatroomlist);
    			
    	
    	
    	return sendserverinfo;
    }
    @SuppressWarnings("unchecked")
    public static JSONObject getHeartbeatRequest (String serverid) {
        JSONObject heartbeatRequest = new JSONObject();
        heartbeatRequest.put("type", "heartbeat");
        heartbeatRequest.put("serverid", serverid);
        return heartbeatRequest;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getHeartbeatReponse (String serverid, boolean approved, Set<String> localrooms) {
        JSONObject heartbeatReponse = new JSONObject();
        JSONArray chatroomList = new JSONArray();
        for (String room : localrooms) {
            chatroomList.add(room);
        }
        heartbeatReponse.put("type", "heartbeat");
        heartbeatReponse.put("serverid", serverid);
        heartbeatReponse.put("approved", Boolean.toString(approved));
        heartbeatReponse.put("rooms", chatroomList);
        return heartbeatReponse;
    }

    @SuppressWarnings("unchecked")
    public static JSONObject getPingResponse() {
        JSONObject pingResponse = new JSONObject();
        pingResponse.put("type", "serverping");
        pingResponse.put("alive", "true");
        return pingResponse;
    }
}
