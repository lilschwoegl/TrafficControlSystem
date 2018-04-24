package clientserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerClient {
	
	private String hostName = "localhost";
	private int port = 4444;
	
	public QueryMessage queryServer(String command)
	{
		
		QueryMessage qm = null;
		
		try {
			Socket socket = new Socket(hostName, port);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

			out.println("[" + command + "]");
			
			qm = (QueryMessage)in.readObject();
			
			socket.close();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return qm;
		
	}
	
	private String[] getHeader(String queryResult)
	{
		String header = queryResult.split("\\{H\\}")[0];
		
		return header.split("\\{C\\}");
	}
	
	public static void main(String[] args)
	{
		ServerClient sc = new ServerClient();
	}
	
}
