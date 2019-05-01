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

								// 로그인 상태를 유저 이름의 갯수로 파악할 것이므로 유저 이름을 하나 추가
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

					// 회원 가입 성공
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

					// 친구가 존재하는지 확인
					while ((line = userData.readLine()) != null) {
						userInfo = line.split(" ");
						if (userInfo[1].equals(friendName)) {
							nosuchuser = false;
							friendName = userInfo[0];
							break;
						}
					}

					// 존재하지 않는 경우 응답
					if (nosuchuser) {
						messageSend.println("no_such_user");
						messageSend.flush();
						continue;

					}

					// 이미 친구인지 확인
					while ((line = friendData.readLine()) != null) {
						friendInfo = line.split(" ");
						if (friendInfo[0].equals(userName) && friendInfo[1].equals(friendName)) {
							alreadyexists = true;
							break;
						}
					}

					// 이미 친구인 경우 응답
					if (alreadyexists) {

						messageSend.println("friend_exists");
						messageSend.flush();
						continue;
					} else {
						// 친구 추가에 성공한 경우
						// 친구 파일에 해당 내용 추가하고 클라이언트에 응답
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

					// 삭제할 친구 리스트를 가져옴
					for (int i = 2; i < message.size(); i++) {
						rmvFriendsList.add(message.get(i));
					}

					// 친구 관계인지 확인 후, 임시 파일에 제외하고 남은 친구들만 추가
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

					// 임시파일의 내용을 친구 관계 파일로 복사
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
					// 로그인 상황 파일을 가져옴
					loginData = new BufferedReader(new FileReader("LoginData.txt"));
					HashMap<String, Integer> countUser = new HashMap<String, Integer>();
					List<String> updateList = new ArrayList<String>();
					updateList.add("refresh_ok");
					String cur_line = "";

					// 해당 유저의 이름이 없으면 추가하고 있으면 갯수를 셈
					while ((cur_line = loginData.readLine()) != null) {
						if (countUser.get(cur_line) == null) {
							countUser.put(cur_line, 1);
						} else {
							countUser.put(cur_line, countUser.get(cur_line) + 1);
						}
					}

					// 유저의 이름이 홀수개 들어있으면 접속중이라는 의미, 갱신리스트에 추가해줌
					countUser.forEach((user1, count) -> {
						if (count % 2 == 1) {
							updateList.add(user1);
						}
					});

					// 갱신리스트 전송
					messageListSend.writeObject(updateList);
					messageListSend.flush();
					messageListSend.reset();
				} else if (message.get(0).equals("do_logout")) {
					// 로그아웃시 로그인 상태 파일에 유저 이름을 추가해준다.
					BufferedWriter loginOutput = new BufferedWriter(new FileWriter("LoginData.txt", true));
					if (userName1 != null) {
						loginOutput.write(userName1);
						loginOutput.newLine();
						loginOutput.flush();
						userName1 = null;
					}
					loginOutput.close();
				} else if (message.get(0).equals("add_chatroom")) {
					// 유저별로 채팅방 목록을 따로 관리한다.
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

					// 이미 채팅방이 존재하는지 확인한다.
					// 전송해온 채팅방과 서버에 저장된 채팅방을 비교한다.
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

					// 채팅방이 이미 존재할 경우 클라이언트에 알림
					if (hasSameChatroom == true) {
						messageSend.println("chatroom_exists");
						messageSend.flush();
						continue;
					}
					// 채팅방이 추가되었음을 클라이언트에 알림
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

					// 해당 유저의 채팅방 리스트를 가져와 리스트에 저장한다.
					while ((line = chatroomData.readLine()) != null) {
						chatroomInfo = line;
						chatroomList.add(chatroomInfo);
					}

					// 채팅방이 존재하면 클라이언트로 전송한다.
					if (chatroomList.size() > 1) {
						chatroomList.set(0, "rcv_ok");
						messageListSend.writeObject(chatroomList);
						messageListSend.flush();
						messageListSend.reset();
					}

					// 존재하지 않는 경우 알려준다
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

					// 삭제할 채팅방 목록을 가져온다
					for (int i = 2; i < message.size(); i++) {
						rmvChatroomsList.add(message.get(i));
					}
					while ((line = chatroomData.readLine()) != null) {
						chatroomsInfo = line.split(", ");
						// 해당 채팅방이 존재하는지 확인 한 후, 이를 제외한 채팅방들을 임시파일에 저장한다
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

					// 임시 파일의 내용을 채팅방 정보로 복사한다.
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