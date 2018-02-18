package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.SVM;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.objdetect.Objdetect;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tracking.CONFIG;
import tracking.Track.DIRECTION;
import tracking.Tracker;

public class SystemUIController {

	@FXML
	private Button selFeedBtn;
	@FXML
	private ImageView imageOutputImgvw;
	@FXML
	private ImageView imgv1, imgv2, subMatImg;
	@FXML
	private ComboBox<String> feedListCbx;
	@FXML
	private Slider blurSlide, dilationSlide, erodeSlide, threshSlide, minCSlide;
	@FXML
	private Label slideProp;
	@FXML
	private Label trackLbl;
	public static ObjectProperty<String> trackLblProp;
	@FXML
	private CheckBox saveImagesCbx;

	private String savedImagesPath = "D:\\Car Images\\";
	private int imgCounter = 0;

	private String carsCascadeName = "haar/cars.xml";

	private ObjectProperty<String> slideValuesProp;

	private double blurValue, dilationValue, erodeValue, threshValue, minCValue;

	private VideoInput videoFeed = new VideoInput();

	private ScheduledExecutorService timer;

	CascadeClassifier detector;
	HOGDescriptor hog;
	
	static Net yolo = Dnn.readNetFromDarknet(
			"yolo/cfg/tiny-yolo-voc.cfg",
			"yolo/weights/tiny-yolo-voc.weights");
//	static Net yolo = Dnn.readNetFromDarknet(
//		"D:\\darknet\\darknet\\cfg\\tiny-yolo.cfg",
//		"D:\\darknet\\weights\\tiny-yolo.weights");


	// this will initialize and update every 500 msec
	private Mat backgroundFrame;
	private int counter = 0;

	BackgroundSubtractorMOG2 backSub;
	Tracker tracker;
    static Mat imag;
    static Mat orgin;
    static Mat kalman;
	boolean firstTime = true;
	static Mat outbox = new Mat();
	static Mat diffFrame = null;

	UITrafficObserver trafficObserver = new UITrafficObserver();
	
	// a flag to change the button behavior
	private boolean cameraActive;

