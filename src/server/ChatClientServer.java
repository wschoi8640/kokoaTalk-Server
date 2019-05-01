package server;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;
/**
 * This Class initiates ChatClientServer
 * 
 * @author wschoi8640
 * @version 1.0
 */
public class ChatClientServer {
	public static void main(String[] args) {
		ServerSocket server;
		Socket sock;
		
		try {
			server = new ServerSocket(10001);

			while (true) {
				sock = server.accept();
				new ChatClientServerThread(sock).start();
			}
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}