package au.edu.unimelb.tcp.server.moses;


import org.kohsuke.args4j.Option;

public class ComLineValues
{
  @Option(required=true, name="-n", aliases={"--serverid"}, usage="ID of Server present in servers_conf file")
  private String serverId;
  @Option(required=true, name="-l", aliases={"--servers_conf"}, usage="Configuration file of server")
  
  private String serverConfig;
  @Option(required=false, name="-i", aliases={"--server IP"}, usage="IP address of new server")
  private String serverIP;

  @Option(required=false, name="-p1", aliases={"--server-client port"}, usage="port number for client communication")
  private String clientport;

  
  @Option(required=false, name="-p2", aliases={"--servers communication port"}, usage="port number for server communication")
  private String serverport;

  
  public String getServerId()
  {
    return this.serverId;
  }
  
  public String getServerConfig()
  {
    return this.serverConfig;
  }
  public String getserverIP()
  {
    return this.serverIP;
  }
  
  public String getclientport()
  {
    return this.clientport;
  }
  
  public String getserverport()
  {
    return this.serverport;
  }
 
  
}
