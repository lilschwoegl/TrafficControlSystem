package application;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;
import org.opencv.core.Rect;

public class DetectedObject {
	
//	static String[] classes = new String[] {
//			"aeroplane",
//			"bicycle",
//			"bird",
//			"boat",
//			"bottle",
//			"bus",
//			"car",
//			"cat",
//			"chair",
//			"cow",
//			"diningtable",
//			"dog",
//			"horse",
//			"motorbike",
//			"person",
//			"pottedplant",
//			"sheep",
//			"sofa",
//			"train",
//			"tvmonitor"
//	};
	
	static List<String> classes = new ArrayList<String>();
	
	public int classId;
	public double classProb;
	public float confidence;
	
	public float xLeftBot;
	public float yLeftBot;
	public float xRightTop;
	public float yRightTop;
	
	public DetectedObject()
	{
		
	}
	
	public static void loadClassNames(String path)
	{
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String line;
			
			while ((line = br.readLine()) != null)
			{
				classes.add(line);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Point getLeftBot()
	{
		return new Point(xLeftBot, yLeftBot);
	}
	
	public Point getRightTop()
	{
		return new Point(xRightTop, yRightTop);
	}
	
	public Rect getBoundingRect()
	{
		return new Rect(getLeftBot(), getRightTop());
	}
	
	public int getObjectCenterX()
	{
		return (int)((xRightTop + xLeftBot) / 2);
	}
	
	public int getObjectCenterY()
	{
		return (int)((yRightTop + yLeftBot) / 2);
	}
	
	public Point getObjectCenter()
	{
		return new Point(getObjectCenterX(), getObjectCenterY());
	}
	
	public String getClassName()
	{
		return classes.get(classId);
	}
	
	public float getWidth()
	{
		return xRightTop - xLeftBot;
	}
	
	public float getHeight()
	{
		return yRightTop - yLeftBot;
	}
}
