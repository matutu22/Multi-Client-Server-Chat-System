package au.edu.unimelb.tcp.server.moses;

public class ServerInfo {
    private String serverId;
    private String serverAddress;
    private int clientsPort;
    private int coordinationPort;
    private boolean status;
    
    public ServerInfo(String serverId, String serverAddress, int clientsPort, int coordinationPort, boolean status) {
        this.serverId = serverId;
        this.serverAddress = serverAddress;
        this.clientsPort = clientsPort;
        this.coordinationPort = coordinationPort;
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getClientsPort() {
        return clientsPort;
    }

    public int getCoordinationPort() {
        return coordinationPort;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String toString()
    {
        return "serverId:"+serverId+" serverAddress:"+serverAddress+" clientsPort:"+clientsPort+" coordinationPort:"+ coordinationPort + " status:"+ status;
    }

}
