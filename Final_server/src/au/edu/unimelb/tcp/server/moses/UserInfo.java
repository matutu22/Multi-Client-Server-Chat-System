package au.edu.unimelb.tcp.server.moses;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;

import org.json.simple.JSONObject;

public class UserInfo {

    private String identity;
    private String currentChatroom;
    private ClientConnection managingThread;
    private SSLSocket clientSocket;
    private String name;

    public static final String MAIN_HALL_OWNER_IDENTITY = "";
    public static final String MAIN_HALL_NAME_PREFIX = "MainHall-";
    public static final String CHAT_ROOM_QUITTING_USER = "";
    public static final String MAIN_HALL_NAME = MAIN_HALL_NAME_PREFIX
            + ServerState.getServerIdentity();

    public UserInfo(String identity, ClientConnection managingThread,
            SSLSocket clientSocket, String landingChatroom, String oldChatroom, String name) {

		
        ServerState state = ServerState.getInstance();

        this.identity = identity;
        if (!MAIN_HALL_NAME.equals(landingChatroom)
                && state.getLocalChatroomById(landingChatroom) != null)
            this.currentChatroom = landingChatroom;
        else
            this.currentChatroom = MAIN_HALL_NAME;
        this.managingThread = managingThread;
        this.clientSocket = clientSocket;
        this.name = name;
        //
        if (!MAIN_HALL_OWNER_IDENTITY.equals(identity)) {
            state.connectUser(this);
            state.getLocalChatroomById(currentChatroom).joinMember(this,
                    oldChatroom);
        }
    }

    public String getIdentity() {
        return identity;
    }

    public String getCurrentChatroom() {
        return currentChatroom;
    }

    // Called only when User Changes room.
    public void setCurrentChatroom(String newCurrentChatroom,
            boolean triggerRoomChange) {
        System.out.println("Starting setCurrentChatroom");
        if (triggerRoomChange) {
            ServerState state = ServerState.getInstance();
            state.getLocalChatroomById(this.currentChatroom).unjoinMember(this,
                    newCurrentChatroom);
            state.getLocalChatroomById(newCurrentChatroom).joinMember(this,
                    this.currentChatroom);
        }
        this.currentChatroom = newCurrentChatroom;
        System.out.println("Finishing setCurrentChatroom");
    }

    public ClientConnection getManagingThread() {
        return managingThread;
    }

    public SSLSocket getClientSocket() {
        return clientSocket;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String toString() {
        return "userid: " + identity + "currentroom: " + currentChatroom;

    }

    public void leaveServer(String newRoomId)
            throws UnknownHostException, IOException {
        ServerState state = ServerState.getInstance();
        if (state.getLocalChatroomByOwner(identity) == null
                && state.getRemoteChatroom(newRoomId) != null) {
            state.getLocalChatroomById(this.currentChatroom).unjoinMember(this,
                    newRoomId);
            this.currentChatroom = CHAT_ROOM_QUITTING_USER;
        } else {
            state.getLocalChatroomByOwner(identity).destroy(this.identity,
                    true, true);
        }
        state.disconnectUser(this);
    }

    public void destroy(boolean notifyUser)
            throws UnknownHostException, IOException {
        ServerState state = ServerState.getInstance();
        if (state.getLocalChatroomByOwner(identity) == null) {
            state.getLocalChatroomById(this.currentChatroom).unjoinMember(this,
                    CHAT_ROOM_QUITTING_USER);
            if (notifyUser) {
                JSONObject userMessage = ServerMessages
                        .getRoomChangeNotification(this.identity,
                                this.currentChatroom, "", this.name);
                this.getManagingThread().write(userMessage.toJSONString());
            }
        } else {
            state.getLocalChatroomByOwner(identity).destroy(this.identity,
                    true, notifyUser);
        }
        state.disconnectUser(this);
    }

}
