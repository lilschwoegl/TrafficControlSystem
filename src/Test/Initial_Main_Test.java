package Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;

import org.junit.Test;

import application.Color;
import application.Config;
import application.Direction;
import application.SQLite;
import application.TrafficController;
import application.TrafficLight;

public class Initial_Main_Test {
	
	@Test
	public void getvideoFeedTest () {
		fail("Not yet implemented");
	}
	
	@Test
	public void incomingVehicleTest () {
		fail("Not yet implemented");
	}
	
	@Test
	public void testDatabaseCreation() {
		String now = Instant.now().toString();
		System.out.println("testDatabaseCreation: now = " + now);
		//String nowShort = now.toString().substring(0, now.toString().length() - 5); //strip off milliseconds
		int numResults = 0;
		
		TrafficController tc = new TrafficController(1, 60, 1, 60);
		SQLite sql = new SQLite(Config.databaseName, Config.databaseSchema);		
		try {
			String stmt = "select * from Events where name like '%Creation' and timestamp >= '" + now + "'";
			System.out.println("stmt = '" + stmt + "'");
			ResultSet rs = sql.executeQuery(stmt);
			while (rs.next() ) {
				numResults++;
				System.out.println(String.format("testDatabaseCreation %d:"
					+ "\n  timestamp %s"
					+ "\n  name      %s"
					+ "\n  value     %s"
					, numResults, rs.getString("timestamp"), rs.getString("name"), rs.getString("value")));
			}
		}
		catch (SQLException e) { e.printStackTrace(); }
		assertTrue("1 or more Creation events are recorded", numResults > 0);
	}
	
	@Test
	public void testDatabaseWrite() {
		Instant now = Instant.now();
		System.out.println("testDatabaseWrite: now = " + now);
		
		SQLite sql = new SQLite("Test.db", Config.databaseSchema);
		int numRowsUpdated = 0;
		try {
			numRowsUpdated += sql.executeUpdate("insert into Events (timestamp,name,value) values ('" + Instant.now() + "', 'testDatabaseWrite', 'test data')");
			System.out.println("testDatabaseWrite: numRowsUpdated = " + numRowsUpdated);
		}
		catch (SQLException e1) { e1.printStackTrace(); }
		
		sql.CloseDatabase();
		
		assertTrue("1 row written to database", numRowsUpdated == 1);
	}

	@Test
	public void testDatabaseRead() {
		Instant now = Instant.now();
		System.out.println("testDatabaseRead: now = " + now);
		String nowShort = now.toString().substring(0, now.toString().length() - 5); //strip off milliseconds
		
		SQLite sql = new SQLite("Test.db", Config.databaseSchema);
		
		// write a row to Events table
		int numRowsUpdated = 0;
		try {
			numRowsUpdated += sql.executeUpdate("insert into Events (timestamp,name,value) values ('" + Instant.now() + "', 'testDatabaseRead', 'value 1')");
			numRowsUpdated += sql.executeUpdate("insert into Events (timestamp,name,value) values ('" + Instant.now() + "', 'testDatabaseRead', 'value 2')");
			System.out.println("testDatabaseRead: numRowsUpdated = " + numRowsUpdated);
		}
		catch (SQLException e1) { e1.printStackTrace(); }
		assertTrue("2+ rows written to database", numRowsUpdated >= 2);
		
		// read rows from Events table after test startup timestamp
		int numRowsRead = 0;
		try {
			String stmt = "select * from Events where name = 'testDatabaseRead' and timestamp >= '" + nowShort + "' order by timestamp";
			System.out.println("stmt = '" + stmt + "'");
			ResultSet rs = sql.executeQuery(stmt);
			while (rs.next() ) {
				numRowsRead++;
				System.out.println(String.format("testDatabaseRead: row %d:"
					+ "\n  timestamp %s"
					+ "\n  name      %s"
					+ "\n  value     %s"
					, numRowsRead, rs.getString("timestamp"), rs.getString("name"), rs.getString("value")));
			}
		}
		catch (SQLException e) { e.printStackTrace(); }
		
		sql.CloseDatabase();
		
		assertTrue("2+ rows read from database", numRowsRead >= 2);
	}
	
	@Test
	public void testControllerCreatesLights() {
		TrafficController tc = new TrafficController(3, 60, 3, 60); // sample settings from simulator
		int numLights = tc.GetTrafficLights().size();
		assertTrue("More than 1 TrafficLight is created, count=" + numLights, numLights > 0);
	}
	
	@Test
	public void getLightCreationTest () {
	    TrafficLight light = new TrafficLight(Direction.North);
	    assertEquals(light.getTravelDirection(), Direction.North);
	    assertEquals(light.getFacingDirection(), Direction.South);
	    assertEquals(light.GetColor(), Color.Red);
	}
	
	@Test
	public void getLightUniqueIDTest () {
	    TrafficLight light = new TrafficLight(Direction.North);
	    TrafficLight light2 = new TrafficLight(Direction.North);
	    assertNotEquals(light.getID(), light2.getID());
	}
	
	@Test
	  public void testLightChangeToGreen() {
	    TrafficLight light = new TrafficLight(Direction.North);
	    assertEquals(light.GetColor(), Color.Red);
	    light.TurnGreen();
//	    try { Thread.sleep(1000); }
//    	catch (InterruptedException e) { e.printStackTrace(); }
	    assertEquals(light.GetColor(), Color.Green);
	}
	 
	@Test
	  public void testLightChangeToRed() {
	    TrafficLight light = new TrafficLight(Direction.North);
	    assertEquals(light.GetColor(), Color.Red);
	    light.TurnGreen();
//	    try { Thread.sleep(1000); }
//	    catch (InterruptedException e) { e.printStackTrace(); }
	    assertEquals(light.GetColor(), Color.Green);
	    light.TurnRed();
//	    try { Thread.sleep(6000); }
//    	catch (InterruptedException e) { e.printStackTrace(); }
	    assertEquals(light.GetColor(), Color.Red);
	}
}
