package application;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import simulator.Constants.Direction;

public class CameraFeedDisplay extends Thread {

	private VideoInput videoFeed;
	private Mat lastFrame = new Mat();
	private boolean staleFrame = true;
	private Direction facingDirection;
	private RoadLinesCollection roadLines;
	
	@FXML
	private Button feedSelBtn;
	@FXML
	private ComboBox<String> feedList;
	@FXML
	private ImageView imgOut;
	
	public CameraFeedDisplay(ImageView imgOut, Button feedSelBtn, ComboBox<String> feedList, Direction facingDirection)
	{
		this.imgOut = imgOut;
		this.feedList = feedList;
		this.feedSelBtn = feedSelBtn;
		videoFeed = new VideoInput();
		this.facingDirection = facingDirection;
		roadLines = new RoadLinesCollection();
	}
	
	public void showImage(Mat frame)
	{
		Image imageToShow = Utils.mat2Image(frame);
		Utils.updateImageView(imgOut, imageToShow);
	}
	
	public boolean isFrameStale()
	{
		return staleFrame;
	}
	
	public synchronized Mat getLastFrame()
	{
		staleFrame = true;
		return lastFrame;
	}
	
	public void selectNewFeed()
	{
		String feedName = feedList.getValue();
		
		if (feedName.equals(""))
		{
			return;
		}

		// if the feed has changed, get the newly selected one
		if (!videoFeed.getCurrentFeed().equals(feedName))
		{
			videoFeed.selectCameraFeed(feedName);
		}

		// is the video stream available?
		if (videoFeed.isOpened())
		{
			if (!isAlive())
				start();
		}
	}
	
	@Override
	public void run()
	{
		while (true)
		{
			// effectively grab and process a single frame
			Mat frame = new Mat();

			try {
				
				// nlogMsg("Grabbing frame");
				
				frame = videoFeed.grabFrame();
				
				//logMsg("Grabbed frame");
				
				if (frame.empty() || frame.width() <= 0 || frame.height() <= 0)
				{
					//logMsg("Got an empty frame!");
					videoFeed.refresh();
					frame = videoFeed.grabFrame();
				}
				
				//System.out.println(frame.size().height);
				//System.out.println(frame.size().width);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// frame = processFrame(frame);
			frame.copyTo(lastFrame);
			staleFrame = false;

			try {
				// grab a frame every 50 ms
				sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public RoadLinesCollection getRoadLines()
	{
		return roadLines;
	}
	
}
