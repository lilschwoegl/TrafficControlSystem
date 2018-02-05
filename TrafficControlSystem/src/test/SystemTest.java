package test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import application.VideoInput;

class SystemTest {

	// As a driver, I would like the traffic light to sense that I am approaching the 
	// intersection and turn green.
	
	VideoInput input;
	
	@BeforeEach
	void setUp() throws Exception {
		input = new VideoInput("");
	}

	@AfterEach
	void tearDown() throws Exception {
		input.release();
	}

	@Test
	void test() {
		boolean detection = false;
		Object light = new Object();
		
		while (!detection)
		{
			// do nothing
		}
		
		// we have a detection, change the lights
	}
	
	boolean isDirectionLightOn()
	{
		return false;
	}
	
	void changeLight()
	{
		System.out.println("");
	}
	
	int counter = 0;
	boolean isDetection()
	{
		counter++;
		
		if (counter > 5)
			return true;
		
		return false;
	}

}
