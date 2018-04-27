package application;
import org.opencv.core.Mat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CameraFeedDisplay extends Thread {

	private VideoInput videoFeed;
	private Mat lastFrame = new Mat();
	private boolean staleFrame = true;
	private RoadLinesCollection roadLines;
	private long sleepTime = 25;
	private volatile boolean running = false;
	
	@FXML
	private Button feedSelBtn;
	@FXML
	private ComboBox<String> feedList;
	@FXML
	private ImageView imgOut;
	
	public CameraFeedDisplay(ImageView imgOut, Button feedSelBtn, ComboBox<String> feedList)
	{
		this.imgOut = imgOut;
		this.feedList = feedList;
		this.feedSelBtn = feedSelBtn;
		videoFeed = new VideoInput();
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
			{
				start();
				running = true;
			}
		}
	}
	
	public void setSleepTime(long ms)
	{
		this.sleepTime = ms;
	}
	
	@Override
	public void run()
	{
		while (running)
		{
			// effectively grab and process a single frame
			Mat frame = new Mat();

			try {
				
				frame = videoFeed.grabFrame();
				
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
				sleep(sleepTime);
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
	
	public void cleanup()
	{
		running = false;
		videoFeed.release();
	}
	
}
