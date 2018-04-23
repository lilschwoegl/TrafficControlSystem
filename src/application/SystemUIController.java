package application;

import java.io.IOException;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import observer.TrackUpdateObservable;
import observer.UITrackObserver;
import simulator.Constants.Direction;
import tracking.Track;
import tracking.Tracker;
import tracking.TrackerConfig;

public class SystemUIController {

	// setup the FXML control accessors
	@FXML
	private Button selFeedBtn1, selFeedBtn2, selFeedBtn3, selFeedBtn4;
	@FXML
	private ImageView imgOut1, imgOut2, imgOut3, imgOut4;
	@FXML
	private ComboBox<String> feedList1, feedList2, feedList3, feedList4;
	@FXML
	private Label trackLbl1, trackLbl2, trackLbl3, trackLbl4;
	public static ObjectProperty<String> trackLblProp1, trackLblProp2, trackLblProp3, trackLblProp4;
	
	private CameraFeedDisplay[] cameraFeeds = new CameraFeedDisplay[4];
	//boolean useVocYolo = true;

	//static Net yolo;

	Tracker tracker[] = new Tracker[4];
	static Mat imag;
	static Mat orgin;
	static Mat kalman;
	
	boolean saveFramesToFile = false;
	boolean firstRun = true;
	long frameCounter = 0;

	UITrackObserver trafficObserver;
	
	boolean drawTrace = false;
	boolean extrapDetects = true;
	
	VideoWriter vwriter = new VideoWriter();
	boolean recordVideo = true;
	
	YoloDetector yolo;
	
