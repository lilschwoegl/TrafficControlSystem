package simulator;

import org.opencv.core.Point;

public class Config {

	static double simDisplayWidth = 600;
	static double simDisplayHeight = 600;
	
	static double frameHeight = 1080;
	static double frameWidth = 1920;
	
	static double widthRatio = frameWidth / simDisplayWidth;
	static double heightRatio = frameHeight / simDisplayHeight;
	
	// length of road before intersection
	static double roadStripLength = 210;
	static double roadStripFudgeFactor = 50;
	static double roadStripFudged = roadStripLength - roadStripFudgeFactor;
	
	static double roadStripRatio = roadStripFudged / frameHeight;
	
	// lane information on intersection image
	static Point eastBoundLane1 = new Point(0, 320);
	static Point eastBoundLane2 = new Point(0, 355);
	
	static Point westBoundLane1 = new Point(600, 245);
	static Point westBoundLane2 = new Point(600, 210);
	
	static Point southBoundLane1 = new Point(250, 0);
	static Point southBoundLane2 = new Point(210, 0);
	
	static Point northBoundLane1 = new Point(320, 600);
	static Point northBoundLane2 = new Point(355, 600);
	
}
