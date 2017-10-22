package au.edu.unimelb.tcp.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MessageReceiveThread implements Runnable {

    private SSLSocket socket;
    private State state;
    private boolean debug;

    private BufferedReader in;

    private JSONParser parser = new JSONParser();

    private boolean run = true;

    private MessageSendThread messageSendThread;

    public MessageReceiveThread(SSLSocket socket, State state,
            MessageSendThread messageSendThread, boolean debug)
            throws IOException {
        this.socket = socket;
        this.state = state;
        this.messageSendThread = messageSendThread;
        this.debug = debug;
    }

    @Override
    public void run() {

        try {
            this.in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));
            JSONObject message;
            while (run) {
                message = (JSONObject) parser.parse(in.readLine());
                if (debug) {
                    System.out.println("Receiving: " + message.toJSONString());
                    System.out.print("[" + state.getRoomId() + "] "
                            + state.getUserName() + "> ");
                }
                MessageReceive(socket, message);
            }
            System.exit(0);
            in.close();
            socket.close();
        } catch (ParseException e) {
            System.out.println("Message Error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Communication Error: " + e.getMessage());
            System.exit(1);
        }

    }

    public void MessageReceive(SSLSocket socket, JSONObject message)
            throws IOException, ParseException {
        String type = (String) message.get("type");

        // server reply of #newidentity
        if (type.equals("newidentity")) {
            boolean approved = Boolean
                    .parseBoolean((String) message.get("approved"));

            // terminate program if failed
            if (!approved) {
                if (message.get("name") != null)
                    System.out.println("You are already logged into the chat, "
                            + message.get("name"));
                else
                    System.out.println(
                            "Sorry, we are unable to determine your identity right now");

                socket.close();
                System.exit(1);
            }
            String identity = (String) message.get("identity");
            state.setIdentity(identity);
            String username = (String) message.get("name");
            state.setUserName(username);

            return;
        }

        // server reply of #list
        if (type.equals("roomlist")) {
            JSONArray array = (JSONArray) message.get("rooms");
            // print all the rooms
            System.out.print("List of chat rooms:");
            for (int i = 0; i < array.size(); i++) {
                System.out.print(" " + array.get(i));
            }
            System.out.println();
            System.out.print("[" + state.getRoomId() + "] "
                    + state.getUserName() + "> ");
            return;
        }

        // server sends roomchange
        if (type.equals("roomchange")) {

            // identify whether the user has quit!
            if (message.get("roomid").equals("")) {
                // quit initiated by the current client
                if (message.get("identity").equals(state.getIdentity())) {
                    System.out.println(message.get("name") + " has quit!");
                    in.close();
                    System.exit(1);
                } else {
                    System.out.println(message.get("name") + " has quit!");
                    System.out.print("[" + state.getRoomId() + "] "
                            + state.getUserName() + "> ");
                }
                // identify whether the client is new or not
            } else if (message.get("former").equals("")) {
                // change state if it's the current client
                if (message.get("identity").equals(state.getIdentity())) {
                    state.setRoomId((String) message.get("roomid"));
                }
                System.out.println(message.get("name") + " moves to "
                        + (String) message.get("roomid"));
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
                // identify whether roomchange actually happens
            } else if (message.get("former").equals(message.get("roomid"))) {
                System.out.println("room unchanged");
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }
            // print the normal roomchange message
            else {
                // change state if it's the current client
                if (message.get("identity").equals(state.getIdentity())) {
                    state.setRoomId((String) message.get("roomid"));
                }

                System.out.println(message.get("name") + " moves from "
                        + message.get("former") + " to "
                        + message.get("roomid"));
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }
            return;
        }

        // server reply of #who
        if (type.equals("roomcontents")) {
            JSONArray arrayNames = (JSONArray) message.get("names");
            JSONArray arrayIds = (JSONArray) message.get("identities");
            System.out.print(message.get("roomid") + " contains");
            for (int i = 0; i < arrayIds.size(); i++) {
                System.out.print(" " + arrayNames.get(i));
                if (message.get("owner").equals(arrayIds.get(i))) {
                    System.out.print("*");
                }
                System.out.print(",");
            }
            System.out.println();
            System.out.print("[" + state.getRoomId() + "] "
                    + state.getUserName() + "> ");
            return;
        }

        // server forwards message
        if (type.equals("message")) {
            System.out.println(
                    message.get("name") + ": " + message.get("content"));
            System.out.print("[" + state.getRoomId() + "] "
                    + state.getUserName() + "> ");
            return;
        }

        // server reply of #createroom
        if (type.equals("createroom")) {
            boolean approved = Boolean
                    .parseBoolean((String) message.get("approved"));
            String temp_room = (String) message.get("roomid");
            if (!approved) {
                System.out.println("Create room " + temp_room + " failed.");
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            } else {
                System.out.println("Room " + temp_room + " is created.");
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }
            return;
        }

        // server reply of # deleteroom
        if (type.equals("deleteroom")) {
            boolean approved = Boolean
                    .parseBoolean((String) message.get("approved"));
            String temp_room = (String) message.get("roomid");
            if (!approved) {
                System.out.println("Delete room " + temp_room + " failed.");
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            } else {
                System.out.println("Room " + temp_room + " is deleted.");
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }
            return;
        }

        // server directs the client to another server
        if (type.equals("route")) {
            String temp_room = (String) message.get("roomid");
            String host = (String) message.get("host");
            int port = Integer.parseInt((String) message.get("port"));

            // connect to the new server
            if (debug) {
                System.out
                        .println("Connecting to server " + host + ":" + port);
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }

            String suites[] = { "TLS_KRB5_WITH_RC4_128_SHA",
                    "TLS_KRB5_WITH_RC4_128_MD5",
                    "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
                    "TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
                    "TLS_KRB5_WITH_DES_CBC_SHA", "TLS_KRB5_WITH_DES_CBC_MD5",
                    "TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
                    "TLS_KRB5_EXPORT_WITH_RC4_40_MD5",
                    "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
                    "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5",
                    "SSL_RSA_WITH_RC4_128_SHA",
                    "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
                    "TLS_ECDH_RSA_WITH_RC4_128_SHA",
                    "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
                    "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                    "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                    "SSL_RSA_WITH_RC4_128_MD5",
                    "TLS_EMPTY_RENEGOTIATION_INFO_SCSV" };

            System.setProperty("javax.net.ssl.trustStore",
                    Client.class.getResource("KS").getPath());
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory
                    .getDefault();
            SSLSocket temp_socket = (SSLSocket) sslSocketFactory
                    .createSocket(host, port);
            temp_socket.setEnabledCipherSuites(suites);

            // send #movejoin
            DataOutputStream out = new DataOutputStream(
                    temp_socket.getOutputStream());
            JSONObject request = ClientMessages.getMoveJoinRequest(
                    state.getIdentity(), state.getRoomId(), temp_room,
                    state.getAccesstoken(), state.getUserName());
            if (debug) {
                System.out.println("Sending: " + request.toJSONString());
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }
            send(out, request);

            // wait to receive serverchange
            BufferedReader temp_in = new BufferedReader(
                    new InputStreamReader(temp_socket.getInputStream()));
            JSONObject obj = (JSONObject) parser.parse(temp_in.readLine());

            if (debug) {
                System.out.println("Receiving: " + obj.toJSONString());
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }

            // serverchange received and switch server
            if (obj.get("type").equals("serverchange")
                    && obj.get("approved").equals("true")) {
                messageSendThread.switchServer(temp_socket, out);
                switchServer(temp_socket, temp_in);
                String serverid = (String) obj.get("serverid");
                System.out.println(state.getUserName() + " switches to server "
                        + serverid);
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }
            // receive invalid message
            else {
                temp_in.close();
                out.close();
                temp_socket.close();
                System.out.println("Server change failed");
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }
            return;
        }

        if (debug) {
            System.out.println("Unknown Message: " + message);
            System.out.print("[" + state.getRoomId() + "] "
                    + state.getUserName() + "> ");
        }
    }

    public void switchServer(SSLSocket temp_socket, BufferedReader temp_in)
            throws IOException {
        in.close();
        in = temp_in;
        socket.close();
        socket = temp_socket;
    }

    private void send(DataOutputStream out, JSONObject obj)
            throws IOException {
        out.write((obj.toJSONString() + "\n").getBytes("UTF-8"));
        out.flush();
    }

    public boolean serverAlive() throws IOException, ParseException {
        this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"));
        JSONObject message = (JSONObject) parser.parse(in.readLine());
        if (debug) {
            System.out.println("Receiving: " + message.toJSONString());
        }
        in.close();
        socket.close();
        String type = (String) message.get("type");
        if (type.equals("serverping")) {
            boolean alive = Boolean
                    .parseBoolean((String) message.get("alive"));
            if (alive)
                return true;
        }
        return false;
    }
}
