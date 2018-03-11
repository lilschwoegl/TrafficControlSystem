package Test;

import static org.junit.Assert.*;

import org.junit.Test;

import application.Direction;
import application.Color;
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
