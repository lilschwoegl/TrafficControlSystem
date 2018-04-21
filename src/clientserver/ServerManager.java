package clientserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServerManager {

	static final int port = 4444;
	ServerSocket serverSocket;
	Socket socket;
	
	private static ServerManager instance;
	
	private ServerManager()
	{		
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static ServerManager getInstance()
	{
		if (instance == null)
			instance = new ServerManager();
		return instance;
	}
	
	public void run()
	{
		while (true)
		{
			try {
				socket = serverSocket.accept();
				(new ServerThread(socket)).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public static void main(String[] args)
	{
		ServerManager.getInstance().run();
	}
	
}
