package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import enums.MsgKeys;

/**
 *  This Class handles request from ChatClient
 *  
 *  @author wschoi8640
 *  @version 1.0
 */
public class ChatClientServerThread extends Thread {
	private Socket sock;
	private File newFriendData;
	private File newUserData;
	private File newTempData;
	private File newLoginData;
	private File cData;
	private List<String> friendList;
	private List<String> chatroomList;
	private HashMap<String, Boolean> loginState = new HashMap<String, Boolean>();
	private List<String> message;
	private OutputStream out;
	private InputStream in;
	private PrintWriter messageSend;
	private int status;
	private ObjectInputStream messageListRcv;
	private ObjectOutputStream messageListSend;
	private BufferedReader chatroomData;
	private BufferedReader userData;
	private BufferedReader friendData;
	private BufferedReader tempData;
	private BufferedReader loginData;
	private BufferedWriter chatroomOutput;
	private String userName1 = null;

	public ChatClientServerThread(Socket sock) {
		this.sock = sock;

		try {
			out = sock.getOutputStream();
			in = sock.getInputStream();
			messageSend = new PrintWriter(new OutputStreamWriter(out));
			messageListRcv = new ObjectInputStream(in);
			messageListSend = new ObjectOutputStream(out);

			// initialize LoginData.txt when server Starts
			newLoginData = new File("LoginData.txt");
			newLoginData.delete();
			newLoginData = new File("LoginData.txt");
			newLoginData.createNewFile();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			while (true) {

				try {
					newUserData = new File("UserData.txt");
					newFriendData = new File("FriendData.txt");
					newTempData = new File("TempData.txt");
					if (!newUserData.exists())
						newUserData.createNewFile();
					if (!newFriendData.exists())
						newFriendData.createNewFile();
					if (!newLoginData.exists())
						newLoginData.createNewFile();
					newTempData.createNewFile();
					loginData = new BufferedReader(new FileReader("LoginData.txt"));
					userData = new BufferedReader(new FileReader("UserData.txt"));
					friendData = new BufferedReader(new FileReader("FriendData.txt"));
					message = (ArrayList<String>) messageListRcv.readObject();
				} catch (ClassNotFoundException e) {
					break;
				} catch (EOFException e) {
					break;
				}

				// Default status(no-id)
				status = 0;

				// Login size : 2, Join size : 3
				if (message.get(0).equals(MsgKeys.LoginRequest.getKey())) {
					String line;
					String[] userInfo = new String[message.size()];

					// compare UserData with Requested Info
					while ((line = userData.readLine()) != null) {
						userInfo = line.split(" ");

						if (userInfo[1].equals(message.get(1))) {
							if (userInfo[2].equals(message.get(2))) {
								status = 2; // Change status(login-success)
								messageSend.println("hello_" + userInfo[0]);
								messageSend.flush();
								userName1 = userInfo[0];
								BufferedWriter loginOutput = new BufferedWriter(new FileWriter("LoginData.txt", true));

								// �α��� ���¸� ���� �̸��� ������ �ľ��� ���̹Ƿ� ���� �̸��� �ϳ� �߰�
								loginOutput.write(userInfo[0]);
								loginOutput.newLine();
								loginOutput.flush();
								loginOutput.close();
								break;
							}
							status = 1; // Change status(wrong-password)
							messageSend.println(MsgKeys.LoginFailByPW.getKey());
							messageSend.flush();
							break;
						}

					}
					// No id match
					if ((status != 1) && (status != 2)) {
						status = 0;
						messageSend.println(MsgKeys.LoginFailByID.getKey());
						messageSend.flush();
					}

					continue;
				}

				else if (message.get(0).equals(MsgKeys.JoinRequest.getKey())) {
					// Join Request
					String line;
					boolean alreadyexists = false; // if true can't join
					String[] userInfo = new String[message.size()];
					while ((line = userData.readLine()) != null) {
						userInfo = line.split(" ");

						if (userInfo[1].equals(message.get(2))) {
							// Already exists
							messageSend.println(MsgKeys.JoinFail.getKey());
							messageSend.flush();

							alreadyexists = true;
							break;
						}
					}
					if (alreadyexists) {
						continue;
					}

					// If not exist allow Join
					BufferedWriter txtOutput = new BufferedWriter(new FileWriter("UserData.txt", true));

					for (int i = 1; i < message.size(); i++) {
						txtOutput.write(message.get(i));
						if (i == message.size() - 1)
							break;
						txtOutput.write(" ");
					}
					txtOutput.newLine();
					txtOutput.flush();
					txtOutput.close();

					// ȸ�� ���� ����
					loginState.put(message.get(2), false);
					messageSend.println(MsgKeys.JoinSuccess.getKey());
					messageSend.flush();
				} else if (message.get(0).equals("rcv_friends")) {
					String line;
					String userName = message.get(1);
					String[] friendInfo = new String[2];
					friendList = new ArrayList<String>();
					friendList.add("message");

					// compare FriendData with Requested Info
					while ((line = friendData.readLine()) != null) {
						friendInfo = line.split(" ");
						if (friendInfo[0].equals(userName)) {
							friendList.add(friendInfo[1]);
						}
					}
					if (friendList.size() > 1) {
						friendList.set(0, "rcv_ok");
						messageListSend.writeObject(friendList);
						messageListSend.flush();
						messageListSend.reset();
					} else if (friendList.size() == 1) {
						friendList.set(0, "no_friends");
						messageListSend.writeObject(friendList);
						messageListSend.flush();
						messageListSend.reset();
					}
				} else if (message.get(0).equals("add_friend")) {
					String line;
					String userName = message.get(1);
					String friendName = message.get(2);
					String[] friendInfo = new String[2];
					String[] userInfo = new String[3];
					boolean alreadyexists = false;
					boolean nosuchuser = true;

					// ģ���� �����ϴ��� Ȯ��
					while ((line = userData.readLine()) != null) {
						userInfo = line.split(" ");
						if (userInfo[1].equals(friendName)) {
							nosuchuser = false;
							friendName = userInfo[0];
							break;
						}
					}

					// �������� �ʴ� ��� ����
					if (nosuchuser) {
						messageSend.println("no_such_user");
						messageSend.flush();
						continue;

					}

					// �̹� ģ������ Ȯ��
					while ((line = friendData.readLine()) != null) {
						friendInfo = line.split(" ");
						if (friendInfo[0].equals(userName) && friendInfo[1].equals(friendName)) {
							alreadyexists = true;
							break;
						}
					}

					// �̹� ģ���� ��� ����
					if (alreadyexists) {

						messageSend.println("friend_exists");
						messageSend.flush();
						continue;
					} else {
						// ģ�� �߰��� ������ ���
						// ģ�� ���Ͽ� �ش� ���� �߰��ϰ� Ŭ���̾�Ʈ�� ����
						BufferedWriter txtOutput = new BufferedWriter(new FileWriter("FriendData.txt", true));
						txtOutput.write(userName);
						txtOutput.write(" ");
						txtOutput.write(friendName);
						txtOutput.newLine();
						txtOutput.flush();
						txtOutput.close();

						messageSend.println("add_" + friendName);
						messageSend.flush();
					}

				} else if (message.get(0).equals("rmv_friend")) {
					String line;
					String userName = message.get(1);
					List<String> rmvFriendsList = new ArrayList<String>();
					String[] friendInfo = new String[2];
					BufferedWriter txtOutput = new BufferedWriter(new FileWriter("TempData.txt"));

					// ������ ģ�� ����Ʈ�� ������
					for (int i = 2; i < message.size(); i++) {
						rmvFriendsList.add(message.get(i));
					}

					// ģ�� �������� Ȯ�� ��, �ӽ� ���Ͽ� �����ϰ� ���� ģ���鸸 �߰�
					while ((line = friendData.readLine()) != null) {
						friendInfo = line.split(" ");
						if (friendInfo[0].equals(userName)) {
							if (!rmvFriendsList.contains(friendInfo[1])) {
								txtOutput.write(friendInfo[0]);
								txtOutput.write(" ");
								txtOutput.write(friendInfo[1]);
								txtOutput.newLine();
							}
						} else {
							txtOutput.write(friendInfo[0]);
							txtOutput.write(" ");
							txtOutput.write(friendInfo[1]);
							txtOutput.newLine();
						}
						txtOutput.flush();

					}
					txtOutput.close();

					// �ӽ������� ������ ģ�� ���� ���Ϸ� ����
					tempData = new BufferedReader(new FileReader("TempData.txt"));
					BufferedWriter tmpOutput = new BufferedWriter(new FileWriter("FriendData.txt"));
					String tmp_line;
					while ((tmp_line = tempData.readLine()) != null) {
						tmpOutput.write(tmp_line);
						tmpOutput.newLine();
						tmpOutput.flush();
					}
					tmpOutput.close();

					messageSend.println("rmv_ok");
					messageSend.flush();
				} else if (message.get(0).equals("do_refresh")) {
					// �α��� ��Ȳ ������ ������
					loginData = new BufferedReader(new FileReader("LoginData.txt"));
					HashMap<String, Integer> countUser = new HashMap<String, Integer>();
					List<String> updateList = new ArrayList<String>();
					updateList.add("refresh_ok");
					String cur_line = "";

					// �ش� ������ �̸��� ������ �߰��ϰ� ������ ������ ��
					while ((cur_line = loginData.readLine()) != null) {
						if (countUser.get(cur_line) == null) {
							countUser.put(cur_line, 1);
						} else {
							countUser.put(cur_line, countUser.get(cur_line) + 1);
						}
					}

					// ������ �̸��� Ȧ���� ��������� �������̶�� �ǹ�, ���Ÿ���Ʈ�� �߰�����
					countUser.forEach((user1, count) -> {
						if (count % 2 == 1) {
							updateList.add(user1);
						}
					});

					// ���Ÿ���Ʈ ����
					messageListSend.writeObject(updateList);
					messageListSend.flush();
					messageListSend.reset();
				} else if (message.get(0).equals("do_logout")) {
					// �α׾ƿ��� �α��� ���� ���Ͽ� ���� �̸��� �߰����ش�.
					BufferedWriter loginOutput = new BufferedWriter(new FileWriter("LoginData.txt", true));
					if (userName1 != null) {
						loginOutput.write(userName1);
						loginOutput.newLine();
						loginOutput.flush();
						userName1 = null;
					}
					loginOutput.close();
				} else if (message.get(0).equals("add_chatroom")) {
					// �������� ä�ù� ����� ���� �����Ѵ�.
					String userChatroom = message.get(1) + "_chatroom.txt";

					cData = new File(userChatroom);
					if (!cData.exists())
						cData.createNewFile();
					chatroomData = new BufferedReader(new FileReader(userChatroom));
					chatroomOutput = new BufferedWriter(new FileWriter(userChatroom, true));

					String line;
					int count = 0;
					boolean hasSameChatroom = false;
					String[] messageInfos = message.get(2).split(", ");
					String[] chatroomInfos = new String[10];

					// �̹� ä�ù��� �����ϴ��� Ȯ���Ѵ�.
					// �����ؿ� ä�ù�� ������ ����� ä�ù��� ���Ѵ�.
					while ((line = chatroomData.readLine()) != null) {
						chatroomInfos = line.split(", ");
						for (String messageInfo : messageInfos) {
							for (String chatroomInfo : chatroomInfos) {
								if (chatroomInfo.equals(messageInfo)) {
									count++;
								}
							}
						}

						if (count == messageInfos.length && count == chatroomInfos.length) {
							hasSameChatroom = true;
							break;
						}
						count = 0;
					}

					// ä�ù��� �̹� ������ ��� Ŭ���̾�Ʈ�� �˸�
					if (hasSameChatroom == true) {
						messageSend.println("chatroom_exists");
						messageSend.flush();
						continue;
					}
					// ä�ù��� �߰��Ǿ����� Ŭ���̾�Ʈ�� �˸�
					else {
						chatroomOutput.write(message.get(2));
						chatroomOutput.newLine();
						chatroomOutput.flush();

						messageSend.println("chatroom_added");
						messageSend.flush();
						message.clear();
						chatroomOutput.close();
					}
				} else if (message.get(0).equals("rcv_chatrooms")) {
					String userName = message.get(1);
					String userChatroom = userName + "_chatroom.txt";

					cData = new File(userChatroom);
					if (!cData.exists())
						cData.createNewFile();
					chatroomData = new BufferedReader(new FileReader(userChatroom));

					String line;
					String chatroomInfo = "";

					chatroomList = new ArrayList<String>();
					chatroomList.add("message");

					// �ش� ������ ä�ù� ����Ʈ�� ������ ����Ʈ�� �����Ѵ�.
					while ((line = chatroomData.readLine()) != null) {
						chatroomInfo = line;
						chatroomList.add(chatroomInfo);
					}

					// ä�ù��� �����ϸ� Ŭ���̾�Ʈ�� �����Ѵ�.
					if (chatroomList.size() > 1) {
						chatroomList.set(0, "rcv_ok");
						messageListSend.writeObject(chatroomList);
						messageListSend.flush();
						messageListSend.reset();
					}

					// �������� �ʴ� ��� �˷��ش�
					else if (chatroomList.size() == 1) {
						chatroomList.set(0, "no_chatrooms");
						messageListSend.writeObject(chatroomList);
						messageListSend.flush();
						messageListSend.reset();
					}
				} else if (message.get(0).equals("rmv_chatroom")) {
					String line;
					String userName = message.get(1);
					String userChatroom = userName + "_chatroom.txt";
					List<String> rmvChatroomsList = new ArrayList<String>();
					String[] chatroomsInfo = new String[10];
					String[] rmvChatInfos = new String[10];

					BufferedWriter txtOutput = new BufferedWriter(new FileWriter("TempData.txt"));
					chatroomData = new BufferedReader(new FileReader(userChatroom));

					// ������ ä�ù� ����� �����´�
					for (int i = 2; i < message.size(); i++) {
						rmvChatroomsList.add(message.get(i));
					}
					while ((line = chatroomData.readLine()) != null) {
						chatroomsInfo = line.split(", ");
						// �ش� ä�ù��� �����ϴ��� Ȯ�� �� ��, �̸� ������ ä�ù���� �ӽ����Ͽ� �����Ѵ�
						for (String rmvChatroom : rmvChatroomsList) {
							int count = 0;
							rmvChatInfos = rmvChatroom.split(", ");
							for (String chatroomInfo : chatroomsInfo) {
								for (String rmvChatInfo : rmvChatInfos) {
									if (chatroomInfo.equals(rmvChatInfo)) {
										count++;
									}
								}
							}

							if (count != chatroomsInfo.length || count != rmvChatInfos.length) {
								for (String chatroomInfo : chatroomsInfo) {
									txtOutput.write(chatroomInfo);
									if (!chatroomInfo.equals(chatroomsInfo[chatroomsInfo.length - 1]))
										txtOutput.write(", ");
								}
								txtOutput.newLine();
							}
							txtOutput.flush();
						}
					}
					txtOutput.close();

					// �ӽ� ������ ������ ä�ù� ������ �����Ѵ�.
					tempData = new BufferedReader(new FileReader("TempData.txt"));
					BufferedWriter tmpOutput = new BufferedWriter(new FileWriter(userChatroom));
					String tmp_line;
					while ((tmp_line = tempData.readLine()) != null) {
						tmpOutput.write(tmp_line);
						tmpOutput.newLine();
						tmpOutput.flush();
					}
					tmpOutput.close();

					messageSend.println("rmv_ok");
					messageSend.flush();
				}
			}
		} catch (IOException e) {
			// do nothing
		}
	}

}