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

import enums.FileNames;
import enums.MsgKeys;

/**
 * This Class handles request from ChatClient
 * 
 * @author wschoi8640
 * @version 1.0
 */
public class ChatClientServerThread extends Thread {
	private Socket sock;
	private File friendFile;
	private File userFile;
	private File tempFile;
	private File loginFile;
	private File chatroomFile;
	private List<String> friendList;
	private List<String> chatroomList;
	private List<String> message;
	private OutputStream out;
	private InputStream in;
	private PrintWriter messageWriter;
	private String status;
	private ObjectInputStream messageListReader;
	private ObjectOutputStream messageListWriter;
	private BufferedReader chatroomFileReader;
	private BufferedReader userFileReader;
	private BufferedReader friendFileReader;
	private BufferedReader tempFileReader;
	private BufferedReader loginFileReader;
	private BufferedWriter chatroomFileOutput;
	private String userName = null;

	public ChatClientServerThread(Socket sock) {
		this.sock = sock;

		try {
			out = sock.getOutputStream();
			in = sock.getInputStream();
			messageWriter = new PrintWriter(new OutputStreamWriter(out));
			messageListReader = new ObjectInputStream(in);
			messageListWriter = new ObjectOutputStream(out);

			// initialize LoginData.txt when server Starts
			loginFile = new File(FileNames.LoginFile.getName());
			loginFile.delete();
			loginFile = new File(FileNames.LoginFile.getName());
			loginFile.createNewFile();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			while (true) {

				try {
					userFile = new File(FileNames.UserFile.getName());
					friendFile = new File(FileNames.FriendFile.getName());
					tempFile = new File(FileNames.TempFile.getName());
					if (!userFile.exists())
						userFile.createNewFile();
					if (!friendFile.exists())
						friendFile.createNewFile();
					if (!loginFile.exists())
						loginFile.createNewFile();
					tempFile.createNewFile();
					loginFileReader = new BufferedReader(new FileReader(FileNames.LoginFile.getName()));
					userFileReader = new BufferedReader(new FileReader(FileNames.UserFile.getName()));
					friendFileReader = new BufferedReader(new FileReader(FileNames.FriendFile.getName()));
					message = (ArrayList<String>) messageListReader.readObject();
				} catch (ClassNotFoundException e) {
					break;
				} catch (EOFException e) {
					break;
				}

				// Default status(no-id)
				status = "no-match";

				// Login size : 2, Join size : 3
				if (message.get(0).equals(MsgKeys.LoginRequest.getKey())) {
					String line;
					String[] userInfo = new String[message.size()];

					// compare UserData with Requested Info
					while ((line = userFileReader.readLine()) != null) {
						userInfo = line.split(" ");

						if (userInfo[1].equals(message.get(1))) {
							if (userInfo[2].equals(message.get(2))) {
								status = "id-pw-match"; // Change status(login-success)
								messageWriter.println("hello_" + userInfo[0]);
								messageWriter.flush();
								userName = userInfo[0];
								BufferedWriter loginFileWriter = new BufferedWriter(
										new FileWriter(FileNames.LoginFile.getName(), true));

								// 로그인 상태를 유저 이름의 갯수로 파악할 것이므로 유저 이름을 하나 추가
								loginFileWriter.write(userInfo[0]);
								loginFileWriter.newLine();
								loginFileWriter.flush();
								loginFileWriter.close();
								break;
							}
							status = "id-match"; // Change status(wrong-password)
							messageWriter.println(MsgKeys.LoginFailByPW.getKey());
							messageWriter.flush();
							break;
						}

					}
					// No id match
					if (status.equals("no-match")) {
						messageWriter.println(MsgKeys.LoginFailByID.getKey());
						messageWriter.flush();
					}
					continue;
				}

				else if (message.get(0).equals(MsgKeys.JoinRequest.getKey())) {
					// Join Request
					String userFileline;
					String userName = message.get(2);
					boolean alreadyexists = false; // if true can't join
					String[] userFileData = new String[message.size()];
					while ((userFileline = userFileReader.readLine()) != null) {
						userFileData = userFileline.split(" ");

						if (userFileData[1].equals(userName)) {
							// Already exists
							messageWriter.println(MsgKeys.JoinFail.getKey());
							messageWriter.flush();
							alreadyexists = true;
							break;
						}
					}
					if (alreadyexists) {
						continue;
					}
					// If not exist allow Join
					BufferedWriter txtOutput = new BufferedWriter(new FileWriter(FileNames.UserFile.getName(), true));
					for (int i = 1; i < message.size(); i++) {
						txtOutput.write(message.get(i));
						if (i == message.size() - 1) break;
						txtOutput.write(" ");
					}
					txtOutput.newLine();
					txtOutput.flush();
					txtOutput.close();

					// 회원 가입 성공
					messageWriter.println(MsgKeys.JoinSuccess.getKey());
					messageWriter.flush();
				} else if (message.get(0).equals(MsgKeys.ReceiveFriendsRequest.getKey())) {
					String line;
					String userName = message.get(1);
					String[] friendInfo = new String[2];
					friendList = new ArrayList<String>();
					friendList.add("message");

					// compare FriendData with Requested Info
					while ((line = friendFileReader.readLine()) != null) {
						friendInfo = line.split(" ");
						if (friendInfo[0].equals(userName)) {
							friendList.add(friendInfo[1]);
						}
					}
					if (friendList.size() > 1) {
						friendList.set(0, MsgKeys.ReceiveSuccess.getKey());
						messageListWriter.writeObject(friendList);
						messageListWriter.flush();
						messageListWriter.reset();
					}
				} else if (message.get(0).equals(MsgKeys.FriendAddRequest.getKey())) {
					String line;
					String userName = message.get(1);
					String friendName = message.get(2);
					String[] friendInfo = new String[2];
					String[] userInfo = new String[3];
					boolean alreadyexists = false;
					boolean nosuchuser = true;

					// 친구가 존재하는지 확인
					while ((line = userFileReader.readLine()) != null) {
						userInfo = line.split(" ");
						if (userInfo[1].equals(friendName)) {
							nosuchuser = false;
							friendName = userInfo[0];
							break;
						}
					}

					// 존재하지 않는 경우 응답
					if (nosuchuser) {
						messageWriter.println(MsgKeys.FriendAddFailByID.getKey());
						messageWriter.flush();
						continue;

					}

					// 이미 친구인지 확인
					while ((line = friendFileReader.readLine()) != null) {
						friendInfo = line.split(" ");
						if (friendInfo[0].equals(userName) && friendInfo[1].equals(friendName)) {
							alreadyexists = true;
							break;
						}
					}

					// 이미 친구인 경우 응답
					if (alreadyexists) {

						messageWriter.println(MsgKeys.FriendAddFailByDupli.getKey());
						messageWriter.flush();
						continue;
					} else {
						// 친구 추가에 성공한 경우
						// 친구 파일에 해당 내용 추가하고 클라이언트에 응답
						BufferedWriter txtOutput = new BufferedWriter(
								new FileWriter(FileNames.FriendFile.getName(), true));
						txtOutput.write(userName);
						txtOutput.write(" ");
						txtOutput.write(friendName);
						txtOutput.newLine();
						txtOutput.flush();
						txtOutput.close();

						messageWriter.println("add_" + friendName);
						messageWriter.flush();
					}

				} else if (message.get(0).equals(MsgKeys.RemoveRequest.getKey())) {
					String line;
					String userName = message.get(1);
					List<String> rmvFriendsList = new ArrayList<String>();
					String[] friendInfo = new String[2];
					BufferedWriter txtOutput = new BufferedWriter(new FileWriter(FileNames.TempFile.getName()));

					// 삭제할 친구 리스트를 가져옴
					for (int i = 2; i < message.size(); i++) {
						rmvFriendsList.add(message.get(i));
					}

					// 친구 관계인지 확인 후, 임시 파일에 제외하고 남은 친구들만 추가
					while ((line = friendFileReader.readLine()) != null) {
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
					tempFileReader = new BufferedReader(new FileReader(FileNames.TempFile.getName()));
					BufferedWriter tmpOutput = new BufferedWriter(new FileWriter(FileNames.FriendFile.getName()));
					String tmp_line;
					while ((tmp_line = tempFileReader.readLine()) != null) {
						tmpOutput.write(tmp_line);
						tmpOutput.newLine();
						tmpOutput.flush();
					}
					tmpOutput.close();

					messageWriter.println(MsgKeys.RemoveSuccess.getKey());
					messageWriter.flush();
				} else if (message.get(0).equals(MsgKeys.RefreshRequest.getKey())) {
					// 로그인 상황 파일을 가져옴
					loginFileReader = new BufferedReader(new FileReader(FileNames.LoginFile.getName()));
					HashMap<String, Integer> countUser = new HashMap<String, Integer>();
					List<String> updateList = new ArrayList<String>();
					updateList.add(MsgKeys.RefreshSuccess.getKey());
					String cur_line = "";

					// 해당 유저의 이름이 없으면 추가하고 있으면 갯수를 셈
					while ((cur_line = loginFileReader.readLine()) != null) {
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
					messageListWriter.writeObject(updateList);
					messageListWriter.flush();
					messageListWriter.reset();
				} else if (message.get(0).equals(MsgKeys.LogoutRequest.getKey())) {
					// 로그아웃시 로그인 상태 파일에 유저 이름을 추가해준다.
					BufferedWriter loginOutput = new BufferedWriter(
							new FileWriter(FileNames.LoginFile.getName(), true));
					if (userName != null) {
						loginOutput.write(userName);
						loginOutput.newLine();
						loginOutput.flush();
						userName = null;
					}
					loginOutput.close();
				} else if (message.get(0).equals(MsgKeys.ChatroomAddRequest.getKey())) {
					// 유저별로 채팅방 목록을 따로 관리한다.
					String userChatroom = message.get(1) + "_chatroom.txt";

					chatroomFile = new File(userChatroom);
					if (!chatroomFile.exists())
						chatroomFile.createNewFile();
					chatroomFileReader = new BufferedReader(new FileReader(userChatroom));
					chatroomFileOutput = new BufferedWriter(new FileWriter(userChatroom, true));

					String line;
					int count = 0;
					boolean hasSameChatroom = false;
					String[] messageInfos = message.get(2).split(", ");
					String[] chatroomInfos = new String[10];

					// 이미 채팅방이 존재하는지 확인한다.
					// 전송해온 채팅방과 서버에 저장된 채팅방을 비교한다.
					while ((line = chatroomFileReader.readLine()) != null) {
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
						messageWriter.println(MsgKeys.ChatroomAddFailByDupli.getKey());
						messageWriter.flush();
						continue;
					}
					// 채팅방이 추가되었음을 클라이언트에 알림
					else {
						chatroomFileOutput.write(message.get(2));
						chatroomFileOutput.newLine();
						chatroomFileOutput.flush();

						messageWriter.println(MsgKeys.ChatroomAddSuccess.getKey());
						messageWriter.flush();
						message.clear();
						chatroomFileOutput.close();
					}
				} else if (message.get(0).equals(MsgKeys.ReceiveChatroomsRequest.getKey())) {
					String userName = message.get(1);
					String userChatroom = userName + "_chatroom.txt";

					chatroomFile = new File(userChatroom);
					if (!chatroomFile.exists())
						chatroomFile.createNewFile();
					chatroomFileReader = new BufferedReader(new FileReader(userChatroom));

					String line;
					String chatroomInfo = "";

					chatroomList = new ArrayList<String>();
					chatroomList.add("message");

					// 해당 유저의 채팅방 리스트를 가져와 리스트에 저장한다.
					while ((line = chatroomFileReader.readLine()) != null) {
						chatroomInfo = line;
						chatroomList.add(chatroomInfo);
					}

					// 채팅방이 존재하면 클라이언트로 전송한다.
					if (chatroomList.size() > 1) {
						chatroomList.set(0, MsgKeys.ReceiveSuccess.getKey());
						messageListWriter.writeObject(chatroomList);
						messageListWriter.flush();
						messageListWriter.reset();
					}
				} else if (message.get(0).equals(MsgKeys.ChatroomRemoveRequest.getKey())) {
					String line;
					String userName = message.get(1);
					String userChatroom = userName + "_chatroom.txt";
					List<String> rmvChatroomsList = new ArrayList<String>();
					String[] chatroomsInfo = new String[10];
					String[] rmvChatInfos = new String[10];

					BufferedWriter txtOutput = new BufferedWriter(new FileWriter(FileNames.TempFile.getName()));
					chatroomFileReader = new BufferedReader(new FileReader(userChatroom));

					// 삭제할 채팅방 목록을 가져온다
					for (int i = 2; i < message.size(); i++) {
						rmvChatroomsList.add(message.get(i));
					}
					while ((line = chatroomFileReader.readLine()) != null) {
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
					tempFileReader = new BufferedReader(new FileReader(FileNames.TempFile.getName()));
					BufferedWriter tmpOutput = new BufferedWriter(new FileWriter(userChatroom));
					String tmp_line;
					while ((tmp_line = tempFileReader.readLine()) != null) {
						tmpOutput.write(tmp_line);
						tmpOutput.newLine();
						tmpOutput.flush();
					}
					tmpOutput.close();

					messageWriter.println(MsgKeys.RemoveSuccess.getKey());
					messageWriter.flush();
				}
			}
		} catch (IOException e) {
			// do nothing
		}
	}

}