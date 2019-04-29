package hufs.cws.network;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class ChatClientServer 
{
	    public static void main(String[] args)
	    {
		    	ServerSocket server;
		    	Socket sock;
		    	List<ObjectOutputStream> listWriters = new ArrayList<ObjectOutputStream>();
		    	
		        try
		        {
		               server = new ServerSocket(10001);
		               
		               while(true)
		               {
		            	       sock = server.accept();
				               new ChatClientServerThread(sock, listWriters).start();
				       }
		        } 
		        catch(IOException e)
		        {
		               System.out.println(e);
		        }
	    }
}