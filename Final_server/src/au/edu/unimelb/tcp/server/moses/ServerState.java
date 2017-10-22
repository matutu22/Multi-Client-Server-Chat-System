package au.edu.unimelb.tcp.server.moses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Singleton object that manages the server state
public class ServerState {

    private static ServerState instance;
    private static String serverIdentity;
    private static String configfilename;

    private ArrayList<ServerInfo> remoteConfig;
    private Map<String, UserInfo> users;

    private Set<String> lockedUsers;
    private Map<String, LocalChatroomInfo> localChatrooms;
    private Map<String, RemoteChatroomInfo> remoteChatrooms;
    private Set<String> lockedChatrooms;

    public static final String MAIN_HALL_OWNER_IDENTITY = "";
    public static final String MAIN_HALL_NAME_PREFIX = "MainHall-";

    private ServerState() {
        remoteConfig = new ArrayList<ServerInfo>();
        users = new HashMap<String, UserInfo>();
        lockedUsers = new HashSet<String>();
        localChatrooms = new HashMap<String, LocalChatroomInfo>();
        remoteChatrooms = new HashMap<String, RemoteChatroomInfo>();
        lockedChatrooms = new HashSet<String>();
    }

    public static synchronized ServerState getInstance() {
        if (instance == null) {
            instance = new ServerState();
        }
        return instance;
    }

    public void setInitialServerState(String serverIdentity,
    		String configfilename,
                                      ArrayList<ServerInfo> remoteConfig) {
    	ServerState.configfilename = configfilename;
        ServerState.serverIdentity = serverIdentity;
        this.remoteConfig = remoteConfig;
        UserInfo mainhallOwner = new UserInfo(MAIN_HALL_OWNER_IDENTITY, null,
                null, null, null, null);
        // Create MainHall chatroom for local server
        new LocalChatroomInfo(MAIN_HALL_NAME_PREFIX + serverIdentity,
                mainhallOwner);
        // Create MainHall chatroom for each remote server
        for (ServerInfo remoteServer : remoteConfig) {
            RemoteChatroomInfo mainhall = new RemoteChatroomInfo(
                    MAIN_HALL_NAME_PREFIX + remoteServer.getServerId(),
                    remoteServer);
            createRemoteChatroom(mainhall);
        }
    }

    public static String getServerIdentity() {
        return serverIdentity;
    }

  
    
    public static String getconfigfilename() {
    	return configfilename ;
    }
    public ArrayList<ServerInfo> getRemoteConfig() {
        return remoteConfig;
    }
    public synchronized void addremoteconfig(ServerInfo server){
    	remoteConfig.add(server);
    	  System.out.println("Added server " + server.getServerId() );
    }

    public ServerInfo getRemoteConfig(String remoteServerId) {
        for (ServerInfo server : remoteConfig)
            if (server.getServerId().equals(remoteServerId))
                return server;
        return null;
    }

    public synchronized void connectUser(UserInfo user) {
        System.out.println("Added user " + user.getIdentity() + " : "
                + users.put(user.getIdentity(), user));
    }

    public synchronized void disconnectUser(UserInfo user) {
        System.out.println("Deleted user " + user.getIdentity() + " : "
                + users.remove(user.getIdentity()));
    }

    public synchronized Map<String, UserInfo> getConnectedUsers() {
        return users;
    }

    public synchronized UserInfo getConnectedUser(String identity) {
        return users.get(identity);
    }

    /*
     * public synchronized Map<String, UserInfo> getConnectedUsers(
     * String chatroom) {
     * Map<String, UserInfo> filteredUsers = new HashMap<String, UserInfo>();
     * for (Map.Entry<String, UserInfo> entry : users.entrySet()) {
     * if (chatroom.equals(entry.getValue().getCurrentChatroom()))
     * ;
     * filteredUsers.put(entry.getKey(), entry.getValue());
     * }
     * return filteredUsers;
     * }
     */
    public synchronized void createLocalChatroom(LocalChatroomInfo room) {
        localChatrooms.put(room.getChatroomid(), room);
    }

    public synchronized void deleteLocalChatroom(LocalChatroomInfo room) {
        localChatrooms.remove(room.getChatroomid());
    }

    public synchronized Map<String, LocalChatroomInfo> getLocalChatrooms() {
        return localChatrooms;
    }

    public synchronized LocalChatroomInfo getLocalChatroomById(
            String identity) {
        return localChatrooms.get(identity);
    }

    public synchronized LocalChatroomInfo getLocalChatroomByOwner(
            String identity) {
        for (LocalChatroomInfo room : localChatrooms.values())
            if (room.getOwner().getIdentity().equals(identity))
                return room;
        return null;
    }

    public synchronized void createRemoteChatroom(RemoteChatroomInfo room) {
        remoteChatrooms.put(room.getChatroomid(), room);
    }

    public synchronized void deleteRemoteChatroom(RemoteChatroomInfo room) {
        remoteChatrooms.remove(room.getChatroomid());
    }

    public synchronized void removeRemoteChatroomByServerID(String serverid) {

        Map<String, RemoteChatroomInfo> remoteChatroomInfo = new HashMap<>(remoteChatrooms);
        for (RemoteChatroomInfo room : remoteChatroomInfo.values()) {
//            System.out.println("Checking Remote chat room"+room.getChatroomid());
            if (room.managingServer.getServerId().equals(serverid)) {
                remoteChatrooms.remove(room.getChatroomid());
//                System.out.println("Remote chat room removed");
            }
        }

    }

    public synchronized void addRemoteChatroomByServer(ServerInfo server, List<String> remoteChatroom) {
        for (String room : remoteChatroom) {
            if (!remoteChatrooms.containsKey(room)) {
                RemoteChatroomInfo remoteChatroomInfo = new RemoteChatroomInfo(room, server);
                remoteChatrooms.put(room, remoteChatroomInfo);
            }
        }
    }

    public synchronized Map<String, RemoteChatroomInfo> getRemoteChatrooms() {
        return remoteChatrooms;
    }

    public synchronized RemoteChatroomInfo getRemoteChatroom(String identity) {
        return remoteChatrooms.get(identity);
    }

    public synchronized boolean lockUser(String user, String serverId) {
        if (!isUserLocked(user)) {
            lockedUsers.add(user + "::" + serverId);
            return true;
        }
        return false;
    }

    public synchronized boolean unlockUser(String user, String serverId) {
        return lockedUsers.remove(user + "::" + serverId);
    }

    public synchronized boolean isUserLocked(String user) {
        for (String tmp : lockedUsers)
            if (tmp.startsWith(user + "::"))
                return true;
        return false;
    }

    public synchronized boolean isChatroomLocked(String chatroom) {
        for (String tmp : lockedChatrooms)
            if (tmp.startsWith(chatroom + "::"))
                return true;
        return false;
    }

    public synchronized boolean lockChatroom(String chatroom,
                                             String serverId) {
        if (!isChatroomLocked(chatroom)) {
            lockedChatrooms.add(chatroom + "::" + serverId);
            return true;
        }
        return false;
    }

    public synchronized boolean unlockChatroom(String chatroom,
                                               String serverId) {
        return lockedChatrooms.remove(chatroom + "::" + serverId);
    }

    public String toString() {
        String ServerState = "serverid: " + serverIdentity + "remoteservers: "
                + remoteConfig + "users: " + users + "lockedusers: "
                + lockedUsers + "localrooms: " + localChatrooms
                + "remoterooms: " + remoteChatrooms + "lockedrooms: "
                + lockedChatrooms;

        return ServerState;
    }

}
