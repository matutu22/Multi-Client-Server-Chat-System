package au.edu.unimelb.tcp.server.moses;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Thread.State;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class Server {
    public static final int CONNECTION_BACKLOG = 50;

    public static void main(String[] args) {

        // System.setProperty("javax.net.debug","all");

        // System.setProperty("javax.net.ssl.keyStore",
        // Server.class.getResource("KS").getPath());
        // System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        ComLineValues values = new ComLineValues();
        CmdLineParser parser = new CmdLineParser(values);
        JSONParser jsonparser = new JSONParser();
        JSONObject replyfromserver;
        String serverId, configFileName, serverip, clientport, serverport;
        try {
            Path batFile = Files.createTempFile("keystore", ".jks");
            try (InputStream stream = Server.class
                    .getResourceAsStream("cacerts")) {
                Files.copy(stream, batFile,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            System.setProperty("javax.net.ssl.keyStore",
                    batFile.toAbsolutePath().toString());
            System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
            System.setProperty("javax.net.ssl.trustStore",
                    batFile.toAbsolutePath().toString());

            parser.parseArgument(args);
            serverId = values.getServerId();
            configFileName = values.getServerConfig();
            // add here

            if (values.getserverIP() != null && values.getserverport() != null
                    && values.getclientport() != null) {

                serverip = values.getserverIP();
                clientport = values.getclientport();
                serverport = values.getserverport();
                BufferedWriter bw = null;
                JSONObject sendToServer = new JSONObject();

                try {
                    bw = new BufferedWriter(
                            new FileWriter(configFileName, true));

                    bw.write(serverId + "	" + serverip + "	" + clientport
                            + "	" + serverport);
                    bw.newLine();
                    bw.flush();
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Config.loadConfig(configFileName, serverId);
                (ServerState.getInstance()).setInitialServerState(
                        Config.localConfig.getServerId(), configFileName,
                        Config.remoteConfig);

                List<ServerInfo> remoteServers = (ServerState.getInstance())
                        .getRemoteConfig();
                // checkFacebookAuthenticationConnection
                checkFacebookAuthenticationConnection();

                // Start Coordination server
                CoordinationServer coordinator = new CoordinationServer(
                        Config.localConfig.getServerAddress(),
                        Config.localConfig.getCoordinationPort());
                coordinator.setName("Coordination Server Thread");
                coordinator.start();

                // start transfer data with existing servers
                for (ServerInfo server : remoteServers) {
                    // System.setProperty("javax.net.ssl.trustStore",
                    // Server.class.getResource("cacerts").getPath());
                    // System.setProperty("javax.net.debug","all");

                    SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory
                            .getDefault();
                    SSLSocket socket = null;
                    try {
                        socket = (SSLSocket) sslSocketFactory.createSocket(
                                server.getServerAddress(),
                                server.getCoordinationPort());
                        // Socket socket = new
                        // Socket(server.getServerAddress(),
                        // server.getCoordinationPort());
                        sendToServer = ServerMessages.newserver(serverId,
                                serverip, clientport, serverport);
                        MessageRequestReply(socket, sendToServer);
                    } catch (Exception e) {
                        System.out.println(
                                "Cannot connect to " + server.getServerId());
                    }
                    if (socket != null)
                        socket.close();

                }
                // Start HeartBeat Thread

                HeartBeat heartBeat = new HeartBeat();
                heartBeat.setName("Heartbeat Thread");
                heartBeat.start();

                startChatServer(Config.localConfig.getServerAddress(),
                        Config.localConfig.getClientsPort());
            } else {

                Config.loadConfig(configFileName, serverId);
                (ServerState.getInstance()).setInitialServerState(
                        Config.localConfig.getServerId(), configFileName,
                        Config.remoteConfig);
                // System.out.println(ServerState.getInstance().getRemoteConfig());

                // checkFacebookAuthenticationConnection
                checkFacebookAuthenticationConnection();
                // Start Coordination server

                CoordinationServer coordinator = new CoordinationServer(
                        Config.localConfig.getServerAddress(),
                        Config.localConfig.getCoordinationPort());
                coordinator.setName("Coordination Server Thread");
                coordinator.start();
                // Start HeartBeat Thread

                HeartBeat heartBeat = new HeartBeat();
                heartBeat.setName("Heartbeat Thread");
                heartBeat.start();

                startChatServer(Config.localConfig.getServerAddress(),
                        Config.localConfig.getClientsPort());
            }
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.out);
        } catch (IOException e) {
            e.printStackTrace();
            Runtime.getRuntime().halt(0);
        }
    }

    public static JSONObject MessageRequestReply(SSLSocket socket,
            JSONObject reqMsg) throws IOException {
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
        JSONArray rooms = (JSONArray) resMsg.get("chatrooms");
        for (int i = 0; i < rooms.size(); i++) {
            String room = (String) rooms.get(i);
            for (ServerInfo server : ServerState.getInstance()
                    .getRemoteConfig()) {
                if (server.getServerId().equals(resMsg.get("serverid"))) {
                    RemoteChatroomInfo remoteroom = new RemoteChatroomInfo(
                            room, server);
                    ServerState.getInstance().createRemoteChatroom(remoteroom);
                }

            }

        }
        return resMsg;
    }

    public static void startChatServer(String serverAddress, int port)
            throws IOException {
        // System.setProperty("javax.net.ssl.keyStore", "C:\\KS");
        // Server.class.getResource("KS");
        // getClassLoader().getResource("au/edu/unimelb/tcp/server/moses/KS");
        // System.setProperty("javax.net.debug","all");

        // System.setProperty("javax.net.ssl.keyStore",
        // Server.class.getResource("KS").getPath());
        // System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        // System.setProperty("javax.net.debug","all");

        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory
                .getDefault();
        SSLServerSocket listeningSocket = (SSLServerSocket) sslServerSocketFactory
                .createServerSocket(port, CONNECTION_BACKLOG,
                        InetAddress.getByName(serverAddress));
        // listeningSocket.setEnabledCipherSuites(suites);

        try {

            // Create a server socket listening on given port
            Thread.currentThread().setName("Chat Server Thread");

            System.out.println(Thread.currentThread().getName()
                    + " - Server listening on port " + port
                    + " for a client connection");

            int clientNum = 0;

            // Listen for incoming connections for ever
            while (true) {

                // Accept an incoming client connection request
                SSLSocket clientSocket = (SSLSocket) listeningSocket.accept();
                System.out.println(Thread.currentThread().getName()
                        + " - Client conection accepted");
                clientNum++;

                // Create a client connection to listen for and process all the
                // messages sent by the client
                ClientConnection clientConnection = new ClientConnection(
                        clientSocket, clientNum);
                clientConnection.setName("Client Thread " + clientNum);
                clientConnection.start();

                // Update the server state to reflect the new connected client
                // ServerState.getInstance().clientConnected(clientConnection);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (listeningSocket != null) {
                try {
                    listeningSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static boolean checkFacebookAuthenticationConnection() {
        try {
            String endpoint = "https://www.facebook.com/";
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
            // System.out.println(result.toString());
            return true;
        } catch (IOException e) {
            // e.printStackTrace();
            System.out
                    .println("Cannot connect to Facebook for authentication");
            System.out
                    .println("Server will not start unless this is resolved");
            System.exit(0);
        }
        return false;
    }

}