	private Thread frameGrabber = new Thread()
	{
		int counter = 0;
		long startTime = Utils.getCurrentTimeMs();;
		long endTime = 0;
		long totalTime = 0;
		long sleepTime = 25;
		
		public void run()
		{
			while(true)
			{
				if (cameraFeeds[counter].isAlive())
				{
					boolean staleFrame = cameraFeeds[counter].isFrameStale();
					Mat frame = cameraFeeds[counter].getLastFrame();
					cameraFeeds[counter].setSleepTime(totalTime);
					
					if (!frame.empty() && frame.width() > 0 && frame.height() > 0)
					{
						if (!staleFrame)
							saveFrame(frame);
						
						frame = processFrame(frame, tracker[counter], cameraFeeds[counter].getRoadLines());
					}
					
					cameraFeeds[counter].showImage(frame);
					
	
					try {
						sleep(sleepTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				if (counter >= 3)
				{
					counter = 0;
					endTime = Utils.getCurrentTimeMs();
					totalTime = (endTime - startTime);
					startTime = Utils.getCurrentTimeMs();
					Utils.onFXThread(SystemUIController.trackLblProp1, "Total time: " + totalTime + " ms, Frame Time: " + sleepTime + " ms");
					
					try {
						sleep(2);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
				{
					counter++;
				}
			}
		}
	};
	
	private void saveFrame(Mat frame)
	{
		if (recordVideo)
			{
				if (firstRun)
				{
					vwriter.open("C:\\Temp\\intersection_18.avi", VideoWriter.fourcc('D', 'I', 'V', 'X'), 15, frame.size(), true);
					firstRun = false;
				}
				
				if (frame.width() > 0 && frame.height() > 0)
				{
					Mat f = new Mat();
					Imgproc.resize(frame, f, new Size(frame.width(), frame.height()));
					if (f.channels() == 4)
						Imgproc.cvtColor(f, f, Imgproc.COLOR_BGRA2BGR);
					
					vwriter.write(f);
				}

			}
	}

	public void initialize()
	{
		// load the feed names
		feedList1.getItems().addAll(VideoInput.feedNames);
		feedList2.getItems().addAll(VideoInput.feedNames);
		feedList3.getItems().addAll(VideoInput.feedNames);
		feedList4.getItems().addAll(VideoInput.feedNames);

		// bind labels from the UI
		trackLblProp1 = new SimpleObjectProperty<>();
		this.trackLbl1.textProperty().bind(trackLblProp1);
		trackLblProp2 = new SimpleObjectProperty<>();
		this.trackLbl2.textProperty().bind(trackLblProp2);
		trackLblProp3 = new SimpleObjectProperty<>();
		this.trackLbl3.textProperty().bind(trackLblProp3);
		trackLblProp4 = new SimpleObjectProperty<>();
		this.trackLbl4.textProperty().bind(trackLblProp4);
		
		cameraFeeds[0] = new CameraFeedDisplay(imgOut1, selFeedBtn1, feedList1, Direction.NORTH);
		cameraFeeds[1] = new CameraFeedDisplay(imgOut2, selFeedBtn2, feedList2, Direction.SOUTH);
		cameraFeeds[2] = new CameraFeedDisplay(imgOut3, selFeedBtn3, feedList3, Direction.EAST);
		cameraFeeds[3] = new CameraFeedDisplay(imgOut4, selFeedBtn4, feedList4, Direction.WEST);

		// create trackers for each camera feed
		// the direction is the heading that an oncoming vehicle would have,
		// so if the camera is facing north, the direction would be south etc.
		tracker[0] = new Tracker((float)TrackerConfig._dt,
				(float)TrackerConfig._Accel_noise_mag,
				TrackerConfig._dist_thres,
				TrackerConfig._maximum_allowed_skipped_frames,
				TrackerConfig._max_trace_length,
				TrackerConfig._max_sec_before_stale,
				Direction.SOUTH);
		tracker[1] = new Tracker((float)TrackerConfig._dt,
				(float)TrackerConfig._Accel_noise_mag,
				TrackerConfig._dist_thres,
				TrackerConfig._maximum_allowed_skipped_frames,
				TrackerConfig._max_trace_length,
				TrackerConfig._max_sec_before_stale,
				Direction.NORTH);
		tracker[2] = new Tracker((float)TrackerConfig._dt,
				(float)TrackerConfig._Accel_noise_mag,
				TrackerConfig._dist_thres,
				TrackerConfig._maximum_allowed_skipped_frames,
				TrackerConfig._max_trace_length,
				TrackerConfig._max_sec_before_stale,
				Direction.WEST);
		tracker[3] = new Tracker((float)TrackerConfig._dt,
				(float)TrackerConfig._Accel_noise_mag,
				TrackerConfig._dist_thres,
				TrackerConfig._maximum_allowed_skipped_frames,
				TrackerConfig._max_trace_length,
				TrackerConfig._max_sec_before_stale,
				Direction.EAST);
		
		// load the darknet yolo network
		try {
			yolo = new YoloDetector("yolo/cfg/yolov2-custom.cfg",
					"yolo/weights/yolov2-tiny-obj_10500.weights",
					"yolo/classes/custom-training.names");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// subscribe observers to listen for traffic updates 
		trafficObserver = new UITrackObserver();
		TrackUpdateObservable.getInstance().addObserver(trafficObserver);
		
		// start the frame grabber thread
		frameGrabber.start();
	}

	@FXML
	private void startFeed(ActionEvent event)
	{
		String btnId = ((Button)event.getSource()).getId();
		
		switch (btnId)
		{
			case "selFeedBtn1":
				cameraFeeds[0].selectNewFeed();
				break;
			case "selFeedBtn2":
				cameraFeeds[1].selectNewFeed();
				break;
			case "selFeedBtn3":
				cameraFeeds[2].selectNewFeed();
				break;
			case "selFeedBtn4":
				cameraFeeds[3].selectNewFeed();
				break;
			default:
				break;
		}
	}


	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Mat processFrame(Mat frame, Tracker tracker, RoadLinesCollection roadLines)
	{
		Mat curFrame = new Mat();

		// used this https://www.pyimagesearch.com/2015/05/25/basic-motion-detection-and-tracking-with-python-and-opencv/
		// http://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_objdetect/py_table_of_contents_objdetect/py_table_of_contents_objdetect.html
		// https://github.com/sgjava/install-opencv/blob/master/opencv-java/src/com/codeferm/opencv/PeopleDetect.java

		frame.copyTo(curFrame);

		Mat output = new Mat();

		logMsg("Processing frame");

		imag = curFrame.clone();
		orgin = curFrame.clone();
		kalman = curFrame.clone();
		

		// Tracker code
		// https://github.com/Franciscodesign/Moving-Target-Tracking-with-OpenCV/blob/master/src/sonkd/Main.java
		Vector<DetectedObject> rects = yolo.detectObjectYolo(curFrame);
		
		logMsg("Updating tracker");
			
		// update the tracker if there are detected objects,
		// otherwise update the kalman filter
		if (rects.size() > 0)
		{
			tracker.update(rects, output);
		}
		else
		{
			tracker.updateKalman(imag, rects);
			tracker.checkForStaleTracks();
		}

		logMsg("Drawing detects");
		
		for (int i = 0; i < tracker.tracks.size(); i++)
		{
			// if there is a trace available, draw it on the image
			if (drawTrace)
			{
				int traceNum = tracker.tracks.get(i).trace.size();
				if (traceNum > 1)
				{
					for (int jt = 1; jt < tracker.tracks.get(i).trace.size(); jt++)
					{
						Imgproc.line(
								imag, 
								tracker.tracks.get(i).trace.get(jt - 1), 
								tracker.tracks.get(i).trace.get(jt), 
								TrackerConfig.Colors[tracker.tracks.get(i).track_id % 9],
								2, 4, 0);
	
					}
				}
			}

			tracker.tracks.get(i).drawDetect(imag, false, true, drawTrace);
			
			// determine the lane that the car is in
			tracker.tracks.get(i).lane = roadLines.isInLane(tracker.tracks.get(i).getBestPositionCenter());
			
		}
		
		roadLines.drawLanes(imag);
		
		return imag;

	}
	
	

	
	
	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
		for (int i = 0; i < 4; i++)
		{
			cameraFeeds[i].cleanup();
		}
	}

	/**
	 * On application close, stop the acquisition from the camera
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
		vwriter.release();
	}
	
	private boolean printLogs = false;
	private void logMsg(String format, String ... args)
	{
		if (printLogs)
			System.out.printf(format + "\n", args);
	}


	private Point mouseDownPt = new Point();
	private Point mouseUpPt = new Point();
	private boolean primaryButtonDown = false;
	@FXML
	private void imgViewMouseDown(MouseEvent event)
	{
		if (event.getSource().getClass() != ImageView.class)
		{
			System.out.printf("Mouse down on type %s, expecting %s",
					event.getSource().getClass().getSimpleName(),
					ImageView.class.getClass().getSimpleName());
			return;
		}
			
		String id = ((ImageView)event.getSource()).getId();
		
		if (event.isPrimaryButtonDown())
		{
			mouseDownPt.x = event.getX() + 10;
			mouseDownPt.y = event.getY() + 10;
			
			primaryButtonDown = true;
			
			System.out.printf("Mouse down on %s at (%f, %f)\n",
					id, mouseDownPt.x, mouseDownPt.y);
		}
	}
	
	@FXML
	private void imgViewMouseUp(MouseEvent event)
	{
		if (event.getSource().getClass() != ImageView.class)
		{
			System.out.printf("Mouse up on type %s, expecting %s",
					event.getSource().getClass().getSimpleName(),
					ImageView.class.getClass().getSimpleName());
			return;
		}
			
		String id = ((ImageView)event.getSource()).getId();
		
		if (primaryButtonDown)
		{
			mouseUpPt.x = event.getX() + 10;
			mouseUpPt.y = event.getY() + 10;
			
			System.out.printf("Mouse down on %s at (%f, %f)\n",
					id, mouseUpPt.x, mouseUpPt.y);
			
			switch (id)
			{
				case "imgOut1":
					cameraFeeds[0].getRoadLines().addLane(mouseDownPt, mouseUpPt);
					break;
				case "imgOut2":
					cameraFeeds[1].getRoadLines().addLane(mouseDownPt, mouseUpPt);
					break;
				case "imgOut3":
					cameraFeeds[2].getRoadLines().addLane(mouseDownPt, mouseUpPt);
					break;
				case "imgOut4":
					cameraFeeds[3].getRoadLines().addLane(mouseDownPt, mouseUpPt);
					break;
			}
			
			primaryButtonDown = false;
		}
		else
		{
			// other than the primary button, delete the last line
			switch (id)
			{
				case "imgOut1":
					cameraFeeds[0].getRoadLines().removeLastLane();
					break;
				case "imgOut2":
					cameraFeeds[1].getRoadLines().removeLastLane();
					break;
				case "imgOut3":
					cameraFeeds[2].getRoadLines().removeLastLane();
					break;
				case "imgOut4":
					cameraFeeds[3].getRoadLines().removeLastLane();
					break;
			}
		}
	}
	
}
