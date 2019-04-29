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
 *  채팅방과 연결되는 서버, 채팅 내용 전송, 저장을 담당한다.
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

				      // 각각의 쓰레드에 메시지를 보내기 위해 동기화 처리를 한다
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
												// 사용자마다 각각 다른 채팅방 이름을 비교하기 위해 데이터 처리를 해준다.
												// 사용자 이름과 채팅방을 합친 후 이를 배열로 나누고, 오름차순으로 정리한다
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
												
												// 해당 채팅방의 채팅내용을 저장할 파일을 만든다.
												newChatroomFile = new File(temp1);
												if(!newChatroomFile.exists()) newChatroomFile.createNewFile();
												newChatroomOutput = new BufferedWriter(new FileWriter(temp1, true));

												// 채팅전송 요청이 올 경우 파일에 작성하고 각 채팅 쓰레드로 메시지를 전송한다.
												if(messageList.get(0).equals("send_chatData"))
												{
														newChatroomOutput.write("[" + messageList.get(1) + "] : " + messageList.get(3));
														newChatroomOutput.newLine();
														newChatroomOutput.flush();
											            sendMessage(messageList);
												}
	
												// 과거의 채팅 받아오기 요청이 올 경우 해당 파일의 내용을 리스트에 저장한후 각 채팅 쓰레드로 전송한다.
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
			// 연결된 각 채팅 쓰레드로 메시지를 전달해준다.
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
