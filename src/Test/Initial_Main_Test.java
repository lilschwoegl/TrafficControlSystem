package Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.Vector;

import org.junit.Test;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import application.BulbColor;
import application.DetectedObject;
import application.SQLite;
import application.TrafficController;
import application.TrafficController.SignalLogicConfiguration;
import clientserver.QueryMessage;
import clientserver.ServerClient;
import clientserver.ServerManager;
import config.TrafficControllerConfig;
import application.TrafficLight;
import application.VideoInput;
import application.YoloDetector;
import simulator.Constants.Direction;

public class Initial_Main_Test {
	
	@Test
	public void getvideoFeedTest () {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoInput vi = new VideoInput();
		vi.selectCameraFeed("Recorded 1");
		
		if (!vi.isOpened())
			fail("Video feed not opened");
		
		Mat m = new Mat();
		try {
			vi.grabFrame().copyTo(m);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertTrue("Video feed connected and frame processed", m.width() > 0 && m.height() > 0);
	}
	
	@Test
	public void incomingVehicleTest () {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		VideoInput vi = new VideoInput();
		YoloDetector yolo = null;
		try {
			yolo = new YoloDetector("yolo/cfg/yolov2-custom.cfg",
					"yolo/weights/yolov2-tiny-obj_10500.weights",
					"yolo/classes/custom-training.names");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			fail("YOLO network not created");
		}
		vi.selectCameraFeed("Recorded 1");
		
		if (!vi.isOpened())
			fail("Video feed not opened");
		
		Mat m = new Mat();
		boolean vehicleFound = false;
		int maxCount = 100, count = 0;
		Vector<DetectedObject> detects = new Vector<DetectedObject>();
		
		while (count < maxCount && !vehicleFound)
		{
		
			try {
				vi.grabFrame().copyTo(m);
				detects = yolo.detectObjectYolo(m);
				
				for (DetectedObject d : detects)
				{
					if (d.getClassName().equals("Regular Vehicle"))
					{
						return;
					}
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	@Test
	public void testDatabaseCreation() {
		String now = Instant.now().toString();
		System.out.println("testDatabaseCreation: now = " + now);
		//String nowShort = now.toString().substring(0, now.toString().length() - 5); //strip off milliseconds
		int numResults = 0;
		
		TrafficController tc = new TrafficController(SignalLogicConfiguration.FailSafe, 1, 60, 1, 60);
		SQLite sql = new SQLite(TrafficControllerConfig.databaseName, TrafficControllerConfig.databaseSchema);		
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
		
		SQLite sql = new SQLite("Test.db", TrafficControllerConfig.databaseSchema);
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
		
		SQLite sql = new SQLite("Test.db", TrafficControllerConfig.databaseSchema);
		
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
		TrafficController tc = new TrafficController(SignalLogicConfiguration.FailSafe, 3, 60, 3, 60); // sample settings from simulator
		int numLights = tc.GetTrafficLights().size();
		assertTrue("More than 1 TrafficLight is created, count=" + numLights, numLights > 0);
	}
	
	@Test
	public void getLightCreationTest () {
	    TrafficLight light = new TrafficLight(Direction.NORTH);
	    assertEquals(light.getTravelDirection(), Direction.NORTH);
	    assertEquals(light.getFacingDirection(), Direction.SOUTH);
	    assertEquals(light.GetColor(), BulbColor.Red);
	}
	
	@Test
	public void getLightUniqueIDTest () {
	    TrafficLight light = new TrafficLight(Direction.NORTH);
	    TrafficLight light2 = new TrafficLight(Direction.NORTH);
	    assertNotEquals(light.getID(), light2.getID());
	}
	
	@Test
	  public void testLightChangeToGreen() {
	    TrafficLight light = new TrafficLight(Direction.NORTH);
	    assertEquals(light.GetColor(), BulbColor.Red);
	    light.TurnGreen();
//	    try { Thread.sleep(1000); }
//    	catch (InterruptedException e) { e.printStackTrace(); }
	    assertEquals(light.GetColor(), BulbColor.Green);
	}
	 
	@Test
	  public void testLightChangeToRed() {
	    TrafficLight light = new TrafficLight(Direction.NORTH);
	    assertEquals(light.GetColor(), BulbColor.Red);
	    light.TurnGreen();
//	    try { Thread.sleep(1000); }
//	    catch (InterruptedException e) { e.printStackTrace(); }
	    assertEquals(light.GetColor(), BulbColor.Green);
	    light.TurnRed();
//	    try { Thread.sleep(6000); }
//    	catch (InterruptedException e) { e.printStackTrace(); }
	    assertEquals(light.GetColor(), BulbColor.Red);
	}
	
	@Test 
	public void serverStartTest()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				ServerManager.getInstance().run();
			}
		};
		
		t.start();
		
		assertTrue("Server is running", ServerManager.getInstance().isRunning());
	}
	
	@Test
	public void clientConnectToServer()
	{
		TrafficController tc = new TrafficController(SignalLogicConfiguration.FailSafe, 3, 60, 3, 60); // sample settings from simulator
		Thread t = new Thread()
		{
			public void run()
			{
				ServerManager.getInstance().run();
			}
		};
		
		t.start();
		ServerClient sc = new ServerClient();
		
		QueryMessage qm = sc.queryServer("TRAFFIC");
		
		assertTrue("Client received data", qm.getRows().length > 0);
	}
}
