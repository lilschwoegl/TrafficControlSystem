package simulator;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class loadImage {

	public static BufferedImage 
		fullImage, 
		upCarImage, 
		downCarImage, 
		rightCarImage, 
		leftCarImage,
		greenLight,
		yellowLight,
		redLight;
	
	public static void init(){
		//fullImage = imageLoader("/Intersection-4_way-5_lanes.png");
		fullImage = imageLoader("/Intersection2.png");
		upCarImage = imageLoader("/upCar.png");
		downCarImage = imageLoader("/downCar.png");
		rightCarImage = imageLoader("/rightCar.png");
		leftCarImage = imageLoader("/leftCar.png");
		greenLight = imageLoader("/green.png");
		yellowLight = imageLoader("/yellow.png");
		redLight = imageLoader("/red.png");
		//crop();
	}
	
	public static BufferedImage imageLoader(String path){
		try {
			return ImageIO.read(loadImage.class.getResource(path));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		return null;	
	}
	
	
	/*
	 * Use this method to crop images
	 * public static void crop(){
		subImage1 = fullImage.getSubimage(100, 100, 200, 200);
	}
	*/
	
}
