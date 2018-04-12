package application;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import observer.TrackUpdateObservable;
import observer.UITrackObserver;
import tracking.TrackerConfig;
import tracking.Tracker;

public class SystemUIController {

	// setup the FXML control accessors
	@FXML
	private Button selFeedBtn;
	@FXML
	private ImageView imageOutputImgvw;
	@FXML
	private ComboBox<String> feedListCbx;
	@FXML
	private Label trackLbl;
	public static ObjectProperty<String> trackLblProp;
	@FXML
	private ImageView img1vb, img2vb, img3vb, img4vb;
	
	@FXML 
	Slider rho_s, threshold_s, minLineLength_s, maxLineGap_s;
	@FXML
	private Label houghLbl;
	public static ObjectProperty<String> houghLblProp;

	private VideoInput videoFeed = new VideoInput();

	private ScheduledExecutorService timer;

	boolean useVocYolo = true;

	static Net yolo;

	Tracker tracker;
	static Mat imag;
	static Mat orgin;
	static Mat kalman;
	
	boolean saveFramesToFile = true;
	boolean firstRun = true;
	long frameCounter = 0;

	UITrackObserver trafficObserver;
	
	float confidenceThreshold  = (float)0.2;	
	float probabilityThreshold = (float)0.2;
	boolean drawTrace = true;
	boolean extrapDetects = true;
	
	boolean testCaffe = false;
	static Net caffe;

