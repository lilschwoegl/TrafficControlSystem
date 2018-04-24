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
				if (line.equals("[METRICS]"))
				{
					try {
						ResultSet result = TrafficController.sql.executeQuery("select * from Metrics");

						String msg = "";
						
						while (result.next())
						{
							msg += String.format("TIME: %s, NAME: %s, DESC: %s, VAL: %s[$]", 
									result.getString("timestamp"), result.getString("name"), 
									result.getString("description"), result.getString("value"));
							// timestamp TEXT NOT NULL, name TEXT NOT NULL, description TEXT NULL, valueType TEXT NOT NULL, value BLOB NOT NULL
						}
						
						out.println(msg);
						
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(line.equals("[EVENTS]"))
				{
					try {
						ResultSet result = TrafficController.sql.executeQuery("select * from Events");

						String msg = "";
						
						while (result.next())
						{
							msg += String.format("TIME: %s, NAME: %s, VAL: %s[$]", 
									result.getString("timestamp"), result.getString("name"), 
									result.getString("value"));
							// timestamp TEXT NOT NULL, name TEXT NOT NULL, description TEXT NULL, valueType TEXT NOT NULL, value BLOB NOT NULL
						}
						
						out.println(msg);
						
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
