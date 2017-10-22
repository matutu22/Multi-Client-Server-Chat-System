# Multi-Client-Server-Chat-System

Multiple Client and server chat system.  
Run on terminal.  
Scalability supported: server size able to scale.  
Connection in ssh secure HTTP.  
Facebook login function.   
Clients can connect to one server, create or join chat room, delete chat room as admin.  
Reconnect to other servers will delete current chat room.  

**How to run?**
`Run a chat client:`
  java -jar client.jar -h server_address [-p server_port] -i identity [-d]

`Run a chat server:`
  java -jar server.jar -n serverid -l servers_conf
