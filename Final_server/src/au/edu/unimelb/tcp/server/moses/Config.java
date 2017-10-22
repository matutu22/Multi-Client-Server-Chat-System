package au.edu.unimelb.tcp.server.moses;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;

public class Config {
    public static ServerInfo localConfig;
    public static ArrayList<ServerInfo> remoteConfig;

    public static void loadConfig(String configFilename, String localServerid)
            throws NumberFormatException, IOException {
        BufferedReader fileReader = new BufferedReader(
                new FileReader(configFilename));
        String configLine;
        remoteConfig = new ArrayList<ServerInfo>();
        try {

            while ((configLine = fileReader.readLine()) != null) {
                String[] configParams = configLine.split("\t");

                if (configParams.length == 4) {
                    ServerInfo server = new ServerInfo(configParams[0],
                            configParams[1], Integer.parseInt(configParams[2]),
                            Integer.parseInt(configParams[3]), false);
                    if (configParams[0].equals(localServerid)) {
                        localConfig = server;

                    } else
                        remoteConfig.add(server);
                } else
                    throw new InvalidPropertiesFormatException(
                            "Config File Format is Invalid");
            }
            /*
             * System.out.println("Local:" + localConfig + " Remote:"
             * + (ServerState.getInstance()).getRemoteConfig());
             */
        } finally {
            fileReader.close();
        }

    }

    public ServerInfo getLocalConfig() {
        return localConfig;
    }
}
