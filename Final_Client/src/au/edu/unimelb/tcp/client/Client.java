package au.edu.unimelb.tcp.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.parser.ParseException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class Client {
    public static String args[];
    public static String hostname;
    public static int port;
    public static boolean debug;

    public static void main(String[] args)
            throws IOException, ParseException, Throwable {
        Path batFile = Files.createTempFile("keystore", ".jks");
        try (InputStream stream = Client.class.getResourceAsStream("cacerts")) {
            Files.copy(stream, batFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING );
        }
        
        System.setProperty("javax.net.ssl.trustStore",
                batFile.toAbsolutePath().toString());
//                Client.class.getResource("/au/edu/unimelb/tcp/client/cacerts").getPath());
//                Client.class.getResource("cacerts").getPath());

        ComLineValues values = new ComLineValues();
        CmdLineParser parser = new CmdLineParser(values);
        try {
            parser.parseArgument(args);
            hostname = values.getHost();
            port = values.getPort();
            debug = values.isDebug();

            // Check Chat Server availability
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory
                    .getDefault();
            SSLSocket socket = (SSLSocket) sslSocketFactory
                    .createSocket(hostname, port);
            MessageSendThread messageSendThread = new MessageSendThread(socket,
                    null, debug);
            messageSendThread.MessageSend(socket, "#pingServer");
            MessageReceiveThread messageReceiveThread = new MessageReceiveThread(
                    socket, null, null, debug);
            if (!messageReceiveThread.serverAlive()) {
                System.out.println("Chat Server Unreachable");
                System.exit(0);
            }

            System.out.println("Connecting to Chat Server..");
            pressAnyKeyToContinue(messageSendThread);

            FacebookLogin.createApplication(null);
            // String
            // location="https://www.facebook.com/connect/login_success.html#access_token=EAAFB3wlrtLkBAL9e3sdZCDCcAoq48kojNzhPGVk71OAqW7KvBbhZBqV1cklixm6ciNz3cgSgHtb3t8PrwTjKAMdZBCbOWqWaPpxgkQfdUGG1rVKaCBl00ERWsqdmA4ClLSHRXuLPp0P2yIO6PO4ynG7jeTkjqwZD&expires_in=5183999";
            // String location =
            // "https://www.facebook.com/connect/login_success.html#access_token=EAAFB3wlrtLkBAFYC7kCDPwaSkAkLv20zNsfbhevoKDY2JZCX98x5clQyLPMaSe5sVxdYlRCZC91wcrXU4dySihhWtoO3ou0AAI7kqZBpDenvwrk3JZCju8nhXYeQaiiUZA1IiY2ZC0AOJ4SgG7NcgbGl8M637JlC4ZD&expires_in=5135285";
            // ClientThread handlelogin = new ClientThread(location);
            // handlelogin.start();
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.out);
        } catch (IOException e) {
            System.out.println("Could Not connect to Chat Server");
            System.out.println("Communication Error: " + e.getMessage());
            // e.printStackTrace();
        }
        // System.out.println("Main Thread Done");
    }

    private static void pressAnyKeyToContinue(
            MessageSendThread messageSendThread) {
        try {

            System.out.println("Press any key to login using facebook..");
            messageSendThread.cmdin.nextLine();

        } catch (Exception e) {
            System.out.println("IO Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
