package application;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.SVM;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.objdetect.Objdetect;
import org.opencv.video.BackgroundSubtractor;
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
	private CheckBox saveImagesCbx;

	private String savedImagesPath = "images/";
	private int imgCounter = 0;

	private String carsCascadeName = "haar/cars.xml";

	private ObjectProperty<String> slideValuesProp;

	private double blurValue, dilationValue, erodeValue, threshValue, minCValue;

	private VideoInput videoFeed = new VideoInput();

	private ScheduledExecutorService timer;

	CascadeClassifier detector;
	HOGDescriptor hog;

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

		detector = new CascadeClassifier(carsCascadeName);
		
		hog = new HOGDescriptor();
		hog.load("hog/my_detector.yml");

		//backSub = Video.createBackgroundSubtractorMOG2(300, 200, false);

		slideValuesProp = new SimpleObjectProperty<>();
		this.slideProp.textProperty().bind(slideValuesProp);

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

		Imgproc.threshold(output, output, threshValue, 255, Imgproc.THRESH_BINARY);


		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilationValue, dilationValue));
		Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(erodeValue, erodeValue));


		Imgproc.erode(output, output, erodeElement);
		Imgproc.erode(output, output, erodeElement);
		Imgproc.dilate(output, output, dilateElement);
		Imgproc.dilate(output, output, dilateElement);


		updateImageView(imgv2, Utils.mat2Image(output));


		int testCase = 3;
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
			List<Rect> rois = detectRoi(grayFrame, detector);

			System.out.println("Number of rois: " + rois.size());

			for (int i = 0; i < rois.size(); i++)
			{
				Rect r = rois.get(i);
				Imgproc.rectangle(curFrame, 
						new Point(r.x-2, r.y-2),
						new Point(r.x + r.width + 2, r.y + r.height + 2),
						new Scalar(0, 250, 0));
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

		return curFrame;

	}

	private double diffUpDown(Mat frame)
	{
		int height = frame.height();
		int width = frame.width();
		int depth = frame.depth();
		int half = height / 2;

		Mat top = frame.submat(new Range(0, half), new Range(0, width));
		Mat bottom = frame.submat(new Range(half, half * 2), new Range(0, width));

		Core.flip(top, top, 1);
		Imgproc.resize(bottom, bottom, new Size(32, 64));
		Imgproc.resize(top, top, new Size(32, 64));

		return mse(top, bottom);
	}

	private double diffLeftRight(Mat frame)
	{
		int height = frame.height();
		int width = frame.width();
		int depth = frame.depth();
		int half = height / 2;

		Mat left  = frame.submat(new Range(0, height), new Range(0, half));
		Mat right = frame.submat(new Range(0, height), new Range(half, (half * 2) - 1));

		Core.flip(right, right, 1);
		Imgproc.resize(left, left, new Size(32, 64));
		Imgproc.resize(right, right, new Size(32, 64));

		return mse(left, right);
	}

	private boolean isNewRoi(int rx, int ry, int rw, int rh, List<Rect> rectangles)
	{
		for (Rect r : rectangles)
		{
			if (Math.abs(r.width - r.x) < 40 && Math.abs(r.height - r.y) < 40)
				return false;
		}

		return true;
	}

	private List<Rect> detectRoi(Mat frame, CascadeClassifier cascade)
	{
		// http://www.emt.tugraz.at/~pinz/data/GRAZ_02/ 
		//http://funvision.blogspot.cz/2016/11/computer-vision-car-dataset-for-opencv.html
		// https://stackoverflow.com/questions/21847110/how-to-get-images-of-cars-samples-for-haarcascade-training
		// https://abhishek4273.com/2014/03/16/traincascade-and-car-detection-using-opencv/
		
		int scaleDown = 1;
		int frameHeight = frame.height();
		int frameWidth  = frame.width();
		Mat tempFrame = new Mat();

		frame.copyTo(tempFrame);

		Imgproc.resize(tempFrame, tempFrame, new Size(frameWidth/scaleDown, frameHeight/scaleDown));
		frameHeight = tempFrame.height();
		frameWidth  = tempFrame.width();

		updateImageView(imgv2, Utils.mat2Image(tempFrame));

		MatOfRect rois = new MatOfRect();
		cascade.detectMultiScale(tempFrame, rois, 1.5, 1, 0 | Objdetect.CASCADE_DO_ROUGH_SEARCH, 
				new Size(10, 10), new Size());

		List<Rect> newRegions = new ArrayList<Rect>();
		int minY = (int)(frameHeight * 0.1);
		Rect[] roisArray = rois.toArray();

		for (int i = 0; i < roisArray.length; i++)
		{
			Mat roiImage = tempFrame.submat(
					new Range(roisArray[i].y, roisArray[i].y + roisArray[i].height),
					new Range(roisArray[i].x, roisArray[i].x + roisArray[i].width));

			int roiWidth = roiImage.width();

			if (roiImage.width() > minY)
			{
//				double diffX = diffLeftRight(roiImage);
//				double diffY = Math.round(diffUpDown(roiImage));
//
//				if (diffX > 1600 && diffX < 3000 && diffY > 12000)
//				{
					newRegions.add(new Rect(
							roisArray[i].x * scaleDown,
							roisArray[i].y * scaleDown,
							roisArray[i].width * scaleDown,
							roisArray[i].height * scaleDown));
//				}
			}
		}

		return newRegions;

	}

	private double mse(Mat matA, Mat matB)
	{
		double sum = 0.0;
		int minW = Math.min(matA.width(), matB.width());
		int minH = Math.min(matA.height(), matB.height());
		
		for (int x = 0; x < minW; x++)
		{
			for (int y = 0; y < minH; y++)
			{
				sum += (Math.pow(matA.get(x, y)[0] - matB.get(x, y)[0], 2));
			}
		}

		return sum / (minW * minH);
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
