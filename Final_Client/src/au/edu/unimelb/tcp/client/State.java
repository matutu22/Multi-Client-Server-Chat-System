package au.edu.unimelb.tcp.client;

import java.util.Calendar;

public class State {

	private String identity;
    private String roomId;
    private String accesstoken;
    private Calendar tokenExpiry;
    private String userName;
	
	public State(String identity, String roomId, String accessToken, Calendar tokenExpiry) {
		this.identity = identity;
		this.roomId = roomId;
		this.setAccesstoken(accessToken);
		this.tokenExpiry=tokenExpiry;
		
	}
	
    public Calendar getTokenExpiry() {
        return tokenExpiry;
    }

    public void setTokenExpiry(Calendar tokenExpiry) {
        this.tokenExpiry = tokenExpiry;
    }

    public synchronized void setIdentity(String identity) {
        this.identity = identity;
    }

	public synchronized String getRoomId() {
		return roomId;
	}
	public synchronized void setRoomId(String roomId) {
		this.roomId = roomId;
	}
	
	public synchronized String getIdentity() {
		return identity;
	}

    public String getAccesstoken() {
        return accesstoken;
    }

    public void setAccesstoken(String accesstoken) {
        this.accesstoken = accesstoken;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
	
	
}
