package enums;

public enum FileNames {
	TempFile("TempData.txt"),
	FriendFile("FriendData.txt"),
	UserFile("UserData.txt"),
	LoginFile("LoginData.txt");
	
	String name;
	FileNames(String name){
		this.name = name;
	}
	public String getName(){
		return name;
	}
}
