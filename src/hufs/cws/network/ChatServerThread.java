package hufs.cws.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 *  ä�ù�� ����Ǵ� ����, ä�� ���� ����, ������ ����Ѵ�.
 */
public class ChatServerThread extends Thread{
	private Socket sock;
	private List<ObjectOutputStream> listWriters;
	private File newChatroomFile;
	private BufferedReader newChatroomData;
	private BufferedWriter newChatroomOutput;

	private List<String> messageList;
	
	public ChatServerThread(Socket sock, List<ObjectOutputStream> listWriters)
	{
			this.sock = sock;
			this.listWriters = listWriters;
	}
	
	@Override
	public void run() 
	{
			try
			{
					  OutputStream out = sock.getOutputStream();
				      InputStream in = sock.getInputStream();
				      
				      ObjectOutputStream messageOutput = new ObjectOutputStream(out);
				      ObjectInputStream messageInput = new ObjectInputStream(in);

				      // ������ �����忡 �޽����� ������ ���� ����ȭ ó���� �Ѵ�
				      synchronized(listWriters)
			    	  {
				    	  	 listWriters.add(messageOutput);
			    	  }	
				      
				      try 
				      {
				    	  		while(true)
				    	  		{
										messageList = (ArrayList<String>)messageInput.readObject();
										if(messageList != null)
										{
												// ����ڸ��� ���� �ٸ� ä�ù� �̸��� ���ϱ� ���� ������ ó���� ���ش�.
												// ����� �̸��� ä�ù��� ��ģ �� �̸� �迭�� ������, ������������ �����Ѵ�
												List<String> tempList = new ArrayList<String>();
												String temp = messageList.get(1) + ", " + messageList.get(2);
												String temp1 = "";
												
												String [] temp_arr = temp.split(", ");
												Arrays.sort(temp_arr, String.CASE_INSENSITIVE_ORDER);
												for(String name : temp_arr)
												{
														temp1 = temp1 + name;
														if(!name.equals(temp_arr[temp_arr.length- 1])) temp1 = temp1 + "_";
												}
												temp1 = temp1 + ".txt";
												
												// �ش� ä�ù��� ä�ó����� ������ ������ �����.
												newChatroomFile = new File(temp1);
												if(!newChatroomFile.exists()) newChatroomFile.createNewFile();
												newChatroomOutput = new BufferedWriter(new FileWriter(temp1, true));

												// ä������ ��û�� �� ��� ���Ͽ� �ۼ��ϰ� �� ä�� ������� �޽����� �����Ѵ�.
												if(messageList.get(0).equals("send_chatData"))
												{
														newChatroomOutput.write("[" + messageList.get(1) + "] : " + messageList.get(3));
														newChatroomOutput.newLine();
														newChatroomOutput.flush();
											            sendMessage(messageList);
												}
	
												// ������ ä�� �޾ƿ��� ��û�� �� ��� �ش� ������ ������ ����Ʈ�� �������� �� ä�� ������� �����Ѵ�.
												if(messageList.get(0).equals("rcv_chatData"))
												{
														String tmp_line;
														tempList.clear();
														tempList.add("rcv_chatData");
														tempList.add(messageList.get(1));
														tempList.add(messageList.get(2));
														newChatroomData = new BufferedReader(new FileReader(temp1));
														while((tmp_line = newChatroomData.readLine()) != null)
														{
																tempList.add(tmp_line + "\n");
														}
														if(tempList.size() > 0)
														{	
																sendMessage(tempList);
														}
												}
									            messageInput = new ObjectInputStream(in);
										}
				    	  		}
				      } 
				      catch (ClassNotFoundException e) 
				      {
				      
				      }
				      messageOutput.close();
				      messageInput.close();
				      sock.close();
			  }
			  catch(IOException e)
			  {
			  }
	}
	
	private void sendMessage(List<String> messages)
	{
			// ����� �� ä�� ������� �޽����� �������ش�.
			synchronized(listWriters) 
			{
					for(ObjectOutputStream writer : listWriters)
					{
							try {
								writer.writeObject(messages);
								writer.flush();
							} catch (IOException e) 
							{
								e.printStackTrace();
							}
							
					}
			}
	}

}
