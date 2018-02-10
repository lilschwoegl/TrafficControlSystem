package Simulator;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class loadImage {

	public static BufferedImage fullImage, subImage1, subImage2;
	
	public static void init(){
		fullImage = imageLoader("Intersection-4_way-5_lanes.png");
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
