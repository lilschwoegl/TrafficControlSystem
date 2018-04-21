package application;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import observer.TrackUpdateObservable;
import observer.UITrackObserver;
import simulator.Constants.Direction;
import tracking.TrackerConfig;
import tracking.Tracker;

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

	boolean useVocYolo = true;

	static Net yolo;

	Tracker tracker[] = new Tracker[4];
	static Mat imag;
	static Mat orgin;
	static Mat kalman;
	
	boolean saveFramesToFile = false;
	boolean firstRun = true;
	long frameCounter = 0;

	UITrackObserver trafficObserver;
	
	float confidenceThreshold  = (float)0.45;	
	float probabilityThreshold = (float)0.45;
	boolean drawTrace = false;
	boolean extrapDetects = true;
	
	VideoWriter vwriter = new VideoWriter();
	boolean recordVideo = true;
	
	private Thread frameGrabber = new Thread()
	{
		int counter = 0;
		public void run()
		{
			while(true)
			{
				if (cameraFeeds[counter].isAlive())
				{
					boolean staleFrame = cameraFeeds[counter].isFrameStale();
					Mat frame = cameraFeeds[counter].getLastFrame();
					
					if (!frame.empty() && frame.width() > 0 && frame.height() > 0)
					{
						if (!staleFrame)
							saveFrame(frame);
						
						frame = processFrame(frame, tracker[counter]);
					}
					
					cameraFeeds[counter].showImage(frame);
	
					try {
						sleep(25);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				counter = (++counter % 4);
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
		if (useVocYolo)
		{
			yolo = Dnn.readNetFromDarknet(
					//"yolo/cfg/tiny-yolo-voc.cfg",
					"yolo/cfg/yolov2-custom.cfg",
					//"yolo/weights/tiny-yolo-voc.weights");
					"yolo/weights/yolov2-tiny-obj_10500.weights");
		}
		else
		{
			yolo = Dnn.readNetFromDarknet(
					"yolo/cfg/tiny-yolo.cfg",
					"yolo/weights/tiny-yolo.weights");
		}

		try {
			// load the yolo class names
			if (useVocYolo)
			{
//				DetectedObject.loadClassNames("yolo/classes/voc.names");
				DetectedObject.loadClassNames("yolo/classes/custom-training.names");
			}
			else
			{
				DetectedObject.loadClassNames("yolo/classes/coco.names");
			}
		} catch (IOException e) {
			System.err.println("Error loading class names for YOLO...");
			return;
		}

		// subscribe observers to listen for traffic updates 
		trafficObserver = new UITrackObserver();
		TrackUpdateObservable.getInstance().addObserver(trafficObserver);
		
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
		
		
//		String btnId = ((Button)event.getSource()).getId();
//		final VideoInput vin;
//		final String feedName;
//		final ImageView imgOut;
//		Runnable feedRunnable;
//		Thread timer;
//		
//		switch (btnId)
//		{
//			case "selFeedBtn1":
//				vin = videoFeed1;
//				feedName = feedList1.getValue();
//				imgOut = imgOut1;
//				feedRunnable = feedRunner1;
//				timer = timer1;
//				break;
//			case "selFeedBtn2":
//				vin = videoFeed2;
//				feedName = feedList2.getValue();
//				imgOut = imgOut2;
//				feedRunnable = feedRunner2;
//				timer = timer2;
//				break;
//			case "selFeedBtn3":
//				vin = videoFeed3;
//				feedName = feedList3.getValue();
//				imgOut = imgOut3;
//				feedRunnable = feedRunner3;
//				timer = timer3;
//				break;
//			case "selFeedBtn4":
//				vin = videoFeed4;
//				feedName = feedList4.getValue();
//				imgOut = imgOut4;
//				feedRunnable = feedRunner4;
//				timer = timer4;
//				break;
//			default:
//				feedName = "";
//				vin = null;
//				imgOut = null;
//				feedRunnable = null;
//				timer = null;
//				break;
//		}
//		
//		// get the current feed name
//		
//
//		if (feedName.equals(""))
//		{
//			return;
//		}
//
//		// if the feed has changed, get the newly selected one
//		if (!vin.getCurrentFeed().equals(feedName))
//		{
//			vin.selectCameraFeed(feedName);
//		}
//
//		// is the video stream available?
//		if (vin.isOpened())
//		{
//
//			if (timer.isAlive())
//			{
//				timer.stop();
//			}
//			
//			timer = new Thread() {
//
//				private VideoInput videoInput = vin;
//				
//				@Override
//				public void run()
//				{
//					while (true)
//					{
//						// effectively grab and process a single frame
//						Mat frame = new Mat();
//	
//						try {
//							
//							logMsg("Grabbing frame");
//							
//							frame = videoInput.grabFrame();
//							
//							logMsg("Grabbed frame");
//							
//							if (frame.empty() || frame.width() <= 0 || frame.height() <= 0)
//							{
//								logMsg("Got an empty frame!");
//								videoInput.release();
//								videoInput.selectCameraFeed(feedName);
//								frame = videoInput.grabFrame();
//							}
//							
//							//System.out.println(frame.size().height);
//							//System.out.println(frame.size().width);
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//						
//						
//						
//						if (saveFramesToFile)
//						{
//							
//							if (firstRun)
//							{
//								Path dir = FileSystems.getDefault().getPath("H:\\CarImages");
//								DirectoryStream<Path> stream;
//								long maxFileCount = 0;
//								try {
//									stream = Files.newDirectoryStream( dir );
//								
//								String fileName;
//							      for (Path path : stream) {
//							    	  fileName = path.getFileName().toString();
//							    	  fileName = fileName.substring(fileName.indexOf('_')+1, fileName.indexOf('.'));  
//							    	  maxFileCount = Math.max(maxFileCount, Integer.parseInt(fileName));
//							      }
//							      stream.close();
//								} catch (IOException e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}
//	
//								frameCounter = ++maxFileCount;
//								firstRun = false;
//							}
//							
//							if (++frameCounter % 10 == 0)
//							{
//								
//								try {
//									BufferedImage bi = Utils.matToBufferedImage(frame);
//									ImageIO.write(bi, "jpg", new File("H:\\CarImages\\img_" + frameCounter + ".jpg"));
//								} catch (IOException e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}
//							}
//						}
//						
//						if (recordVideo)
//						{
//							if (firstRun)
//							{
//								vwriter.open("C:\\Temp\\intersection_17.avi", VideoWriter.fourcc('D', 'I', 'V', 'X'), 15, frame.size(), true);
//								firstRun = false;
//							}
//							
//							if (frame.width() > 0 && frame.height() > 0)
//							{
//								Mat f = new Mat();
//								Imgproc.resize(frame, f, new Size(frame.width(), frame.height()));
//								if (f.channels() == 4)
//									Imgproc.cvtColor(f, f, Imgproc.COLOR_BGRA2BGR);
//								
//								vwriter.write(f);
//							}
//	
//						}
//	
//						frame = processFrame(frame);
//	
//						// convert and show the frame
//						Image imageToShow = Utils.mat2Image(frame);
//						updateImageView(imgOut, imageToShow);
//						
//						try {
//							// grab a frame every 50 ms
//							sleep(50);
//						} catch (InterruptedException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//					}
//				}
//			};
//
//			// grab a frame every 100 ms (~30 frames/sec)
//			//timer = Executors.newSingleThreadScheduledExecutor();
//			// timer.scheduleAtFixedRate(feedRunnable, 0, 50, TimeUnit.MILLISECONDS);
//			timer.start();
//
//			// update the button content
//			this.selFeedBtn1.setText("Select Video Feed");
//		}
//		else
//		{
//			// log the error
//			System.err.println("Failed to open the camera connection...");
//		}
	}


	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Mat processFrame(Mat frame, Tracker tracker)
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
		
		//detectLanes(imag);
		

	// Tracker code
			// https://github.com/Franciscodesign/Moving-Target-Tracking-with-OpenCV/blob/master/src/sonkd/Main.java
			Vector<DetectedObject> rects = detectObjectYolo(curFrame);
			
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

			try
			{
				// get the last detect for this track
				DetectedObject detect = tracker.tracks.get(i).lastDetect;

				Point lb, rt;

				// if there was a recent detect, use it to draw the bounding box
				// otherwise use the predicted position of the detect
				if (tracker.tracks.get(i).skipped_frames < 1)
				{

					lb = detect.getLeftBot();
					rt = detect.getRightTop();
				}
				else
				{
					if (extrapDetects)
					{
						float width = detect.getWidth();
						float height = detect.getHeight();
						Point lastCenter = tracker.tracks.get(i).getLastCenter();
	
						lb = new Point(lastCenter.x - width / 2, lastCenter.y - height / 2);
						rt = new Point(lastCenter.x + width / 2, lastCenter.y + height / 2);
					}
					else
					{
						lb = detect.getLeftBot();
						rt = detect.getRightTop();
					}
				}

				Scalar fontColor = new Scalar(255,255,255);
				Scalar boxColor = new Scalar(0,0,0);
				int boxThickness = 2;
				double fontScale = .3;
				int thickness = 1;
				int xPixelStep = 20;
				int[] baseline = new int[] {0};
				Size fontSize = Imgproc.getTextSize(tracker.tracks.get(i).getDirectionToString(), 
						Core.FONT_HERSHEY_SIMPLEX, fontScale, thickness, baseline);
				Size boxSize = new Size(fontSize.width + 20, fontSize.height * 4);
				
				boolean drawBox = true;
				boolean drawDetectInfo = false;
				
				if (drawBox) 
				{
					// draw the bounding box around the detect
					Imgproc.rectangle(
							imag,
							lb,
							rt, 
							TrackerConfig.Colors[tracker.tracks.get(i).track_id % 9],
							boxThickness);
				}
				
				if (drawDetectInfo)
				{
					// draw the box to put info in
					Imgproc.rectangle(
							imag,
							lb,
							new Point(lb.x + boxSize.width, lb.y - boxSize.height),
							//CONFIG.Colors[tracker.tracks.get(i).track_id % 9],
							boxColor,
							Core.FILLED
							);
					
					// draw the class and probability of the detect
					Imgproc.putText(
							imag, 
							String.format("%s - %.0f%%", detect.getClassName(), detect.classProb * 100), 
							new Point(lb.x, lb.y - fontSize.height * 3), 
							Core.FONT_HERSHEY_SIMPLEX, 
							fontScale, 
							fontColor,
							thickness);
					
					// draw the direction the detected object is traveling
					Imgproc.putText(
							imag, 
							String.format("%s", tracker.tracks.get(i).getDirectionToString()), 
							new Point(lb.x, lb.y-fontSize.height * 2), 
							Core.FONT_HERSHEY_SIMPLEX, 
							fontScale, 
							fontColor,
							thickness);
					
					// determine teh lane that the car is in
					tracker.tracks.get(i).lane = rlc.isInLane(tracker.tracks.get(i).getBestPositionCenter());
					
					// draw the lane the car is in
					Imgproc.putText(
							imag, 
							String.format("Lane: %d", tracker.tracks.get(i).lane), 
							new Point(lb.x, lb.y-fontSize.height * 1), 
							Core.FONT_HERSHEY_SIMPLEX, 
							fontScale, 
							fontColor,
							thickness);
					
					// draw the track ID
					Imgproc.putText(
							imag, 
							String.format("ID: %d", tracker.tracks.get(i).track_id), 
							new Point(lb.x, lb.y), 
							Core.FONT_HERSHEY_SIMPLEX, 
							fontScale, 
							fontColor,
							thickness);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		// update the tracks in the traffic observer
		//TrafficUpdateObservable.getInstance().updateTracks(tracker.tracks);
		
		return imag;

	}

	private Vector<DetectedObject> detectObjectYolo(Mat inputMat)
	{
		Vector<DetectedObject> rects = new Vector<DetectedObject>();
		
		logMsg("Detecting objects with YOLO");

		Mat m = new Mat();
		inputMat.copyTo(m);

		// darknet yolo only works with 3 channel images
		if (m.channels() == 4)
			Imgproc.cvtColor(m, m, Imgproc.COLOR_BGRA2BGR);

		// darknet yolo wants inputs of size 416x416
		Mat inputBlob = Dnn.blobFromImage(m, (float)(1/255.0), new Size(416, 416), new Scalar(0), false, false);

		// setup the network inputs
		yolo.setInput(inputBlob);

		// http://junkiyoshi.com/tag/dnn/
		Mat detMat = yolo.forward("detection_out");		
	
		for (int i = 0; i < detMat.rows(); i++)
		{
			float confidence = (float)detMat.get(i, 4)[0];

			Mat vals = new Mat(detMat, new Range(i, i+1), new Range(5, DetectedObject.classes.size() + 5));

			int classId;
			double classProb;
			MinMaxLocResult res = Core.minMaxLoc(vals);

			classProb = res.maxVal;
			classId = (int)res.maxLoc.x;

			if (classProb > probabilityThreshold &&
				confidence > confidenceThreshold &&
				DetectedObject.isClassAllowed(classId))
			{

				/* outputs from network are:
				 * 25 columns
				 * 
				 * 0:     x coordinate
				 * 1:     y coordinate
				 * 2:     width
				 * 3:     height
				 * 4:     confidence
				 * 5-end: probabilities for each class
				 */
				
				// read the outputs
				float x = (float)detMat.get(i, 0)[0];
				float y = (float)detMat.get(i, 1)[0];
				float w = (float)detMat.get(i, 2)[0];
				float h = (float)detMat.get(i, 3)[0];

				// the x, y, w, h from the network are relative to the image's width and height,
				// so we need to multiply them by the rows and cols of the image
				float xLeftBot = (x - w / 2) * inputMat.cols();
				float yLeftBot = (y - h / 2) * inputMat.rows();
				float xRightTop = (x + w / 2) * inputMat.cols();
				float yRightTop = (y + h / 2) * inputMat.rows();

				// if the confidence is less than the threshold, ignore it
				if (confidence <= confidenceThreshold)
					continue;

				//System.out.printf("Found a %s with %.2f confidence\n", DetectedObject.classes[classId], confidence);

				// setup the detected object for the tracker
				DetectedObject dob = new DetectedObject();

				dob.classId = classId;
				dob.classProb = classProb;
				dob.confidence = confidence;
				dob.xLeftBot = xLeftBot;
				dob.yLeftBot = yLeftBot;
				dob.xRightTop = xRightTop;
				dob.yRightTop = yRightTop;

				// add to the list of detected objects
				rects.add(dob);

			}
		}
		
		logMsg("Done with YOLO");

		return rects;
	}
	
	private void detectLanes(Mat frame)
	{
		Mat grayFrame = new Mat();
		
		
		frame.copyTo(grayFrame);
		
		
		
		
		grayFrame = colorSelection(grayFrame);
		
		
		// convert to grayscale image
		Imgproc.cvtColor(grayFrame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		
		
		
		
		// apply blur
		Imgproc.GaussianBlur(grayFrame, grayFrame, new Size(3,3), 0);
		
		MatOfPoint mop = new MatOfPoint();
		int c = grayFrame.cols();
		int r = grayFrame.rows();
		mop.fromArray(new Point[] {
				new Point(0,r), 
				new Point(0,r - (r/2)), 
				new Point(c,r - (r/2)), 
				new Point(c,r)
				});
		
		Mat mask = Mat.zeros(grayFrame.size(), grayFrame.type());
		Imgproc.fillConvexPoly(mask, mop, new Scalar(255,0,0));
		
		Core.bitwise_and(grayFrame, mask, grayFrame);
		
		Imgproc.Canny(grayFrame, grayFrame, 50, 150);
		
		Mat lines = new Mat();
		// do hough lines
		
//		Imgproc.HoughLinesP(
//				grayFrame, 
//				lines, 
//				Math.ceil(rho_s.getValue()), 
//				Math.PI / 180, 
//				(int)threshold_s.getValue(), 
//				Math.ceil(minLineLength_s.getValue()), 
//				Math.ceil(maxLineGap_s.getValue()));
		
				
		Imgproc.HoughLinesP(
				grayFrame, 
				lines, 
				2, 
				Math.PI / 180, 
				30, 
				88, 
				49);
		
		
//		Imgproc.HoughLinesP(
//				grayFrame, 
//				lines, 
//				1, 
//				Math.PI / 60, 
//				100, 
//				100, 
//				50);
//				
		drawLines(imag, lines);
		
	}
	
	// http://jeffwen.com/2017/02/23/lane_finding
	private Mat colorSelection(Mat frame)
	{
		Mat hlsImg = new Mat();
		frame.copyTo(hlsImg);
		
		Imgproc.cvtColor(hlsImg, hlsImg, Imgproc.COLOR_RGB2HLS);
		
		Mat whiteColor = new Mat();
		Core.inRange(frame, new Scalar(220,220,220), new Scalar(255,255,255), whiteColor);
		
		Mat yellowColorRgb = new Mat();
		Core.inRange(frame, new Scalar(225,180,0), new Scalar(255,255,170), yellowColorRgb);
		
		Mat yellowColorHls = new Mat();
		Core.inRange(hlsImg, new Scalar(20,120,80), new Scalar(45,200,255), yellowColorHls);
		
		//Mat combined = new Mat();
		//Core.bitwise_or(whiteColor, yellowColor, combined);
		
		
		
		Mat ret = new Mat();
		//Core.bitwise_and(frame, frame, ret, ret);
		
		Core.bitwise_and(frame, frame, ret, whiteColor);
		Core.bitwise_and(frame, frame, ret, yellowColorRgb);
		Core.bitwise_and(frame, frame, ret, yellowColorHls);
		
		return ret;
	}
	
	RoadLinesCollection rlc = new RoadLinesCollection();
	private void drawLines(Mat frame, Mat lines)
	{
		Mat original = new Mat();
		frame.copyTo(original);
		
		Mat lineImg = Mat.zeros(original.size(), original.type());
		
		double data[];
		double slopeThreshold = .5;
		double slope;
		int cols = lines.cols();
		int rows = lines.rows();
		
		Line line;
		for (int i = 0; i < lines.rows(); i+=4)
		{
			data = lines.get(i, 0);
			
			line = new Line(data[0], data[1], data[2], data[3]);
			
			if (Math.abs(line.getSlope()) < slopeThreshold)
			{
				continue;
			}
			
//			Imgproc.putText(
//					frame, 
//					String.format("%.2f", line.getSlope()), 
//					new Point(data[0], data[1]),
//					Core.FONT_HERSHEY_SIMPLEX, 
//					1, 
//					new Scalar(0,255,0),
//					3);
			//Imgproc.line(frame, new Point(data[0], data[1]), new Point(data[2], data[3]), new Scalar(255,0,0), 20);
		
			rlc.coorelateLine(line);
		
		}
		
		rlc.drawLanes(frame);
		
	}


	/**
	 * Stop the acquisition from the camera and release all the resources
	 */
	private void stopAcquisition()
	{
//		if (this.timer1!=null && !this.timer1.isShutdown())
//		{
//			try
//			{
//				// stop the timer
//				this.timer1.shutdown();
//				this.timer1.awaitTermination(33, TimeUnit.MILLISECONDS);
//			}
//			catch (InterruptedException e)
//			{
//				// log any exception
//				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
//			}
//		}

		//if (this.videoFeed1.isOpened())
		//{
			// release the video feed
		//	this.videoFeed1.release();
		//}
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


}
