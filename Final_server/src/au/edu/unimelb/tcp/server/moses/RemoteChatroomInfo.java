package au.edu.unimelb.tcp.server.moses;

public class RemoteChatroomInfo extends ChatroomInfo {
    ServerInfo managingServer;

    public RemoteChatroomInfo(String chatroomid, ServerInfo managingServer) {
        super(chatroomid);
        this.managingServer = managingServer;
        ServerState state = ServerState.getInstance();
        state.createRemoteChatroom(this);
    }

    public String toString() {
        return "roomid: " + getChatroomid() + " Server: "
                + managingServer.getServerId();
    }

    public boolean destroy(String requestor) {
        if ( managingServer.getServerId().equals(requestor)) {
            ServerState state = ServerState.getInstance();
            state.deleteRemoteChatroom(this);
            return true;
        }
        return false;

    }

}
