package hufs.cws.network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer 
{

		public static void main(String[] args) 
		{
		    	ServerSocket server;
		    	Socket sock;
		    	List<ObjectOutputStream> listWriters = new ArrayList<ObjectOutputStream>();
		    	
		        try
		        {
		               server = new ServerSocket(10002);
		               while(true)
		               {
		            	       sock = server.accept();
				               new ChatServerThread(sock, listWriters).start();
				       }
		        } 
		        catch(IOException e)
		        {
		               System.out.println(e);
		        }
		}

}