	public void initialize()
	{
		// load the feed names
		feedListCbx.getItems().addAll(VideoInput.feedNames);

		detector = new CascadeClassifier(carsCascadeName);
		
		hog = new HOGDescriptor();
		hog.load("hog/my_detector.yml");

		slideValuesProp = new SimpleObjectProperty<>();
		this.slideProp.textProperty().bind(slideValuesProp);
		
		trackLblProp = new SimpleObjectProperty<>();
		this.trackLbl.textProperty().bind(trackLblProp);
		
		backSub = Video.createBackgroundSubtractorMOG2();
		tracker = new Tracker((float)CONFIG._dt,
				(float)CONFIG._Accel_noise_mag,
				CONFIG._dist_thres,
				CONFIG._maximum_allowed_skipped_frames,
				CONFIG._max_trace_length);
		
		DetectedObject.loadClassNames("yolo/classes/voc.names");
//		DetectedObject.loadClassNames("D:\\darknet\\darknet\\data\\coco.names");
		
		TrafficUpdateObserver.getInstance().addObserver(trafficObserver);

		Utils.onFXThread(this.slideValuesProp, readUIControls());
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
					Mat frame = new Mat();

					try {
						frame = videoFeed.grabFrame();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

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


	/**
	 * Get a frame from the opened video stream (if any)
	 * 
	 * @return the {@link Image} to show
	 */
	private Mat processFrame(Mat frame)
	{
		Mat grayFrame = new Mat();
		Mat curFrame = new Mat();

		// used this https://www.pyimagesearch.com/2015/05/25/basic-motion-detection-and-tracking-with-python-and-opencv/
		// http://opencv-python-tutroals.readthedocs.io/en/latest/py_tutorials/py_objdetect/py_table_of_contents_objdetect/py_table_of_contents_objdetect.html
		// https://github.com/sgjava/install-opencv/blob/master/opencv-java/src/com/codeferm/opencv/PeopleDetect.java
		
		frame.copyTo(curFrame);


		Utils.onFXThread(this.slideValuesProp, readUIControls());

		// convert colors and remove blur
		//Imgproc.resize(frame, frame, new Size(500,500));
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		//Imgproc.GaussianBlur(grayFrame, grayFrame, new Size(blurValue,blurValue), 0);
		updateImageView(imgv1, Utils.mat2Image(grayFrame));

		if (++counter % 2 == 0 || backgroundFrame == null)
		{
			try {
				backgroundFrame = videoFeed.grabFrame();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// convert colors and remove blur
			//Imgproc.resize(backgroundFrame, backgroundFrame, new Size(500,500));
			Imgproc.cvtColor(backgroundFrame, backgroundFrame, Imgproc.COLOR_BGR2GRAY);

			Imgproc.GaussianBlur(backgroundFrame, backgroundFrame, new Size(blurValue,blurValue), 0);
		}

		Mat output = new Mat();


		Core.absdiff(backgroundFrame, grayFrame, output);

		//Mat output = new Mat();
		//grayFrame.copyTo(output, fgMask);

//		Imgproc.threshold(output, output, threshValue, 255, Imgproc.THRESH_BINARY);
//
//
//		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilationValue, dilationValue));
//		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(erodeValue, erodeValue));
//
//
//		Imgproc.erode(output, output, erodeElement);
//		Imgproc.erode(output, output, erodeElement);
//		Imgproc.dilate(output, output, dilateElement);
//		Imgproc.dilate(output, output, dilateElement);
//
//
//		updateImageView(imgv2, Utils.mat2Image(output));

		
		
		int testCase = 8;
		if (testCase == 1)
		{
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

					if (Imgproc.contourArea(contours.get(idx)) < minCValue)
					{
						System.out.println("Skipping one...");
						continue;
					}

					Rect r = Imgproc.boundingRect(contours.get(idx));

//					try
//					{
//						// check cars classifier
//						MatOfRect cars = new MatOfRect();
//
//						detector.detectMultiScale(
//								curFrame.submat(new Rect(r.x - 50, r.y - 50, r.width + 50, r.height + 50))
//								, cars, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
//								new Size(20, 20), new Size());
//
//						Rect[] carsArray = cars.toArray();
//
//						if (carsArray.length <= 0)
//							continue;
//					}
//					catch (Exception e)
//					{
//						continue;
//					}

					Imgproc.rectangle(curFrame, 
							new Point(r.x-2, r.y-2),
							new Point(r.x + r.width + 2, r.y + r.height + 2),
							new Scalar(0, 250, 0));

					if (idx == 0)
					{
						Mat subMat = curFrame.submat(r);
						Imgproc.resize(subMat, subMat, new Size(100,100));

						recognizeVehicle(subMat);
					}
				}
			}
		}	
		else if (testCase == 2)
		{
			try {
				ImageIO.write(Utils.matToBufferedImage(frame), "jpeg", 
						new File(savedImagesPath + "cars_" + imgCounter + ".jpeg"));
				imgCounter++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (testCase == 3)
		{
			// https://www.programcreek.com/java-api-examples/index.php?source_dir=DrawDroid-master/PaintHelper/opencv/src/org/opencv/objdetect/HOGDescriptor.java
			// http://answers.opencv.org/question/60555/vehicle-and-other-objects-in-hogdescriptor/
			// https://medium.com/@mithi/vehicles-tracking-with-hog-and-linear-svm-c9f27eaf521a
			// http://www.gti.ssr.upm.es/data/Vehicle_database.html
			// https://github.com/ozcanovunc/opencv-samples/tree/master/vehicle-detection-hog
			
			Mat hogMat = new Mat();
			MatOfRect foundLocations = new MatOfRect();
			MatOfDouble foundWeights = new MatOfDouble();
			Size winStride = new Size(8, 8);
	        Size padding = new Size(32, 32);
	        Point rectPoint1 = new Point();
	        Point rectPoint2 = new Point();
			
	        hog.detectMultiScale(curFrame, foundLocations, foundWeights, 0,
	        		winStride, padding, 1.5, 2.0, false);
	        
	        //hog.detectMultiScale(curFrame, foundLocations, foundWeights);
	        
	        if (foundLocations.rows() > 0)
	        {
	        	final List<Double> weightList = foundWeights.toList();
                final List<Rect> rectList = foundLocations.toList();
                int index = 0;
                for (final Rect rect : rectList) {
                    rectPoint1.x = rect.x;
                    rectPoint1.y = rect.y;
                    rectPoint2.x = rect.x + rect.width;
                    rectPoint2.y = rect.y + rect.height;
                    // Draw rectangle around fond object
                    Imgproc.rectangle(curFrame, rectPoint1, rectPoint2, new Scalar(0, 250, 0), 2);
                    index++;
                }
	        }
		}
		else if (testCase == 4)
		{
			final String[] classes = new String[] {
					"aeroplane",
					"bicycle",
					"bird",
					"boat",
					"bottle",
					"bus",
					"car",
					"cat",
					"chair",
					"cow",
					"diningtable",
					"dog",
					"horse",
					"motorbike",
					"person",
					"pottedplant",
					"sheep",
					"sofa",
					"train",
					"tvmonitor"
			};
			
			
			
			Mat m = new Mat();
			curFrame.copyTo(m);
			
			Imgproc.resize(m, m, new Size(416,416));
			
			if (m.channels() == 4)
				Imgproc.cvtColor(m, m, Imgproc.COLOR_BGRA2BGR);
			
			Mat inputBlob = Dnn.blobFromImage(m, (float)(1/255.0), new Size(416, 416), new Scalar(0), false, false);
			
			yolo.setInput(inputBlob);
			
			// http://junkiyoshi.com/tag/dnn/
			Mat detMat = yolo.forward("detection_out");
			
			List<String> layers = yolo.getLayerNames();
			
			
			float confidenceThreshold = (float)0.1;		
			for (int i = 0; i < detMat.rows(); i++)
			{
				float confidence = (float)detMat.get(i, 4)[0];
				
				Mat vals = new Mat(detMat, new Range(i, i+1), new Range(5, 25));
				
				int classId;
				double classProb;
				MinMaxLocResult res = Core.minMaxLoc(vals);
				
				classProb = res.maxVal;
				classId = (int)res.maxLoc.x;
				
				if (classProb > .3)
				{
				
					float x = (float)detMat.get(i, 0)[0];
					float y = (float)detMat.get(i, 1)[0];
					float w = (float)detMat.get(i, 2)[0];
					float h = (float)detMat.get(i, 3)[0];
					
					float xLeftBot = (x - w / 2) * curFrame.cols();
					float yLeftBot = (y - h / 2) * curFrame.rows();
					float xRightTop = (x + w / 2) * curFrame.cols();
					float yRightTop = (y + h / 2) * curFrame.rows();
					

					if (confidence <= confidenceThreshold)
						continue;
						
					//System.out.println("");
					//System.out.printf("%f %f %f %f %f\n", xLeftBot, yLeftBot, xRightTop, yRightTop, confidence);
					System.out.printf("Found a %s with %.2f confidence\n", classes[classId], confidence);
					
					Imgproc.putText(
							curFrame, 
							String.format("%s - %.2f", classes[classId], classProb * 100), 
							new Point(xLeftBot, yLeftBot), 
							Core.FONT_HERSHEY_SIMPLEX, 
							2, 
							new Scalar(255,0,0),
							3);
					
					Imgproc.rectangle(curFrame, 
							new Point(xLeftBot, yLeftBot), 
							new Point(xRightTop, yRightTop), 
							new Scalar(0,255,0),
							5);
				}
			}
			
		}
		else if (testCase == 5)
		{
			try {
				ImageIO.write(Utils.matToBufferedImage(frame), "jpeg", 
						new File(savedImagesPath + "cars_" + imgCounter + ".jpeg"));
				imgCounter++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (testCase == 6)
		{
			final Net caffe = Dnn.readNetFromCaffe(
					"D:\\darknet\\caffe\\bvlc_googlenet.prototxt", 
					"D:\\darknet\\caffe\\bvlc_googlenet.caffemodel");
			
			Mat img = new Mat();
			curFrame.copyTo(img);
			
			Imgproc.resize(img, img, new Size(224, 224));
			
			Mat inputBlob = Dnn.blobFromImage(img, 1, new Size(224, 224), new Scalar(104,117,123), false, false);
			caffe.setInput(inputBlob);
			
			
			Mat prob = new Mat();
			
			for (int i = 0; i < 2; i++)
			{
				 prob = caffe.forward("prob");
			}
			
			Mat probMat = new Mat();
			prob.copyTo(probMat);
			probMat = probMat.reshape(1, 1);
			
			int classId;
			double classProb;
			MinMaxLocResult res = Core.minMaxLoc(prob);
			
			classProb = res.maxVal;
			classId = (int)res.maxLoc.x;
			
			List<String> layers = caffe.getLayerNames();
			
			//if (classProb > .2)
			//	System.out.printf("Class: %s, Confidence: %f\n", classNames.get(classId), classProb);
		}
		else if (testCase == 7)
		{
			Mat m = new Mat();
			grayFrame.copyTo(m);
			
			MatOfRect mOut = new MatOfRect();
			detector.detectMultiScale(m, mOut, 1.1, 1, 0, new Size(0,0), new Size(300,300));
			
			if (mOut.rows() > 0)
	        {
				Point rectPoint1 = new Point();
				Point rectPoint2 = new Point();
                final List<Rect> rectList = mOut.toList();
                int index = 0;
                for (final Rect rect : rectList) {
                    rectPoint1.x = rect.x;
                    rectPoint1.y = rect.y;
                    rectPoint2.x = rect.x + rect.width;
                    rectPoint2.y = rect.y + rect.height;
                    // Draw rectangle around fond object
                    Imgproc.rectangle(curFrame, rectPoint1, rectPoint2, new Scalar(0, 250, 0), 2);
                    index++;
                    
//                    
                    
                }
	        }
		}
		else if (testCase == 8)
		{
			// Tracker code
			// https://github.com/Franciscodesign/Moving-Target-Tracking-with-OpenCV/blob/master/src/sonkd/Main.java
			Vector<DetectedObject> rects = detectObjectYolo(curFrame);
			
			imag = curFrame.clone();
			orgin = curFrame.clone();
			kalman = curFrame.clone();
			
			if (firstTime)
			{
				diffFrame = new Mat(outbox.size(), CvType.CV_8UC1);
				diffFrame = outbox.clone();
				firstTime = false;
			}
			
			diffFrame = new Mat(curFrame.size(), CvType.CV_8UC1);
			
			if (rects.size() > 0)
			{
				tracker.update(rects, output);
				
//				for (DetectedObject det : rects)
//				{
//					Imgproc.putText(
//							imag, 
//							String.format("%s - %.2f", det.getClassName(), det.classProb * 100), 
//							new Point(det.xLeftBot, det.yLeftBot - 10), 
//							Core.FONT_HERSHEY_SIMPLEX, 
//							1, 
//							new Scalar(255,0,0),
//							3);
//					
//					Imgproc.rectangle(imag, 
//							new Point(det.xLeftBot, det.yLeftBot), 
//							new Point(det.xRightTop, det.yRightTop), 
//							new Scalar(0,255,0),
//							5);
//				}
			}
			else
			{
				tracker.updateKalman(imag, rects);
			}
				
			for (int i = 0; i < tracker.tracks.size(); i++)
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
				
				try
				{
					DetectedObject detect = tracker.tracks.get(i).lastDetect;
					
					Point lb, rt;
					
					if (tracker.tracks.get(i).skipped_frames < 1)
					{

						lb = detect.getLeftBot();
						rt = detect.getRightTop();
					}
					else
					{
						float width = detect.getWidth();
						float height = detect.getHeight();
						Point lastCenter = tracker.tracks.get(i).getLastCenter();
						
						lb = new Point(lastCenter.x - width / 2, lastCenter.y - height / 2);
						rt = new Point(lastCenter.x + width / 2, lastCenter.y + height / 2);
					
					}
					
					Imgproc.rectangle(
							imag,
							lb,
							rt, 
							CONFIG.Colors[tracker.tracks.get(i).track_id % 9],
							5);
					Imgproc.putText(
							imag, 
							String.format("%s - %.0f", detect.getClassName(), detect.classProb * 100), 
							new Point(lb.x, lb.y - 30), 
							Core.FONT_HERSHEY_SIMPLEX, 
							1, 
							new Scalar(255,255,255),
							3);
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
			
			TrafficUpdateObserver.getInstance().updateTracks(tracker.tracks);
			
			return imag;
			
		}

		return curFrame;

	}
	
	
	
	private Vector<DetectedObject> detectObjectYolo(Mat inputMat)
	{
		Vector<DetectedObject> rects = new Vector<DetectedObject>();
		
		Mat m = new Mat();
		inputMat.copyTo(m);
		
		//Imgproc.resize(m, m, new Size(416,416));
		
		if (m.channels() == 4)
			Imgproc.cvtColor(m, m, Imgproc.COLOR_BGRA2BGR);
		
		Mat inputBlob = Dnn.blobFromImage(m, (float)(1/255.0), new Size(416, 416), new Scalar(0), false, false);
		
		yolo.setInput(inputBlob);
		
		// http://junkiyoshi.com/tag/dnn/
		Mat detMat = yolo.forward("detection_out");		
		
		float confidenceThreshold = (float)0.1;		
		for (int i = 0; i < detMat.rows(); i++)
		{
			float confidence = (float)detMat.get(i, 4)[0];
			
			Mat vals = new Mat(detMat, new Range(i, i+1), new Range(5, 25));
			
			int classId;
			double classProb;
			MinMaxLocResult res = Core.minMaxLoc(vals);
			
			classProb = res.maxVal;
			classId = (int)res.maxLoc.x;
			
			if (classProb > .2)
			{
			
				float x = (float)detMat.get(i, 0)[0];
				float y = (float)detMat.get(i, 1)[0];
				float w = (float)detMat.get(i, 2)[0];
				float h = (float)detMat.get(i, 3)[0];
				
				float xLeftBot = (x - w / 2) * inputMat.cols();
				float yLeftBot = (y - h / 2) * inputMat.rows();
				float xRightTop = (x + w / 2) * inputMat.cols();
				float yRightTop = (y + h / 2) * inputMat.rows();
				

				if (confidence <= confidenceThreshold)
					continue;
					
				//System.out.printf("Found a %s with %.2f confidence\n", DetectedObject.classes[classId], confidence);
				
				DetectedObject dob = new DetectedObject();
				
				dob.classId = classId;
				dob.classProb = classProb;
				dob.confidence = confidence;
				dob.xLeftBot = xLeftBot;
				dob.yLeftBot = yLeftBot;
				dob.xRightTop = xRightTop;
				dob.yRightTop = yRightTop;
				
				
				rects.add(dob);
				

			}
		}
		
		return rects;
	}


	private void recognizeVehicle(Mat frame)
	{
		// cascade classification
		// https://docs.opencv.org/3.3.0/dc/d88/tutorial_traincascade.html
		// http://coding-robin.de/2013/07/22/train-your-own-opencv-haar-classifier.html
		Mat newMat = new Mat();
		Imgproc.resize(frame, newMat, new Size(100,100));
		updateImageView(subMatImg, Utils.mat2Image(frame));

		if (saveImagesCbx.isSelected())
		{
			try {
				ImageIO.write(Utils.matToBufferedImage(frame), "png", 
						new File(savedImagesPath + "cars_" + imgCounter + ".png"));
				imgCounter++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private String readUIControls()
	{
		// get slider values
		blurValue = this.blurSlide.getValue(); 			// 21
		threshValue = this.threshSlide.getValue(); 		// 25
		dilationValue = this.dilationSlide.getValue(); 	// 16.42
		erodeValue = this.erodeSlide.getValue(); 		// 3.15
		minCValue = this.minCSlide.getValue(); 			// 460.97

		// show the current selected HSV range
		String valuesToPrint = 
				"Blur:     " + blurValue + "\n" + 
						"Thresh:   " + threshValue + "\n" + 
						"Dilation: " + dilationValue + "\n" + 
						"Erode:    " + erodeValue + "\n" + 
						"C Value:  " + minCValue;

		return valuesToPrint;
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
