package application;

import java.io.IOException;
import java.util.Vector;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

public class YoloDetector {

	private Net yolo;
	private String classNameFile;
	private float confidenceThreshold  = (float)0.3;	
	private float probabilityThreshold = (float)0.3;
	
	public YoloDetector(String cfg, String weights, String classes) throws IOException
	{
		yolo = Dnn.readNetFromDarknet(cfg, weights);
		classNameFile = classes;
		DetectedObject.loadClassNames("yolo/classes/custom-training.names");
	}
	
	public void setConfidenceThresh(float thresh)
	{
		confidenceThreshold = thresh;
	}
	
	public void setProbabilityThresh(float thresh)
	{
		probabilityThreshold = thresh;
	}
	
	public Vector<DetectedObject> detectObjectYolo(Mat inputMat)
	{
		Vector<DetectedObject> rects = new Vector<DetectedObject>();
		
		//logMsg("Detecting objects with YOLO");

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
				confidence > confidenceThreshold)
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
		
		// logMsg("Done with YOLO");

		return rects;
	}
	
}