	public void initialize()
	{
		// load the feed names
		feedListCbx.getItems().addAll(VideoInput.feedNames);

		// bind labels from the UI
		trackLblProp = new SimpleObjectProperty<>();
		this.trackLbl.textProperty().bind(trackLblProp);
		
		houghLblProp = new SimpleObjectProperty<>();
		this.houghLbl.textProperty().bind(houghLblProp);

		// create a new tracker for the detected objects
		tracker = new Tracker((float)TrackerConfig._dt,
				(float)TrackerConfig._Accel_noise_mag,
				TrackerConfig._dist_thres,
				TrackerConfig._maximum_allowed_skipped_frames,
				TrackerConfig._max_trace_length,
				TrackerConfig._max_sec_before_stale);

		// load the darknet yolo network
		if (useVocYolo)
		{
			yolo = Dnn.readNetFromDarknet(
					"yolo/cfg/tiny-yolo-voc.cfg",
					"yolo/weights/tiny-yolo-voc.weights");
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
				DetectedObject.loadClassNames("yolo/classes/voc.names");
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
		
		
		// try the caffe
		if (testCaffe)
		{
			try {
				DetectedObject.loadClassNames("caffe/synset_words.txt");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			caffe = Dnn.readNetFromCaffe(
					"caffe/bvlc_googlenet.prototxt.txt", 
					"caffe/bvlc_googlenet.caffemodel");
		}
	}

	@FXML
	private void startFeed()
	{
		// get the current feed name
		String feedName = feedListCbx.getValue();

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
		if (this.videoFeed.isOpened())
		{
			
			Runnable frameGrabber = new Runnable() {

				@Override
				public void run()
				{
					// effectively grab and process a single frame
					Mat frame = new Mat();

					try {
						frame = videoFeed.grabFrame();
						
						//System.out.println(frame.size().height);
						//System.out.println(frame.size().width);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					if (saveFramesToFile)
					{
						
						if (firstRun)
						{
							Path dir = FileSystems.getDefault().getPath("H:\\CarImages");
							DirectoryStream<Path> stream;
							long maxFileCount = 0;
							try {
								stream = Files.newDirectoryStream( dir );
							
							String fileName;
						      for (Path path : stream) {
						    	  fileName = path.getFileName().toString();
						    	  fileName = fileName.substring(fileName.indexOf('_')+1, fileName.indexOf('.'));  
						    	  maxFileCount = Math.max(maxFileCount, Integer.parseInt(fileName));
						      }
						      stream.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							frameCounter = ++maxFileCount;
							firstRun = false;
						}
						
						if (++frameCounter % 10 == 0)
						{
							
							try {
								BufferedImage bi = Utils.matToBufferedImage(frame);
								ImageIO.write(bi, "jpg", new File("H:\\CarImages\\img_" + frameCounter + ".jpg"));
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}

					frame = processFrame(frame);

					// convert and show the frame
					Image imageToShow = Utils.mat2Image(frame);
					updateImageView(imageOutputImgvw, imageToShow);
				}
			};

			// grab a frame every 100 ms (~30 frames/sec)
			this.timer = Executors.newSingleThreadScheduledExecutor();
			this.timer.scheduleAtFixedRate(frameGrabber, 0, 50, TimeUnit.MILLISECONDS);

			// update the button content
			this.selFeedBtn.setText("Select Video Feed");
		}
		else
		{
			// log the error
			System.err.println("Failed to open the camera connection...");
		}
	}


	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Mat processFrame(Mat frame)
	{
		Mat curFrame = new Mat();

		// used this https://www.pyimagesearch.com/2015/05/25/basic-motion-detection-and-tracking-with-python-and-opencv/
		// http://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_objdetect/py_table_of_contents_objdetect/py_table_of_contents_objdetect.html
		// https://github.com/sgjava/install-opencv/blob/master/opencv-java/src/com/codeferm/opencv/PeopleDetect.java

		frame.copyTo(curFrame);

		Mat output = new Mat();

		

		imag = curFrame.clone();
		orgin = curFrame.clone();
		kalman = curFrame.clone();
		
		detectLanes(imag);
		
		// try caffe
		if (testCaffe)
		{
			// get blobs
			Mat inputBlob = Dnn.blobFromImage(imag, 1.0, new Size(224,224), new Scalar(104,117,123), true, true);
			caffe.setInput(inputBlob, "data");
			Mat prob = caffe.forward("prob");
			
			updateImageView(img1vb, Utils.mat2Image(inputBlob));
			
			for (int i = 0; i < 5; i++)
			{
				caffe.setInput(inputBlob, "data");
				prob = caffe.forward("prob");
			}
			
			Mat probMat = prob.reshape(1, 1);
			MinMaxLocResult res = Core.minMaxLoc(prob);
			
			double classProb = res.maxVal;
			double classId = res.maxLoc.x;
			
			System.out.printf("Prob %.0f: %s\n",
					classProb * 100,
					DetectedObject.classes.get((int)classId));
		}
		
		
if (!testCaffe)
{
	
	// Tracker code
			// https://github.com/Franciscodesign/Moving-Target-Tracking-with-OpenCV/blob/master/src/sonkd/Main.java
			Vector<DetectedObject> rects = detectObjectYolo(curFrame);
			
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
				double fontScale = .4;
				int thickness = 1;
				int xPixelStep = 20;
				int[] baseline = new int[] {0};
				Size fontSize = Imgproc.getTextSize(tracker.tracks.get(i).getDirectionToString(), 
						Core.FONT_HERSHEY_SIMPLEX, fontScale, thickness, baseline);
				Size boxSize = new Size(fontSize.width + 20, fontSize.height * 4);
				
				// draw the bounding box around the detect
				Imgproc.rectangle(
						imag,
						lb,
						rt, 
						TrackerConfig.Colors[tracker.tracks.get(i).track_id % 9],
						5);
				
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
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		// update the tracks in the traffic observer
		//TrafficUpdateObservable.getInstance().updateTracks(tracker.tracks);

	}
		
		return imag;

	}

	private Vector<DetectedObject> detectObjectYolo(Mat inputMat)
	{
		Vector<DetectedObject> rects = new Vector<DetectedObject>();

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
		
		String str = String.format(
				"rho: %f\nthreshold: %f\nminLineLength: %f\nmaxLineGap: %f",
				Math.ceil(rho_s.getValue()),
				Math.ceil(threshold_s.getValue()),
				Math.ceil(minLineLength_s.getValue()),
				Math.ceil(maxLineGap_s.getValue()));
		
		Utils.onFXThread(SystemUIController.houghLblProp, str);
				
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
		
		// show the canny image
		updateImageView(img1vb, Utils.mat2Image(grayFrame));
	}
	
	// http://jeffwen.com/2017/02/23/lane_finding
	private Mat colorSelection(Mat frame)
	{
		Mat hlsImg = new Mat();
		frame.copyTo(hlsImg);
		
		Imgproc.cvtColor(hlsImg, hlsImg, Imgproc.COLOR_RGB2HLS);
		
		Mat whiteColor = new Mat();
		Core.inRange(frame, new Scalar(220,220,220), new Scalar(255,255,255), whiteColor);
		
		updateImageView(img2vb, Utils.mat2Image(whiteColor));
		
		Mat yellowColorRgb = new Mat();
		Core.inRange(frame, new Scalar(225,180,0), new Scalar(255,255,170), yellowColorRgb);
		
		updateImageView(img3vb, Utils.mat2Image(yellowColorRgb));
		
		Mat yellowColorHls = new Mat();
		Core.inRange(hlsImg, new Scalar(20,120,80), new Scalar(45,200,255), yellowColorHls);
		
		updateImageView(img4vb, Utils.mat2Image(yellowColorHls));
		
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
			// release the video feed
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
