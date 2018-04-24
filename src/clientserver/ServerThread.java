package clientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import application.TrafficController;

public class ServerThread extends Thread{

	protected Socket socket;
	protected ObjectOutputStream out;
	protected BufferedReader in;

	
	public ServerThread(Socket socket)
	{
		this.socket = socket;
		
		try {
			out = new ObjectOutputStream(socket.getOutputStream());
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
				
				QueryMessage qm = new QueryMessage();
				
				if (line.equals("[METRICS]"))
				{
					try {
						ResultSet result = TrafficController.sql.executeQuery("select * from Metrics");

						qm.setColumns("TIMESTAMP", "NAME", "DESCRIPTION", "VALUE");
						
						while (result.next())
						{
							qm.addRow(result.getString("timestamp"), result.getString("name"), 
									result.getString("description"), result.getString("value"));
						}
						
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(line.equals("[EVENTS]"))
				{
					try {
						ResultSet result = TrafficController.sql.executeQuery("select * from Metrics");

						qm.setColumns("TIMESTAMP", "NAME", "VALUE");
						
						while (result.next())
						{
							qm.addRow(result.getString("timestamp"), result.getString("name"), result.getString("value"));
						}
						
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(line.equals("[TRAFFIC]"))
				{
					try {
						ResultSet result = TrafficController.sql.executeQuery("select * from Traffic");

						qm.setColumns("TIMESTAMP", "DIRECTION");
						
						while (result.next())
						{
							qm.addRow(result.getString("timestamp"), result.getString("direction"));
						}
						
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(line.equals("[bye]"))
				{
					break;
				}
				
				out.writeObject(qm);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
