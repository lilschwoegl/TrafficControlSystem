package application;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class SystemUIController {
	
	@FXML
	private Button selFeedBtn;
	@FXML
	private ImageView imageOutputImgvw;
	@FXML
	private ImageView imgv1, imgv2;
	@FXML
	private ComboBox<String> feedListCbx;
	@FXML
	private Slider blurSlide, dilationSlide, threshSlide, minCSlide;
	@FXML
	private Label slideProp;
	private ObjectProperty<String> slideValuesProp;
	
	private double blurValue, dilationValue, threshValue, minCValue;
	
	private VideoInput videoFeed = new VideoInput();
	
	private ScheduledExecutorService timer;
	
	// this will initialize and update every 500 msec
	private Mat backgroundFrame;
	private int counter = 0;
	
	BackgroundSubtractor backSub;
	
	// a flag to change the button behavior
	private boolean cameraActive;
	
	public void initialize()
	{
		// load the feed names
		feedListCbx.getItems().addAll(VideoInput.feedNames);
		
		//backSub = Video.createBackgroundSubtractorMOG2(300, 200, false);
		
		slideValuesProp = new SimpleObjectProperty<>();
		this.slideProp.textProperty().bind(slideValuesProp);
		
		// get slider values
		blurValue = this.blurSlide.getValue();
		threshValue = this.threshSlide.getValue();
		dilationValue = this.dilationSlide.getValue();
		minCValue = this.minCSlide.getValue();
		
		// show the current selected HSV range
		String valuesToPrint = 
				"Blur:     " + blurValue + "\n" + 
				"Thresh:   " + threshValue + "\n" + 
				"Dilation: " + dilationValue + "\n" + 
				"C Value:  " + minCValue;
		Utils.onFXThread(this.slideValuesProp, valuesToPrint);
	}
	
	@FXML
	private void startFeed()
	{
		
		String feedName = feedListCbx.getValue();
		
		if (feedName.equals(""))
		{
			return;
		}
		
		if (!videoFeed.getCurrentFeed().equals(feedName))
		{
			videoFeed.selectCameraFeed(feedName);
		}
		
		//if (!this.cameraActive)
		//{
			// is the video stream available?
			if (this.videoFeed.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 100 ms (30 frames/sec)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						// effectively grab and process a single frame
						Mat frame = grabFrame();
						frame = processFrame(frame);
						
						// convert and show the frame
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(imageOutputImgvw, imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 100, TimeUnit.MILLISECONDS);
				
				// update the button content
				this.selFeedBtn.setText("Select Video Feed");
			}
			else
			{
				// log the error
				System.err.println("Failed to open the camera connection...");
			}
	}
	
	private Mat grabFrame()
	{
		Mat frame = new Mat();
		
		// check if the capture is open
		if (this.videoFeed.isOpened())
		{
			try
			{
				// read the current frame
				this.videoFeed.read(frame);
				
				// if the frame is not empty, process it
				if (!frame.empty())
				{
					Imgproc.resize(frame, frame, new Size(500,500));
					return frame;
				}
			}
			catch (Exception e)
			{
				// log the (full) error
				System.err.print("Exception during the image elaboration...");
				e.printStackTrace();
			}
		}
		
		return frame;
	}
	
	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Mat processFrame(Mat frame)
	{
		Mat grayFrame = new Mat();
		Mat curFrame = new Mat();
		
		frame.copyTo(curFrame);
		
		// get slider values
		blurValue = this.blurSlide.getValue();
		threshValue = this.threshSlide.getValue();
		dilationValue = this.dilationSlide.getValue();
		minCValue = this.minCSlide.getValue();
		
		// show the current selected HSV range
		String valuesToPrint = 
				"Blur:     " + blurValue + "\n" + 
				"Thresh:   " + threshValue + "\n" + 
				"Dilation: " + dilationValue + "\n" + 
				"C Value:  " + minCValue;
		Utils.onFXThread(this.slideValuesProp, valuesToPrint);

		// convert colors and remove blur
		//Imgproc.resize(frame, frame, new Size(500,500));
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		Imgproc.GaussianBlur(grayFrame, grayFrame, new Size(blurValue,blurValue), 0);

		updateImageView(imgv1, Utils.mat2Image(grayFrame));
		
		if (++counter % 2 == 0 || backgroundFrame == null)
		{
			backgroundFrame = grabFrame();
			// convert colors and remove blur
			//Imgproc.resize(backgroundFrame, backgroundFrame, new Size(500,500));
			Imgproc.cvtColor(backgroundFrame, backgroundFrame, Imgproc.COLOR_BGR2GRAY);
			Imgproc.GaussianBlur(backgroundFrame, backgroundFrame, new Size(blurValue,blurValue), 0);
		}
		
		Mat output = new Mat();
		Core.absdiff(backgroundFrame, grayFrame, output);
		
		//Mat output = new Mat();
		//grayFrame.copyTo(output, fgMask);
		
		Imgproc.threshold(output, output, threshValue, 255, Imgproc.THRESH_BINARY);
		
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilationValue, dilationValue));
		Imgproc.dilate(output, output, dilateElement);
		
		updateImageView(imgv2, Utils.mat2Image(output));
		
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		
		// find contours
		Imgproc.findContours(output, contours, hierarchy, Imgproc.RETR_EXTERNAL,
				Imgproc.CHAIN_APPROX_SIMPLE);
		
		
		
		// if any contour exist...
		if (hierarchy.size().height > 0 && hierarchy.size().width > 0)
		{
			// for each contour, display it in blue
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
			{

				//if (Imgproc.contourArea(hierarchy) < 2)
				//	continue;
				
				if (Imgproc.contourArea(contours.get(idx)) < minCValue)
				{
					System.out.println("Skipping one...");
					continue;
				}
				
				Rect r = Imgproc.boundingRect(contours.get(idx));
				
				Imgproc.rectangle(curFrame, 
						new Point(r.x, r.y),
						new Point(r.x + r.width, r.y + r.height),
						new Scalar(0, 250, 0));
				
				//Imgproc.drawContours(curFrame, contours, idx, new Scalar(0, 250, 0), 
				//		2, 8, hierarchy, 0, new Point());
			}
		}
		
		return curFrame;
			
	}
	
	/**
	 * Set typical {@link ImageView} properties: a fixed width and the
	 * information to preserve the original image ration
	 * 
	 * @param image
	 *            the {@link ImageView} to use
	 * @param dimension
	 *            the width of the image to set
	 */
	private void imageViewProperties(ImageView image, int dimension)
	{
		// set a fixed width for the given ImageView
		image.setFitWidth(dimension);
		// preserve the image ratio
		image.setPreserveRatio(true);
	}
	
	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		
		if (this.videoFeed.isOpened())
		{
			// release the camera
			this.videoFeed.release();
		}
	}
	
	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * 
	 * @param view
	 *            the {@link ImageView} to update
	 * @param image
	 *            the {@link Image} to show
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}
	
	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}
	
}
