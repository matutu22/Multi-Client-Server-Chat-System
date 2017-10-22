package au.edu.unimelb.tcp.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.simple.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class ClientThread extends Thread {
    String authenticationMessage;
    String hostname;
    int port;
    boolean debug;

    public ClientThread(String authenticationMessage) {
        this.authenticationMessage = authenticationMessage;
        this.hostname = Client.hostname;
        this.port = Client.port;
        this.debug = Client.debug;
    }

    public void run() {
        SSLSocket socket = null;

        try {

            if (authenticationMessage.contains("error=access_denied")) {
                System.out.println("You need to grant access to KCSnM Chat for logging in");
            } else if (authenticationMessage.contains("access_token=")) {
                // start sending thread
                String queryFragment = authenticationMessage.substring(
                        authenticationMessage.indexOf("access_token=") + 13,
                        authenticationMessage.length());
                String accessToken = queryFragment.split("&")[0];
                int expiry = Integer
                        .parseInt((queryFragment.split("&")[1]).split("=")[1]);
                Calendar tokenExpiry = Calendar.getInstance(); 
                tokenExpiry.add(Calendar.SECOND, expiry);

                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory
                        .getDefault();
                socket = (SSLSocket) sslSocketFactory.createSocket(hostname,
                        port);

                au.edu.unimelb.tcp.client.State state = new au.edu.unimelb.tcp.client.State(
                        "", "", accessToken, tokenExpiry);
                MessageSendThread messageSendThread = new MessageSendThread(
                        socket, state, debug);
                Thread sendThread = new Thread(messageSendThread);
                sendThread.start();

                // start receiving thread
                Thread receiveThread = new Thread(new MessageReceiveThread(
                        socket, state, messageSendThread, debug));
                receiveThread.start();
            }

        } catch (UnknownHostException e) {
            System.out.println("Unknown host");
        } catch (IOException e) {
            System.out.println("Communication Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
