package au.edu.unimelb.tcp.server.moses;

import java.io.BufferedReader;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ClientMessageReader extends Thread {

    private BufferedReader reader;
    private BlockingQueue<Message> messageQueue;

    public ClientMessageReader(BufferedReader reader,
            BlockingQueue<Message> messageQueue) {
        this.reader = reader;
        this.messageQueue = messageQueue;
    }

    @Override
    // This thread reads messages from the client's socket input stream
    public void run() {
        try {

            System.out.println(Thread.currentThread().getName()
                    + " - Reading messages from client connection");

            String clientMsg = null;
            JSONParser parser = new JSONParser();
            while ((clientMsg = reader.readLine()) != null) {
                System.out.println(Thread.currentThread().getName()
                        + " - Message from client received: " + clientMsg);
                // place the message in the queue for the client connection
                // thread to process
                Message msg = new Message(true, clientMsg);
                messageQueue.add(msg);

                // Close ClientMessageReader thread if client quits
                JSONObject message = (JSONObject) parser.parse(clientMsg);
                String type = (String) message.get("type");
                if ("quit".equals(type) || "serverping".equals(type))
                    return;
            }

            Message exit = new Message(false, "exit");
            messageQueue.add(exit);

        } catch (SocketException e) {
       //     e.printStackTrace();
            Message exit = new Message(false, "exit");
            messageQueue.add(exit);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println(Thread.currentThread().getName()
                    + " - Closing thread - no more messages will be read for client connection");
        }
    }
}
