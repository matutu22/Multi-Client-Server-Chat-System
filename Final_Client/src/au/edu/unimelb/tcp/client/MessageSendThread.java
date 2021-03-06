package au.edu.unimelb.tcp.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;

import org.json.simple.JSONObject;

public class MessageSendThread implements Runnable {

    private SSLSocket socket;

    private DataOutputStream out;

    private State state;

    private boolean debug;

    // reading from console
    public Scanner cmdin = new Scanner(System.in);

    public MessageSendThread(SSLSocket socket, State state, boolean debug)
            throws IOException {
        this.socket = socket;
        this.state = state;
        out = new DataOutputStream(socket.getOutputStream());
        this.debug = debug;
    }

    @Override
    public void run() {

        try {
            // send the #newidentity command
            MessageSend(socket, "#newidentity " + state.getAccesstoken());
        } catch (IOException e1) {
            e1.printStackTrace();
            System.exit(1);
        }

        while (true) {
            String msg = cmdin.nextLine();
            System.out.print("[" + state.getRoomId() + "] "
                    + state.getUserName() + "> ");
            try {
                MessageSend(socket, msg);
            } catch (IOException e) {
                System.out.println("Communication Error: " + e.getMessage());
                System.exit(1);
            }
        }

    }

    private void send(JSONObject obj) throws IOException {
        if (debug) {
            System.out.println("Sending: " + obj.toJSONString());
            if(state!=null)
            System.out.print("[" + state.getRoomId() + "] "
                    + state.getUserName() + "> ");
        }
        out.write((obj.toJSONString() + "\n").getBytes("UTF-8"));
        out.flush();
    }

    // send command and check validity
    public void MessageSend(SSLSocket socket, String msg) throws IOException {

        JSONObject sendToServer = new JSONObject();
        String[] array = msg.split(" ");
        if (!array[0].startsWith("#")) {
            sendToServer = ClientMessages.getMessage(msg);
            send(sendToServer);
        } else if (array.length == 1) {
            if (array[0].startsWith("#pingServer")) {
                sendToServer = ClientMessages.getServerPingRequest();
                send(sendToServer);
            } else if (array[0].startsWith("#list")) {
                sendToServer = ClientMessages.getListRequest();
                send(sendToServer);
            } else if (array[0].startsWith("#quit")) {
                sendToServer = ClientMessages.getQuitRequest();
                send(sendToServer);
            } else if (array[0].startsWith("#who")) {
                sendToServer = ClientMessages.getWhoRequest();
                send(sendToServer);
            } else {
                System.out.println("Invalid command!");
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }
        } else if (array.length == 2) {
            if (array[0].startsWith("#joinroom")) {
                sendToServer = ClientMessages.getJoinRoomRequest(array[1]);
                send(sendToServer);
            } else if (array[0].startsWith("#createroom")) {
                sendToServer = ClientMessages.getCreateRoomRequest(array[1]);
                send(sendToServer);
            } else if (array[0].startsWith("#deleteroom")) {
                sendToServer = ClientMessages.getDeleteRoomRequest(array[1]);
                send(sendToServer);
            } else if (array[0].startsWith("#newidentity")) {
                sendToServer = ClientMessages.getNewIdentityRequest(array[1]);
                send(sendToServer);
            } else {
                System.out.println("Invalid command!");
                System.out.print("[" + state.getRoomId() + "] "
                        + state.getUserName() + "> ");
            }
        } else {
            System.out.println("Invalid command!");
            System.out.print("[" + state.getRoomId() + "] "
                    + state.getUserName() + "> ");
        }

    }

    public void switchServer(SSLSocket temp_socket, DataOutputStream temp_out)
            throws IOException {
        // switch server initiated by the receiving thread
        // need to use synchronize
        synchronized (out) {
            out.close();
            out = temp_out;
        }
        socket = temp_socket;
    }
}
