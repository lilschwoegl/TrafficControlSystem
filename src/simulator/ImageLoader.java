package simulator;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import application.BulbColor;

public class ImageLoader {

	private static BufferedImage normalVehicle[] = new BufferedImage[4]; 
	private static BufferedImage emergencyVehicle[] = new BufferedImage[4];
	private static BufferedImage intersection;
	private static BufferedImage trafficLights[] = new BufferedImage[3];
	private boolean initialized = false;
	
	public enum Orientation
	{
		UP,
		DOWN,
		LEFT,
		RIGHT
	}
	
	public static void init()
	{
		// load intersection image
		intersection = loadImage("/Intersection2.png");
		
		// fill in the image arrays
		normalVehicle[Orientation.UP.ordinal()] = loadImage("/upCar.png");
		normalVehicle[Orientation.DOWN.ordinal()] = loadImage("/downCar.png");
		normalVehicle[Orientation.LEFT.ordinal()] = loadImage("/leftCar.png");
		normalVehicle[Orientation.RIGHT.ordinal()] = loadImage("/rightCar.png");
		
		emergencyVehicle[Orientation.UP.ordinal()] = loadImage("/upFireTruck.png");
		emergencyVehicle[Orientation.DOWN.ordinal()] = loadImage("/downFireTruck.png");
		emergencyVehicle[Orientation.LEFT.ordinal()] = loadImage("/leftFireTruck.png");
		emergencyVehicle[Orientation.RIGHT.ordinal()] = loadImage("/rightFireTruck.png");
		
		trafficLights[BulbColor.Red.ordinal()] = loadImage("/red.png");
		trafficLights[BulbColor.Yellow.ordinal()] = loadImage("/yellow.png");
		trafficLights[BulbColor.Green.ordinal()] = loadImage("/green.png");
	}
	
	public static BufferedImage getVehicleImage(String type, Orientation orientation)
	{
		switch (type)
		{
		case "normalVehicle":
			return normalVehicle[orientation.ordinal()];
		case "emergencyVehicle":
			return emergencyVehicle[orientation.ordinal()];
		default:
			return null;
		}
	}
	
	public static BufferedImage getTrafficLightImage(BulbColor color)
	{
		return trafficLights[color.ordinal()];
	}
	
	public static BufferedImage getIntersectionImage()
	{
		return intersection;
	}
	
	private static BufferedImage loadImage(String path)
	{
		try {
			return ImageIO.read(ImageLoader.class.getResource(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
}
