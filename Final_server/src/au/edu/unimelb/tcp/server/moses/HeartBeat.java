package au.edu.unimelb.tcp.server.moses;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Created by Zephyr on 14/10/16.
 */

public class HeartBeat extends Thread {

    private static final int FREQUENCY = 5;
    private static final int SOCKET_TIMEOUT = 2;
    private ServerState state = ServerState.getInstance();

    public void run() {

        System.out.println("Heart Beat Thread Started");
        while (true) {

            try {
                JSONObject heartbeat = ServerMessages.getHeartbeatRequest(ServerState.getServerIdentity());
                List<ServerInfo> remoteServers = state.getRemoteConfig();
//               System.out.println("size of Remote Servers"+remoteServers.size());
                for (ServerInfo server : remoteServers) {
                    JSONObject msg = null;
                    boolean approved;
                    try {
                  //      System.setProperty("javax.net.ssl.trustStore", Server.class.getResource("cacerts").getPath());
                    //    System.setProperty("javax.net.debug","all");
                        
                       SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                       SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(server.getServerAddress(),
                               server.getCoordinationPort());

 //                       Socket socket = new Socket(server.getServerAddress(),
 //                               server.getCoordinationPort());
                        //                 System.out.println("Sending HEartbeat to "+server.getServerId());
                        msg = MessageRequestReply(socket, heartbeat);
                        //                 System.out.println("Recieved HEartbeat: "+msg.toJSONString());

                        approved = Boolean.parseBoolean(
                                (String) msg.get("approved"));
                        socket.close();
                    } catch (Exception e) {
                        approved = false;
                    }
                    if (approved == false) {
                        server.setStatus(false);
                        System.out.println("Heartbeat Response not received from "+server.getServerId());
                        state.removeRemoteChatroomByServerID(server.getServerId());
                    } else {
                        server.setStatus(true);
                        List<String> remoteRooms = new ArrayList<>();
                        JSONArray room = (JSONArray) msg.get("rooms");
                        for (int i = 0; i < room.size(); i++) {
                            remoteRooms.add((String) room.get(i));
                        }
                        state.addRemoteChatroomByServer(server, remoteRooms);
                    }
                }
                Thread.sleep(FREQUENCY * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public JSONObject MessageRequestReply(SSLSocket socket, JSONObject reqMsg)
            throws IOException, ParseException {
        socket.setSoTimeout(SOCKET_TIMEOUT * 1000);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"));
        JSONParser parser = new JSONParser();
        JSONObject resMsg = null;
        //System.out.println("Sending: " + reqMsg.toJSONString());
        out.write((reqMsg.toJSONString() + "\n").getBytes("UTF-8"));
        out.flush();
        resMsg = (JSONObject) parser.parse(in.readLine());
        return resMsg;
    }


}