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
	
	static List<String> classes = new ArrayList<String>();
	
	public int classId;
	public double classProb;
	public float confidence;
	
	public float xLeftBot;
	public float yLeftBot;
	public float xRightTop;
	public float yRightTop;
	
	/**
	 * Loads the class names from the file at the given path
	 * @param path Path to the class name file
	 * @throws IOException Exception when file cannot be read
	 */
	public static void loadClassNames(String path) throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line;
		
		// read all lines in the file
		while ((line = br.readLine()) != null)
		{
			classes.add(line);
		}
	}
	
	/**
	 * Gets the bottom left coordinate of the detect box
	 * @return Bottom left coordinate
	 */
	public Point getLeftBot()
	{
		return new Point(xLeftBot, yLeftBot);
	}
	
	/**
	 * Gets the top right coordinate of the detect box
	 * @return Top right coordinate
	 */
	public Point getRightTop()
	{
		return new Point(xRightTop, yRightTop);
	}
	
	/**
	 * Gets the bounding rect of the detect
	 * @return Detect bounding rect
	 */
	public Rect getBoundingRect()
	{
		return new Rect(getLeftBot(), getRightTop());
	}
	
	/**
	 * Gets the center X coordinate of the detect box
	 * @return Center X coordinate
	 */
	public int getObjectCenterX()
	{
		return (int)((xRightTop + xLeftBot) / 2);
	}
	
	/**
	 * Gets the center Y coordinate of the detect
	 * @return Center Y coordinate
	 */
	public int getObjectCenterY()
	{
		return (int)((yRightTop + yLeftBot) / 2);
	}
	
	/**
	 * Gets the center point of the detect
	 * @return Detect center point
	 */
	public Point getObjectCenter()
	{
		return new Point(getObjectCenterX(), getObjectCenterY());
	}
	
	/**
	 * Gets the class assigned to the detect
	 * @return Class name of the detect
	 */
	public String getClassName()
	{
		return classes.get(classId);
	}
	
	/**
	 * Gets the width of the detect box
	 * @return Detect box width
	 */
	public float getWidth()
	{
		return xRightTop - xLeftBot;
	}
	
	/**
	 * Gets the height of the detect box
	 * @return Detect box height
	 */
	public float getHeight()
	{
		return yRightTop - yLeftBot;
	}
}
