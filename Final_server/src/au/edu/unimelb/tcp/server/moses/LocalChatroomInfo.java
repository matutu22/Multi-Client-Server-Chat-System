package au.edu.unimelb.tcp.server.moses;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.JSONObject;

public class LocalChatroomInfo extends ChatroomInfo {
    UserInfo owner;
    ArrayList<UserInfo> members;

    public static final String MAIN_HALL_OWNER_IDENTITY = "";
    public static final String MAIN_HALL_NAME_PREFIX = "MainHall-";

    public LocalChatroomInfo(String chatroomId, UserInfo owner) {
        super(chatroomId);
        this.owner = owner;
        this.members = new ArrayList<UserInfo>();
        ServerState state = ServerState.getInstance();
        state.createLocalChatroom(this);
        // Change owners Room to the newly created room
        if (!(MAIN_HALL_NAME_PREFIX + ServerState.getServerIdentity())
                .equals(chatroomId))
            owner.setCurrentChatroom(chatroomId, true);
    }

    public UserInfo getOwner() {
        return owner;
    }

    public void setOwner(UserInfo owner) {
        this.owner = owner;
    }

    public ArrayList<UserInfo> getMembers() {
        return members;
    }

    public void setMembers(ArrayList<UserInfo> members) {
        this.members = members;
    }

    public synchronized void joinMember(UserInfo member, String oldRoomId) {
        System.out.println("Starting joinMember");
        String memberId = member.getIdentity();
        JSONObject roomChangeNotify = ServerMessages.getRoomChangeNotification(
                memberId, oldRoomId, this.chatroomId, member.getName());
        Message roomChangeNotifymsg = new Message(false,
                roomChangeNotify.toJSONString());
        broadcastMessage(roomChangeNotifymsg);
        members.add(member);
        System.out.println("Finishing joinMember");
    }

    public synchronized void unjoinMember(UserInfo member, String newRoomId) {
        System.out.println("Starting unjoinMember");
        String memberId = member.getIdentity();
        String memberName = member.getName();
        members.remove(member);
        JSONObject broadcastMessage = ServerMessages.getRoomChangeNotification(
                memberId, this.chatroomId, newRoomId, memberName);
        Message broadcastmsg = new Message(false,
                broadcastMessage.toJSONString());
        broadcastMessage(broadcastmsg);
        System.out.println("Finishing unjoinMember");
    }

    public synchronized void broadcastMessage(Message msg) {
        for (UserInfo user : members) {
            user.getManagingThread().getMessageQueue().add(msg);
        }

    }

    public synchronized void broadcastMessage(Message msg,
            ArrayList<UserInfo> group) {
        System.out.println(" Broadcast received to group of "+group.size());
        for (UserInfo user : group) {
            System.out.println("Sending broadcast message to "+user.getIdentity()+ " :: "+msg.getMessage());
            if (user == this.owner)
                user.getManagingThread().write(msg.getMessage());
            else
                user.getManagingThread().getMessageQueue().add(msg);
        }

    }

    public String toString() {
        return "roomid: " + getChatroomid() + " Owner: " + owner.getIdentity()
                + "Members:" + members;
    }

    public boolean destroy(String requestor, boolean ownerQuitting,
            boolean notifyOwner) throws UnknownHostException, IOException {
        JSONObject msgToRequestor;
        if (requestor.equals(owner.getIdentity())) {
            ServerState state = ServerState.getInstance();
            LocalChatroomInfo mainhall = state.getLocalChatroomById(
                    MAIN_HALL_NAME_PREFIX + ServerState.getServerIdentity());
            ArrayList<UserInfo> currentRoomMembers=new ArrayList<UserInfo>(this.getMembers());
            ArrayList<UserInfo> mainHallMembers=new ArrayList<UserInfo>(mainhall.getMembers());
            ArrayList<UserInfo> notificationListForMemberMove;
            notificationListForMemberMove = new ArrayList<UserInfo>(currentRoomMembers);
            notificationListForMemberMove.addAll(mainHallMembers);
            if(ownerQuitting){
                notificationListForMemberMove.remove(owner);
            }
            else{
                msgToRequestor = ServerMessages
                        .getDeleteRoomResponse(this.chatroomId, true);
                owner.getManagingThread().write(msgToRequestor.toJSONString());
            }

            JSONObject broadcastMessage;
            Message broadcastmsg;
            // Handle each member of current room except owner
            for (UserInfo member : this.members) {
                if (member != owner) {
                    mainhall.members.add(member);
                    member.setCurrentChatroom(mainhall.getChatroomid(), false);
                    broadcastMessage = ServerMessages
                            .getRoomChangeNotification(member.getIdentity(),
                                    this.chatroomId, mainhall.getChatroomid(),member.getName());
                    broadcastmsg = new Message(false,
                            broadcastMessage.toJSONString());
                    broadcastMessage(broadcastmsg, notificationListForMemberMove);
                }
            }

            // Handle owner related notification finally
            ArrayList<UserInfo> notificationListForOwnerMove;
            notificationListForOwnerMove = new ArrayList<UserInfo>(currentRoomMembers);
            if (!ownerQuitting) {
            notificationListForOwnerMove.addAll(mainHallMembers);     
            mainhall.members.add(owner);
            owner.setCurrentChatroom(mainhall.getChatroomid(), false);
            }
            if(!notifyOwner){
                notificationListForOwnerMove.remove(owner);
            }
            String ownerLandingRoom = ownerQuitting ? ""
                    : mainhall.getChatroomid();
            broadcastMessage = ServerMessages.getRoomChangeNotification(
                    requestor, this.chatroomId, ownerLandingRoom,owner.getName());
            broadcastmsg = new Message(false, broadcastMessage.toJSONString());

            broadcastMessage(broadcastmsg, notificationListForOwnerMove);

            members = null;
            owner = null;

            // Notify Other servers about deletion
            JSONObject sendToServer = ServerMessages.getDeleteRoomServerNotice(
                    this.chatroomId, ServerState.getServerIdentity());
            notifyServers(sendToServer.toJSONString());

            state.deleteLocalChatroom(this);

            return true;
        } else if (notifyOwner) {
            msgToRequestor = ServerMessages
                    .getDeleteRoomResponse(this.chatroomId, false);
            ServerState state = ServerState.getInstance();
            state.getConnectedUser(requestor).getManagingThread()
                    .write(msgToRequestor.toJSONString());

        }
        return false;
    }

    public boolean notifyServers(String Message)
            throws UnknownHostException, IOException {
        ServerState state = ServerState.getInstance();
        List<ServerInfo> remoteServers = state.getRemoteConfig();
       
        
		
    	
        
        for (ServerInfo server : remoteServers) {
            if (server.getStatus()) {
        //	System.setProperty("javax.net.ssl.trustStore", Server.class.getResource("cacerts").getPath());
		 //   System.setProperty("javax.net.debug","all");
        	SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(server.getServerAddress(),
                    server.getCoordinationPort());
            //socket.setEnabledCipherSuites(suites);
            
            DataOutputStream out = new DataOutputStream(
                    socket.getOutputStream());
            System.out.println("Sending: " + Message);
            out.write((Message + "\n").getBytes("UTF-8"));
            out.flush();
            socket.close();
        }
        }
        return true;
    }

}
