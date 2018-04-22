package clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;

import application.TrafficController;

public class ServerThread extends Thread{

	protected Socket socket;
	protected PrintWriter out;
	protected BufferedReader in;

	
	public ServerThread(Socket socket)
	{
		this.socket = socket;
		
		try {
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(
			            new InputStreamReader(
			                socket.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void run() {
		String line;
		
		try {
			while ((line = in.readLine()) != null)
			{
				if (line.equals("[data]"))
				{
					try {
						ResultSet result = TrafficController.sql.executeQuery("select * from Metrics");
						System.out.println(result.getString(1));
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(line.equals("[bye]"))
				{
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
