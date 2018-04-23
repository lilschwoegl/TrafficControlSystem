package clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerClient {
	
	public static void main(String[] args)
	{
		String hostName = "localhost";
		int port = 4444;
		
		try {
			Socket socket = new Socket(hostName, port);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(
	                new InputStreamReader(socket.getInputStream()));

			out.println("[METRICS]");
			
			String queryResult = in.readLine();
			String[] queryParts = queryResult.split("\\[\\$\\]");
			
			for (String s : queryParts)
			{
				System.out.println(s);
			}
			
			out.println("[EVENTS]");

			queryResult = in.readLine();
			queryParts = queryResult.split("\\[\\$\\]");
			
			for (String s : queryParts)
			{
				System.out.println(s);
			}
			
			socket.close();
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
