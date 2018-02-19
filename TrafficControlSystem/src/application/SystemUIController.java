package application;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Mat;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import observer.TrafficUpdateObservable;
import observer.UITrafficObserver;
import tracking.CONFIG;
import tracking.Tracker;

public class SystemUIController {

	@FXML
	private Button selFeedBtn;
	@FXML
	private ImageView imageOutputImgvw;
	@FXML
	private ComboBox<String> feedListCbx;
	@FXML
	private Label trackLbl;
	public static ObjectProperty<String> trackLblProp;

	private VideoInput videoFeed = new VideoInput();

	private ScheduledExecutorService timer;

	boolean useVocYolo = true;

	static Net yolo;

	Tracker tracker;
	static Mat imag;
	static Mat orgin;
	static Mat kalman;
	

	UITrafficObserver trafficObserver;
	
	float confidenceThreshold  = (float)0.1;	
	float probabilityThreshold = (float)0.2;
	boolean drawTrace = true;
	boolean extrapDetects = true;

	public void initialize()
	{
		// load the feed names
		feedListCbx.getItems().addAll(VideoInput.feedNames);

		// bind labels from the UI
		trackLblProp = new SimpleObjectProperty<>();
		this.trackLbl.textProperty().bind(trackLblProp);

		// create a new tracker for the detected objects
		tracker = new Tracker((float)CONFIG._dt,
				(float)CONFIG._Accel_noise_mag,
				CONFIG._dist_thres,
				CONFIG._maximum_allowed_skipped_frames,
				CONFIG._max_trace_length,
				CONFIG._max_sec_before_stale);

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
		trafficObserver = new UITrafficObserver();
		TrafficUpdateObservable.getInstance().addObserver(trafficObserver);
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
					} catch (Exception e) {
						e.printStackTrace();
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

		// Tracker code
		// https://github.com/Franciscodesign/Moving-Target-Tracking-with-OpenCV/blob/master/src/sonkd/Main.java
		Vector<DetectedObject> rects = detectObjectYolo(curFrame);

		imag = curFrame.clone();
		orgin = curFrame.clone();
		kalman = curFrame.clone();
		

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
								CONFIG.Colors[tracker.tracks.get(i).track_id % 9],
								5, 4, 0);
	
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

				// draw the bounding box around the detect
				Imgproc.rectangle(
						imag,
						lb,
						rt, 
						CONFIG.Colors[tracker.tracks.get(i).track_id % 9],
						5);
				
				Imgproc.rectangle(
						imag,
						lb,
						new Point(lb.x + 200, lb.y - 50),
						CONFIG.Colors[tracker.tracks.get(i).track_id % 9],
						Core.FILLED
						);
				
				// draw the class and probability of the detect
				Imgproc.putText(
						imag, 
						String.format("%s - %.0f%%", detect.getClassName(), detect.classProb * 100), 
						new Point(lb.x, lb.y - 30), 
						Core.FONT_HERSHEY_SIMPLEX, 
						1, 
						new Scalar(255,255,255),
						3);
				
				// draw the direction the detected object is traveling
				Imgproc.putText(
						imag, 
						String.format("%s", tracker.tracks.get(i).getDirectionToString()), 
						new Point(lb.x, lb.y), 
						Core.FONT_HERSHEY_SIMPLEX, 
						1, 
						new Scalar(255,255,255),
						3);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		// update the tracks in the traffic observer
		TrafficUpdateObservable.getInstance().updateTracks(tracker.tracks);

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

			if (classProb > probabilityThreshold)
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
