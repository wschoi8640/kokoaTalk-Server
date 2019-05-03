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
	private List<String> messageList;
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
					messageList = new ArrayList<String>();
				} catch (ClassNotFoundException e) {
					break;
				} catch (EOFException e) {
					break;
				}

				// Login size : 2, Join size : 3
				if (message.get(0).equals(MsgKeys.LoginRequest.getKey())) {
					String line;
					String[] userInfo = new String[message.size()];

					// Default status(no-id)
					status = "no-match";

					// compare UserData with Requested Info
					while ((line = userFileReader.readLine()) != null) {
						userInfo = line.split(" ");

						if (userInfo[1].equals(message.get(1))) {
							if (userInfo[2].equals(message.get(2))) {
								status = "id-pw-match"; // Change status(login-success)
								messageList.clear();
								messageList.add("hello_" + userInfo[0]);

								messageListWriter.writeObject(messageList);
								messageListWriter.flush();
								messageListWriter.reset();
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
							messageList.clear();
							messageList.add(MsgKeys.LoginFailByPW.getKey());

							messageListWriter.writeObject(messageList);
							messageListWriter.flush();
							messageListWriter.reset();
							break;
						}

					}
					// No id match
					if (status.equals("no-match")) {
						messageList.clear();
						messageList.add(MsgKeys.LoginFailByID.getKey());

						messageListWriter.writeObject(messageList);
						messageListWriter.flush();
						messageListWriter.reset();
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
					BufferedWriter userFileWriter = new BufferedWriter(
							new FileWriter(FileNames.UserFile.getName(), true));
					for (int i = 1; i < message.size(); i++) {
						userFileWriter.write(message.get(i));
						if (i == message.size() - 1)
							break;
						userFileWriter.write(" ");
					}
					userFileWriter.newLine();
					userFileWriter.flush();
					userFileWriter.close();

					// 회원 가입 성공
					messageWriter.println(MsgKeys.JoinSuccess.getKey());
					messageWriter.flush();
				} else if (message.get(0).equals(MsgKeys.ReceiveFriendsRequest.getKey())) {
					String line;
					String userName = message.get(1);
					String[] friendFileData = new String[2];
					friendList = new ArrayList<String>();
					friendList.add("dummy");

					// compare FriendData with Requested Info
					while ((line = friendFileReader.readLine()) != null) {
						friendFileData = line.split(" ");
						if (friendFileData[0].equals(userName)) {
							friendList.add(friendFileData[1]);
						}
					}
					if (friendList.size() > 1) {
						friendList.set(0, MsgKeys.ReceiveSuccess.getKey());
						messageListWriter.writeObject(friendList);
						messageListWriter.flush();
						messageListWriter.reset();
					} else {
						messageListWriter.writeObject(friendList);
						messageListWriter.flush();
						messageListWriter.reset();
					}
				} else if (message.get(0).equals(MsgKeys.FriendAddRequest.getKey())) {
					String fileLine;
					String userName = message.get(1);
					String friendName = message.get(2);
					String[] friendFileData = new String[2];
					String[] userFileData = new String[3];

					status = "no-id-match";
					// 친구가 존재하는지 확인
					while ((fileLine = userFileReader.readLine()) != null) {
						userFileData = fileLine.split(" ");
						if (userFileData[1].equals(friendName)) {
							status = "id-match";
							friendName = userFileData[0];
							break;
						}
					}

					// 존재하지 않는 경우 응답
					if (status.equals("no-id-match")) {
						messageList.clear();
						messageList.add(MsgKeys.FriendAddFailByID.getKey());

						messageListWriter.writeObject(messageList);
						messageListWriter.flush();
						messageListWriter.reset();
						continue;

					}

					// 이미 친구인지 확인
					while ((fileLine = friendFileReader.readLine()) != null) {
						friendFileData = fileLine.split(" ");
						if (friendFileData[0].equals(userName) && friendFileData[1].equals(friendName)) {
							status = "already-friend";
							break;
						}
					}

					// 이미 친구인 경우 응답
					if (status.equals("already-friend")) {
						messageList.clear();
						messageList.add(MsgKeys.FriendAddFailByDupli.getKey());

						messageListWriter.writeObject(messageList);
						messageListWriter.flush();
						messageListWriter.reset();
						continue;
					} else {
						// 친구 추가에 성공한 경우
						// 친구 파일에 해당 내용 추가하고 클라이언트에 응답
						BufferedWriter friendFileWriter = new BufferedWriter(
								new FileWriter(FileNames.FriendFile.getName(), true));
						friendFileWriter.write(userName);
						friendFileWriter.write(" ");
						friendFileWriter.write(friendName);
						friendFileWriter.newLine();
						friendFileWriter.flush();
						friendFileWriter.close();

						messageList.clear();
						messageList.add("add_" + friendName);

						messageListWriter.writeObject(messageList);
						messageListWriter.flush();
						messageListWriter.reset();
					}

				} else if (message.get(0).equals(MsgKeys.RemoveRequest.getKey())) {
					String friendFileLine;
					String userName = message.get(1);
					List<String> rmvFriendsList = new ArrayList<String>();
					String[] friendFileData = new String[2];
					BufferedWriter tempFileWriter = new BufferedWriter(new FileWriter(FileNames.TempFile.getName()));

					// 삭제할 친구 리스트를 가져옴
					for (int i = 2; i < message.size(); i++) {
						rmvFriendsList.add(message.get(i));
					}

					// 친구 관계인지 확인 후, 임시 파일에 친구들만 추가
					while ((friendFileLine = friendFileReader.readLine()) != null) {
						friendFileData = friendFileLine.split(" ");
						if (friendFileData[0].equals(userName)) {
							if (!rmvFriendsList.contains(friendFileData[1])) {
								tempFileWriter.write(friendFileData[0]);
								tempFileWriter.write(" ");
								tempFileWriter.write(friendFileData[1]);
								tempFileWriter.newLine();
							}
						} else {
							tempFileWriter.write(friendFileData[0]);
							tempFileWriter.write(" ");
							tempFileWriter.write(friendFileData[1]);
							tempFileWriter.newLine();
						}
						tempFileWriter.flush();

					}
					tempFileWriter.close();

					// 임시파일의 내용을 친구 관계 파일로 복사
					tempFileReader = new BufferedReader(new FileReader(FileNames.TempFile.getName()));
					BufferedWriter friendFileWriter = new BufferedWriter(
							new FileWriter(FileNames.FriendFile.getName()));
					String tempFileLine;
					while ((tempFileLine = tempFileReader.readLine()) != null) {
						friendFileWriter.write(tempFileLine);
						friendFileWriter.newLine();
						friendFileWriter.flush();
					}
					friendFileWriter.close();

					messageList.clear();
					messageList.add(MsgKeys.RemoveSuccess.getKey());

					messageListWriter.writeObject(messageList);
					messageListWriter.flush();
					messageListWriter.reset();
				} else if (message.get(0).equals(MsgKeys.RefreshRequest.getKey())) {
					// 로그인 상황 파일을 가져옴
					loginFileReader = new BufferedReader(new FileReader(FileNames.LoginFile.getName()));
					HashMap<String, Integer> countUser = new HashMap<String, Integer>();
					List<String> refreshList = new ArrayList<String>();
					refreshList.add(MsgKeys.RefreshSuccess.getKey());
					String cur_line = "";

					// 해당 유저의 이름이 없으면 추가하고 있으면 갯수를 셈
					while ((cur_line = loginFileReader.readLine()) != null) {
						if (message.contains(cur_line)) {
							if (countUser.get(cur_line) == null) {
								countUser.put(cur_line, 1);
							} else {
								countUser.put(cur_line, countUser.get(cur_line) + 1);
							}
						}
					}

					// 유저의 이름이 홀수개 들어있으면 접속중이라는 의미, 갱신리스트에 추가해줌
					countUser.forEach((user1, count) -> {
						if (count % 2 == 1) {
							refreshList.add(user1);
						}
					});

					// 갱신리스트 전송
					messageListWriter.writeObject(refreshList);
					messageListWriter.flush();
					messageListWriter.reset();
				} else if (message.get(0).equals(MsgKeys.LogoutRequest.getKey())) {
					// 로그아웃시 로그인 상태 파일에 유저 이름을 추가해준다.
					BufferedWriter tempFileWriter = new BufferedWriter(new FileWriter(FileNames.TempFile.getName()));
					loginFileReader = new BufferedReader(new FileReader(FileNames.LoginFile.getName()));
					String fileLine;
					while ((fileLine = loginFileReader.readLine()) != null) {
						if (!fileLine.equals(userName)) {
							tempFileWriter.write(fileLine);
							tempFileWriter.newLine();
							tempFileWriter.flush();
						}
					}
					tempFileReader = new BufferedReader(new FileReader(FileNames.TempFile.getName()));
					loginFile.createNewFile();
					BufferedWriter loginFileWriter = new BufferedWriter(new FileWriter(FileNames.LoginFile.getName()));

					while ((fileLine = tempFileReader.readLine()) != null) {
						loginFileWriter.write(fileLine);
						loginFileWriter.newLine();
						loginFileWriter.flush();
					}
					loginFileWriter.close();
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
					chatroomList.add("dummy");

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
					} else {
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