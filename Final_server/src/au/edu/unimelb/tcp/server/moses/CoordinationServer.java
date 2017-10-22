package au.edu.unimelb.tcp.server.moses;

import java.io.IOException;
import java.net.InetAddress;


import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class CoordinationServer extends Thread {
	
    public static final int CONNECTION_BACKLOG = 50;
    
    SSLServerSocket listeningSocket = null;
    int port;
    String serverAddress;
    static int serverConnectionCount = 0;

    public CoordinationServer(String serverAddress, int port) {
        this.port = port;
        this.serverAddress = serverAddress;
       // this.listeningSocket = null;
    }

    @Override
    public void run() {
        try {

        	
        	// Create a server socket listening on Coordination server port
        	// System.setProperty("javax.net.ssl.keyStore", Server.class.getResource("cacerts").getPath());
            // System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
             SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
             listeningSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port, CONNECTION_BACKLOG, 
            		 InetAddress.getByName(serverAddress));
             //listeningSocket.setEnabledCipherSuites(suites);
             
             System.out.println(Thread.currentThread().getName()
                    + " - Coordination Server listening on port " + port
                    + " for a server connection");
            
            // Keep track of the number of clients
            int managementNum = 0;

            // Listen for incoming connections for ever
            while (true) {
           
                // Accept an incoming client connection request
                //System.out.println(Thread.currentThread().getName()
                //        + " - Server conection accepted");
                SSLSocket coordinatingServerSocket = (SSLSocket) listeningSocket.accept();
                managementNum++;
                
                // Create one thread per client connection, each thread will be
                // responsible for listening for messages from the client
                // and then 'handing' them to the client manager (coordinating
                // singleton)
                // to process them
                CoordinationServerConnection clientConnection = new CoordinationServerConnection(
                        coordinatingServerSocket, managementNum);
                clientConnection.setName("Management Thread" + managementNum);
                clientConnection.start();

                // Register the new client connection with the client manager
                // ClientManager.getInstance().clientConnected(clientConnection);
            }
        } catch (Exception e) {
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
}
