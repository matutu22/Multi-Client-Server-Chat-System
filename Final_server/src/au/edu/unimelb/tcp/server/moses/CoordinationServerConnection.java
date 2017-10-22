package au.edu.unimelb.tcp.server.moses;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CoordinationServerConnection extends Thread {

    private SSLSocket managementSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private int managementNum;

    public CoordinationServerConnection(SSLSocket managementSocket,
            int managementNum) {
        try {
            this.managementSocket = managementSocket;
            reader = new BufferedReader(new InputStreamReader(
                    managementSocket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(
                    managementSocket.getOutputStream(), "UTF-8"));
            this.managementNum = managementNum;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {
			
 /*           System.out.println(Thread.currentThread().getName()
                    + " - Reading messages from sever " + managementNum
                    + " connection");
*/
            String managementMsg = reader.readLine();
/*            System.out.println(Thread.currentThread().getName()
                    + " - Message from server " + managementNum + " received: "
                    + managementMsg);
*/
            String responseMsg = handleServerMessage(managementMsg);
            if (responseMsg != null) {
                writer.write((responseMsg + "\n"));
                writer.flush();
            }
  /*          System.out.println(Thread.currentThread().getName()
                    + " - Message sent to server " + managementNum);
*/
            managementSocket.close();

            /*System.out.println(Thread.currentThread().getName() + " - Server "
                    + managementNum + " disconnected");
*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String handleServerMessage(String managementMsg) throws IOException {
        ServerState state = ServerState.getInstance();
        JSONParser parser = new JSONParser();
        JSONObject requestFromClient;
        String roomid, identity;
        String replyToServer = null;
        String serverid = ServerState.getServerIdentity();
        boolean approval;
        try {
            requestFromClient = (JSONObject) parser.parse(managementMsg);
            String type = (String) requestFromClient.get("type");
            String sourceServer = (String) requestFromClient.get("serverid");
//            System.out.println("Type is " + type + "::");
            switch (type) {
            case "lockroomid":
                roomid = (String) requestFromClient.get("roomid");

                approval = state.getLocalChatroomById(roomid) == null
                        && state.lockChatroom(roomid, sourceServer);
                replyToServer = ServerMessages
                        .getLockRoomResponse(serverid, roomid, approval)
                        .toJSONString();
                break;
            case "releaseroomid":
                roomid = (String) requestFromClient.get("roomid");
                approval = Boolean.parseBoolean(
                        (String) requestFromClient.get("approved"));
                if (state.unlockChatroom(roomid, sourceServer) && approval)
                    new RemoteChatroomInfo(roomid,
                            state.getRemoteConfig(sourceServer));
                break;
            case "lockidentity":
                identity = (String) requestFromClient.get("identity");
                approval = state.getConnectedUser(identity) == null
                        && state.lockUser(identity, sourceServer);
                replyToServer = ServerMessages
                        .getLockIdentityResponse(serverid, identity, approval)
                        .toJSONString();
                break;
            case "releaseidentity":
                identity = (String) requestFromClient.get("identity");
                state.unlockUser(identity, sourceServer);
                break;
            case "deleteroom":
                roomid = (String) requestFromClient.get("roomid");
                state.getRemoteChatroom(roomid).destroy(sourceServer);
                break;
            case "newserver":
            	String serverId = (String) requestFromClient.get("serverid");
            	String serverip = (String) requestFromClient.get("serverip");
            	String clientport = (String) requestFromClient.get("clientport"); 
            	String serverport = (String) requestFromClient.get("serverport");
            	ServerInfo server = new ServerInfo(serverId,
            			serverip, Integer.parseInt(clientport),
                        Integer.parseInt(serverport),true);
            	String configFileName = state.getconfigfilename();
            	BufferedWriter bw = null;
            	BufferedReader fileReader = new BufferedReader(
                        new FileReader(configFileName));
                String configLine;
            	int i = 0;

                while ((configLine = fileReader.readLine()) != null) {
                	if(configLine.contains(serverId)){
                		i = i + 1;
                	}
                	
                }
                if(i==0){
            		try{
                		bw = new BufferedWriter(new FileWriter(configFileName, true));

                		bw.write(serverId + "	" + serverip + "	" + clientport + "	" + serverport);        		 
                		bw.newLine();
                		bw.flush();
                		bw.close();
                	}catch(IOException e) {
                		e.printStackTrace();
                	} 
            	}

            	state.addremoteconfig(server);
            	System.out.println(state.getRemoteConfig());
            	RemoteChatroomInfo remotechatroom = new RemoteChatroomInfo("MainHall-" + serverId, server);
            	state.createRemoteChatroom(remotechatroom);
            	replyToServer = ServerMessages.sendserverinfo
            			(state.getServerIdentity(), state.getLocalChatrooms()).toJSONString();
                    break;
                case "heartbeat":
                    serverid = (String) requestFromClient.get("serverid");
                    replyToServer = ServerMessages.getHeartbeatReponse(serverid, true, state.getLocalChatrooms().keySet()).toJSONString();
                    break;
            }
            if(!type.equals("heartbeat"))
            {
                System.out.println("Request from "+sourceServer+ ":: "+ managementMsg);
                System.out.println("Response :: "+replyToServer);
            }
        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println("Parse Exception");
        }
        return replyToServer;
    }

    public synchronized void write(String msg) {
        try {
            writer.write(msg + "\n");
            writer.flush();
            System.out.println(Thread.currentThread().getName()
                    + " - Message sent to Server " + managementNum);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
