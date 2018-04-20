package Test;

import static org.junit.Assert.*;

import org.junit.Test;

import application.BulbColor;
import application.TrafficLight;
import simulator.Constants.Direction;

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
}
