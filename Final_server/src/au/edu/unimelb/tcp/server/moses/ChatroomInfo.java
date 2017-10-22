package au.edu.unimelb.tcp.server.moses;

public class ChatroomInfo {
    String chatroomId;

    public ChatroomInfo(String chatroomId) {
        this.chatroomId = chatroomId;
    }

    public String getChatroomid() {
        return chatroomId;
    }

    public void setChatroomid(String chatroomId) {
        this.chatroomId = chatroomId;
    }
}
