package au.edu.unimelb.tcp.server.moses;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.jayway.jsonpath.JsonPath;

public class ClientConnection extends Thread {

    private SSLSocket clientSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private BlockingQueue<Message> messageQueue;
    private int clientNum;
    private UserInfo user;

    public static final String MAIN_HALL_NAME_PREFIX = "MainHall-";
    public static final String MAIN_HALL_OWNER_IDENTITY = "";
    public static final String NEW_USER_FORMER_ROOM = "";
    public static final int ID_LENGTH_LOWER_BOUND = 3;
    public static final int ID_LENGTH_UPPER_BOUND = 16;

    public ClientConnection(SSLSocket clientSocket, int clientNum) {
        try {
            this.clientSocket = clientSocket;
            reader = new BufferedReader(new InputStreamReader(
                    clientSocket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(
                    clientSocket.getOutputStream(), "UTF-8"));
            messageQueue = new LinkedBlockingQueue<Message>();
            this.clientNum = clientNum;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {

            ClientMessageReader messageReader = new ClientMessageReader(reader,
                    messageQueue);
            messageReader.setName(this.getName() + "Reader");
            messageReader.start();

            System.out.println(Thread.currentThread().getName()
                    + " - Processing client " + clientNum + "  messages");

            while (true) {

                Message msg = messageQueue.take();
                System.out.println("Message is " + msg);

                if (!msg.isFromClient() && msg.getMessage().equals("exit")) {
                    user.destroy(false);
                    break;
                }

                if (msg.isFromClient()) {
                    System.out.println(Thread.currentThread().getName()
                            + " - Received qeueue msg from Client"
                            + msg.getMessage());

                    if (handleClientMessage(msg))
                        break;
                } else {
                    System.out.println(Thread.currentThread().getName()
                            + " - Received qeueue msg from Server"
                            + msg.getMessage());
                    // need to handle exit here otherwise just write the
                    // message
                    write(msg.getMessage());
                }
            }

            if (clientSocket != null)
                clientSocket.close();
            System.out.println(Thread.currentThread().getName() + " - Client "
                    + clientNum + " disconnected");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // return true to close thread
    @SuppressWarnings("unchecked")
    private boolean handleClientMessage(Message msg)
            throws UnknownHostException, IOException {
        ServerState state = ServerState.getInstance();
        JSONParser parser = new JSONParser();
        JSONObject requestFromClient;
        JSONObject replyToClient = new JSONObject();
        try {
            requestFromClient = (JSONObject) parser.parse(msg.getMessage());
            String type = (String) requestFromClient.get("type");
            String userIdentity, roomIdentity, token;
            System.out.println(
                    "Client Message Type Received is ::" + type + "::");
            switch (type) {
            case "serverping":
                replyToClient = ServerMessages.getPingResponse();
                write(replyToClient.toJSONString());
                return true;
            case "newidentity":
                token = (String) requestFromClient.get("token");
                FbUser fbUser = validateFBLogin(token);

                boolean validUser = fbUser != null
                        && state.getConnectedUser(fbUser.getUserid()) == null
                        && !state.isUserLocked(fbUser.getUserid());
                boolean allServersApprovedUser = false;

                if (validUser) {
                    allServersApprovedUser = lockServersApproval("identity",
                            fbUser.getUserid());
                    if (allServersApprovedUser) {
                        UserInfo newUser = new UserInfo(fbUser.getUserid(),
                                this, this.clientSocket,
                                MAIN_HALL_NAME_PREFIX
                                        + ServerState.getServerIdentity(),
                                "", fbUser.getName());
                        this.user = newUser;
                    }
                    releaseServers("identity", fbUser.getUserid(), null);
                }
                System.out.println("Valid User ? " + fbUser != null);
                replyToClient = ServerMessages.getNewIdentityResponse(
                        validUser && allServersApprovedUser,
                        fbUser != null ? fbUser.getUserid() : null,
                        fbUser != null ? fbUser.getName() : null);
                write(replyToClient.toJSONString());
                if (allServersApprovedUser) {
                    JSONObject roomchange = ServerMessages
                            .getRoomChangeNotification(user.getIdentity(),
                                    NEW_USER_FORMER_ROOM,
                                    MAIN_HALL_NAME_PREFIX
                                            + ServerState.getServerIdentity(),
                                    user.getName());
                    write(roomchange.toJSONString());
                } else
                    return true;
                break;
            case "list":
                replyToClient = ServerMessages.getListChatroomsResponse(
                        state.getLocalChatrooms().keySet(),
                        state.getRemoteChatrooms().keySet());
                write(replyToClient.toJSONString());
                break;
            case "who":
                replyToClient = ServerMessages.getRoomContents(
                        state.getLocalChatroomById(user.getCurrentChatroom()));
                write(replyToClient.toJSONString());
                break;
            case "createroom":
                roomIdentity = (String) requestFromClient.get("roomid");
                boolean validRoomId = state
                        .getLocalChatroomByOwner(user.getIdentity()) == null
                        && StringUtils.isAlphanumeric(roomIdentity)
                        && Character.isLetter(roomIdentity.charAt(0))
                        && roomIdentity.length() >= ID_LENGTH_LOWER_BOUND
                        && roomIdentity.length() <= ID_LENGTH_UPPER_BOUND
                        && state.getLocalChatroomById(roomIdentity) == null
                        && state.getRemoteChatroom(roomIdentity) == null
                        && !state.isChatroomLocked(roomIdentity);
                boolean allServersApprovedRoom = false;
                String formerChatroom = this.user.getCurrentChatroom();
                if (validRoomId) {
                    allServersApprovedRoom = lockServersApproval("roomid",
                            roomIdentity);
                    if (allServersApprovedRoom) {
                        new LocalChatroomInfo(roomIdentity, user);
                    }
                    releaseServers("roomid", roomIdentity,
                            allServersApprovedRoom);
                }

                replyToClient = ServerMessages.getCreateRoomResponse(
                        roomIdentity, (validRoomId && allServersApprovedRoom));
                write(replyToClient.toJSONString());
                if (allServersApprovedRoom) {
                    JSONObject roomchange = ServerMessages
                            .getRoomChangeNotification(user.getIdentity(),
                                    formerChatroom, user.getCurrentChatroom(),
                                    user.getName());
                    write(roomchange.toJSONString());
                }

                break;
            case "join":
                roomIdentity = (String) requestFromClient.get("roomid");
                boolean isNotOwner = state.getLocalChatroomByOwner(
                        this.user.getIdentity()) == null;
                if (!user.getCurrentChatroom().equals(roomIdentity)
                        && state.getLocalChatroomById(roomIdentity) != null
                        && isNotOwner) {
                    replyToClient = ServerMessages.getRoomChangeNotification(
                            this.user.getIdentity(),
                            this.user.getCurrentChatroom(), roomIdentity,
                            this.user.getName());
                    this.user.setCurrentChatroom(roomIdentity, true);
                } else if (state.getRemoteChatrooms().containsKey(roomIdentity)
                        && isNotOwner) {
                    replyToClient = ServerMessages.getRouteResponse(
                            roomIdentity,
                            state.getRemoteChatroom(
                                    roomIdentity).managingServer
                                            .getServerAddress(),
                            state.getRemoteChatroom(
                                    roomIdentity).managingServer
                                            .getClientsPort());
                    user.leaveServer(roomIdentity);
                    write(replyToClient.toJSONString());
                    return true;
                } else {
                    replyToClient = ServerMessages.getRoomChangeNotification(
                            this.user.getIdentity(),
                            this.user.getCurrentChatroom(),
                            this.user.getCurrentChatroom(),
                            this.user.getName());
                }
                write(replyToClient.toJSONString());

                break;
            case "movejoin":
                token = (String) requestFromClient.get("token");
                userIdentity = (String) requestFromClient.get("identity");
                String name = (String) requestFromClient.get("name");
                String formerRoom = (String) requestFromClient.get("former");
                roomIdentity = (String) requestFromClient.get("roomid");

                FbUser movedFbUser = validateFBLogin(token);

                if (movedFbUser != null && movedFbUser.getName().equals(name)
                        && movedFbUser.getUserid().equals(userIdentity)
                        && state.getConnectedUser(userIdentity) == null) {
                    UserInfo newUser = new UserInfo(userIdentity, this,
                            this.clientSocket, roomIdentity, formerRoom, name);
                    this.user = newUser;

                    replyToClient = ServerMessages.getServerChangeResponse(
                            true, ServerState.getServerIdentity());
                    write(replyToClient.toJSONString());
                    JSONObject roomchange = ServerMessages
                            .getRoomChangeNotification(user.getIdentity(),
                                    formerRoom, user.getCurrentChatroom(),
                                    user.getName());
                    write(roomchange.toJSONString());
                } else {
                    replyToClient = ServerMessages.getServerChangeResponse(
                            false, ServerState.getServerIdentity());
                    write(replyToClient.toJSONString());
                }
                break;
            case "deleteroom":
                roomIdentity = (String) requestFromClient.get("roomid");
                if (state.getLocalChatroomById(roomIdentity) == null) {
                    replyToClient = ServerMessages
                            .getDeleteRoomResponse(roomIdentity, false);
                    write(replyToClient.toJSONString());

                } else {
                    state.getLocalChatroomById(roomIdentity)
                            .destroy(user.getIdentity(), false, true);
                }

                break;
            case "message":
                requestFromClient.put("identity", user.getIdentity());
                requestFromClient.put("name", user.getName());
                Message broadcastMsg = new Message(false,
                        requestFromClient.toString());
                state.getLocalChatroomById(user.getCurrentChatroom())
                        .broadcastMessage(broadcastMsg);
                break;
            case "quit":
                user.destroy(true);
                return true;
            }
            System.out.println(state);
        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println("Parse Exception");
        }
        return false;

    }

    public boolean lockServersApproval(String commandType,
            String lockingParameter) throws UnknownHostException, IOException {
        ServerState state = ServerState.getInstance();
        String serverid = ServerState.getServerIdentity();
        JSONObject sendToServer = new JSONObject();
        if ("roomid".equals(commandType)
                && state.lockChatroom(lockingParameter, serverid)) {
            sendToServer = ServerMessages.getLockRoomRequest(serverid,
                    lockingParameter);
        } else if ("identity".equals(commandType)
                && state.lockUser(lockingParameter, serverid)) {
            sendToServer = ServerMessages.getLockIdentityRequest(serverid,
                    lockingParameter);
        } else
            return false;
        List<ServerInfo> remoteServers = (ServerState.getInstance())
                .getRemoteConfig();
        for (ServerInfo server : remoteServers) {
            if (server.getStatus()) {

                // System.setProperty("javax.net.ssl.trustStore",
                // Server.class.getResource("cacerts").getPath());
                // System.setProperty("javax.net.debug","all");

                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory
                        .getDefault();
                SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(
                        server.getServerAddress(),
                        server.getCoordinationPort());
                // socket.setEnabledCipherSuites(suites);
                boolean approved = Boolean.parseBoolean(
                        (String) MessageRequestReply(socket, sendToServer)
                                .get("locked"));
                socket.close();
                if (!approved) {
                    System.out.println("Lock not obtained");
                    return false;
                }
            }
        }
        System.out.println("Lock obtained");
        return true;
    }

    public boolean releaseServers(String commandType, String notifyParameter,
            Boolean approval) throws UnknownHostException, IOException {
        ServerState state = ServerState.getInstance();
        String serverid = ServerState.getServerIdentity();
        JSONObject sendToServer = new JSONObject();
        if ("roomid".equals(commandType)
                && state.unlockChatroom(notifyParameter, serverid))
            sendToServer = ServerMessages.getReleaseRoomNotification(serverid,
                    notifyParameter, approval);
        else if ("identity".equals(commandType)
                && state.unlockUser(notifyParameter, serverid))
            sendToServer = ServerMessages
                    .getReleaseIdentityNotification(serverid, notifyParameter);
        else
            return false;
        List<ServerInfo> remoteServers = (ServerState.getInstance())
                .getRemoteConfig();
        for (ServerInfo server : remoteServers) {
            if (server.getStatus()) {
                // System.setProperty("javax.net.ssl.trustStore",
                // Server.class.getResource("cacerts").getPath());
                // System.setProperty("javax.net.debug","all");
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory
                        .getDefault();
                SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(
                        server.getServerAddress(),
                        server.getCoordinationPort());
                // socket.setEnabledCipherSuites(suites);
                MessageNotification(socket, sendToServer);

                socket.close();
            }
        }
        return true;
    }

    public void MessageNotification(SSLSocket socket, JSONObject reqMsg)
            throws IOException {
        // System.setProperty("javax.net.ssl.trustStore", "KS");
        // System.setProperty("javax.net.debug","all");
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        System.out.println("Sending: " + reqMsg.toJSONString());
        out.write((reqMsg.toJSONString() + "\n").getBytes("UTF-8"));
        out.flush();
    }

    public JSONObject MessageRequestReply(SSLSocket socket, JSONObject reqMsg)
            throws IOException {
        // System.setProperty("javax.net.ssl.trustStore", "KS");
        // System.setProperty("javax.net.debug","all");
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"));
        JSONParser parser = new JSONParser();
        JSONObject resMsg = null;
        System.out.println("Sending: " + reqMsg.toJSONString());
        out.write((reqMsg.toJSONString() + "\n").getBytes("UTF-8"));
        out.flush();
        try {
            resMsg = (JSONObject) parser.parse(in.readLine());
        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println("Message Error: " + e.getMessage());
            System.exit(1);
        }
        return resMsg;
    }

    public BlockingQueue<Message> getMessageQueue() {
        return messageQueue;
    }

    public synchronized void write(String msg) {
        if (this.user != null) {
            System.out.println(Thread.currentThread().getName()
                    + " - Message sent to client " + user.getIdentity());
            System.out.println(msg);
        }
        try {
            writer.write((msg + "\n"));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    final class FbUser {
        private final String name;
        private final String userid;

        public FbUser(String name, String userid) {
            this.name = name;
            this.userid = userid;
        }

        public String getName() {
            return name;
        }

        public String getUserid() {
            return userid;
        }
    }

    public FbUser validateFBLogin(String token) {
        try {

            // System.setProperty("javax.net.ssl.trustStore", "cacerts");
            String endpoint = "https://graph.facebook.com/debug_token?access_token=353901168276665|YxNfe88eOATUgQStUFMHbY5bGl4&input_token="
                    + token;
            StringBuilder result = new StringBuilder();
            StringBuilder result2 = new StringBuilder();
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            System.out.println(result.toString());
            JSONParser parse = new JSONParser();
            JSONObject jsonObj = (org.json.simple.JSONObject) parse
                    .parse(result.toString());
            System.out.println("JSON OBject is " + jsonObj.toJSONString());
            System.out.println("JSON "
                    + JsonPath.read(result.toString(), "$.data.is_valid"));
            if (JsonPath.read(result.toString(), "$.data.is_valid")
                    .equals(new Boolean(true))) {
                System.out.println("All good mate");
                endpoint = "https://graph.facebook.com/me?access_token="
                        + token;
                URL url2 = new URL(endpoint);
                HttpURLConnection conn2 = (HttpURLConnection) url2
                        .openConnection();
                conn2 = (HttpURLConnection) url2.openConnection();
                conn2.setRequestMethod("GET");
                BufferedReader rds = new BufferedReader(
                        new InputStreamReader(conn2.getInputStream()));
                while ((line = rds.readLine()) != null) {
                    result2.append(line);
                }
                jsonObj = (org.json.simple.JSONObject) parse
                        .parse(result2.toString());
                System.out.println("JSON OBject is " + jsonObj.toJSONString());

                // System.out.println("JSON
                // "+JsonPath.read(result.toString(), "$.data.is_valid"));
                rds.close();
                String name = JsonPath.read(jsonObj.toJSONString(), "$.name");
                String userid = JsonPath.read(jsonObj.toJSONString(), "$.id");
                System.out.println(name + "::" + userid);
                return new FbUser(name, userid);
            }
        } catch (UnknownHostException e) {
            System.out.println("Cannot connect to Facebook");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
