import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import edu.wpi.first.smartdashboard.camera.WPICameraExtension;
import edu.wpi.first.smartdashboard.camera.WPILaptopCameraExtension;
import edu.wpi.first.smartdashboard.properties.DoubleProperty;
import edu.wpi.first.smartdashboard.properties.IntegerProperty;
import edu.wpi.first.wpijavacv.StormExtensions;
import edu.wpi.first.wpijavacv.WPIColorImage;
import edu.wpi.first.wpijavacv.WPIImage;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import static com.googlecode.javacv.cpp.opencv_core.*;

/*
 * @author: Storm 2729
 * 
 * Based on code by FRC team 3019, Firebird Robotics
 */

/*
 * Measurements (cm)
 * 
 * Bin (garbage can)
 * Height: 75, Width: 46
 * 
 * Tote
 * Height: 30, Width: 68, Depth: 42
 * 
 * Output to SmartDashboard
 * Whether bin/tote is detected or not
 * Angle relative to center of camera to bin/tote
 */

public class StormCV2015 extends WPICameraExtension{

	private static final long serialVersionUID = 1L;

	private IntegerProperty _hueCLow = new IntegerProperty(this, "Container hue low", 30),
							_hueCHigh= new IntegerProperty(this, "Container hue high", 60),
							_satCLow = new IntegerProperty(this, "Container sat low", 40),
							_satCHigh= new IntegerProperty(this, "Container sat high", 250),
							_valCLow = new IntegerProperty(this, "Container val low", 0),
							_valCHigh= new IntegerProperty(this, "Container val high", 255),
							_hueTLow = new IntegerProperty(this, "Tote hue low", 90),
							_hueTHigh= new IntegerProperty(this, "Tote hue high", 120),
							_satTLow = new IntegerProperty(this, "Tote sat low", 200),
							_satTHigh= new IntegerProperty(this, "Tote sat high", 250),
							_valTLow = new IntegerProperty(this, "Tote val low", 0),
							_valTHigh= new IntegerProperty(this, "Tote val high", 250),
							_aspectRatioC = new IntegerProperty(this, "Container aspect ratio", 46/75),
							_aspectRatioT = new IntegerProperty(this, "Tote aspect ratio", 68/30);
	
	private DoubleProperty  _fieldOfViewV = new DoubleProperty(this, "Vertical field of view", 36.13),
						 	_fieldOfViewH = new DoubleProperty(this, "Horizontal field of view", 47);
	
	public static Scalar
		Red = new Scalar(0, 0, 255),
		Blue = new Scalar(255, 0, 0),
		Green = new Scalar(0, 255, 0),
		Yellow = new Scalar(0, 255, 255),
		
		greenThreshLower,
		greenThreshHigher,
		
		yellowThreshLower,
		yellowThreshHigher;
	
	public static Mat greenFrame, yellowFrame, original, _hsv;
	public static ArrayList<MatOfPoint> greenContours = new ArrayList<>(), yellowContours = new ArrayList<>();
	
	static JFrame window = new JFrame();
	static JLabel outputImage = new JLabel();
	
	static boolean binDetected = false, toteDetected = false;
	static int binAngle = 0, toteAngle = 0, fieldOfView = 47;
	
	public static String _userLoc = System.getenv("USERPROFILE");
	
	public static NetworkTable table;
	
	public static void main(String[] args) {
		//load native library for OpenCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		//start SmartDashboard
		NetworkTable.setClientMode();
		NetworkTable.setIPAddress("roborio-2729.local");
		table = NetworkTable.getTable("SmartDashboard");
	}
	
	public StormCV2015(){
		
	}
	
	@Override
	public WPIImage processImage(WPIColorImage rawImage){
		//System.out.println(System.getProperty("java.library.path"));
		//load native library for OpenCV

		greenThreshLower   = new Scalar(_hueCLow.getValue(),_satCLow.getValue(),_valCLow.getValue());
		greenThreshHigher  = new Scalar(_hueCHigh.getValue(),_satCHigh.getValue(),_valCHigh.getValue());
		yellowThreshLower  = new Scalar(_hueTLow.getValue(),_satTLow.getValue(),_valTLow.getValue());
		yellowThreshHigher = new Scalar(_hueTHigh.getValue(),_satTHigh.getValue(),_valTHigh.getValue());
		
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		//start SmartDashboard
		table = NetworkTable.getTable("SmartDashboard");
		//input image from camera
		try {
			ImageIO.write(ImageIO.read((new URL("http://10.27.29.11/axis-cgi/jpg/image.cgi")).openConnection().getInputStream()), "png", new File(_userLoc + "/frame.png"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//convert image from RGB to HSV
		original = Highgui.imread(_userLoc + "/frame.png");
		_hsv = new Mat();
		System.out.println("original depth: " + original.depth() + " original Chann: " + original.channels());
		
		Imgproc.cvtColor(original, _hsv, Imgproc.COLOR_RGB2HSV);
		processBin();
		//recognize yellow tote
		processTote();
		//update debug frame
		updateFrame();
		
		//reset variables
		original.release();
		greenFrame.release();
		yellowFrame.release();
		_hsv.release();
		greenContours.clear();
		yellowContours.clear();
		
		//return image
		WPIImage output = rawImage;
		try {
			StormExtensions.copyImage(output, IplImage.createFrom(ImageIO.read(new File(_userLoc + "/frame2.png"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return output;
	}
	
	public void processBin(){
		//clone image
		greenFrame = _hsv.clone();

		//apply threshold
		Core.inRange(_hsv, greenThreshLower, greenThreshHigher, greenFrame);
		
		//apply contours
		Imgproc.findContours(greenFrame, greenContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		//remove objects with too small area
		for(Iterator<MatOfPoint> iterator = greenContours.iterator(); iterator.hasNext();){
			MatOfPoint matOfPoint = (MatOfPoint) iterator.next();
			
			if(matOfPoint.width() * matOfPoint.height() < 100){
				iterator.remove();
			}
		}
		
		//utilize contours if one or more are detected
		if(greenContours.size()>=1){
			//pick contours of best fit
			int bestDifference = 10;
			MatOfPoint bestfit = null;
			for(Iterator<MatOfPoint> iterator = greenContours.iterator(); iterator.hasNext();){
				MatOfPoint matOfPoint = (MatOfPoint) iterator.next();
				if(Math.abs(matOfPoint.width() / matOfPoint.height() - (_aspectRatioC.getValue())) < bestDifference){
					bestfit = matOfPoint;
					bestDifference = Math.abs(matOfPoint.width() / matOfPoint.height() - (_aspectRatioC.getValue()));
				}
			}
			greenContours.clear();
			greenContours.add(bestfit);
			
			//create rectangle which bounds contours
			Rect rec1 = Imgproc.boundingRect(greenContours.get(0));
			Core.rectangle(original, rec1.tl(), rec1.br(), Green);
			
			//find horizontal angle from center of camera to bin, place text
			binAngle = (int) (((((2 * rec1.tl().x + rec1.width)) / original.width()) - 1) * (fieldOfView/2));
			Core.putText(original, Integer.toString(binAngle), new Point(0, greenFrame.size().height-10), Core.FONT_HERSHEY_PLAIN, 1, Red);
			
			//activate boolean
			binDetected = true;
		} else {
			//deactivate boolean
			binDetected = false;
		}
		
		//send values to SmartDashboard
		table.putNumber("Bin angle", binAngle);
		table.putBoolean("Bin detected", binDetected);
	}
	
	public void processTote(){
		//clone image
		yellowFrame = _hsv.clone();

		//apply threshold
		Core.inRange(_hsv, yellowThreshLower, yellowThreshHigher, yellowFrame);
		
		//apply contours
		Imgproc.findContours(yellowFrame, yellowContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		//remove objects with too small area
		for(Iterator<MatOfPoint> iterator = yellowContours.iterator(); iterator.hasNext();){
			MatOfPoint matOfPoint = (MatOfPoint) iterator.next();
			
			if(matOfPoint.width() * matOfPoint.height() < 50){
				iterator.remove();
			}
		}
		
		//utilize contours if one or more are detected
		if(yellowContours.size()>=1){
			//pick contours of best fit
			int bestDifference = 10;
			MatOfPoint bestfit = null;
			for(Iterator<MatOfPoint> iterator = yellowContours.iterator(); iterator.hasNext();){
				MatOfPoint matOfPoint = (MatOfPoint) iterator.next();
				if(Math.abs(matOfPoint.width() / matOfPoint.height() - (_aspectRatioT.getValue())) < bestDifference){
					bestfit = matOfPoint;
					bestDifference = Math.abs(matOfPoint.width() / matOfPoint.height() - (_aspectRatioT.getValue()));
				}
			}
			yellowContours.clear();
			yellowContours.add(bestfit);
			
			//create rectangle which bounds contours
			Rect rec1 = Imgproc.boundingRect(yellowContours.get(0));
			Core.rectangle(original, rec1.tl(), rec1.br(), Yellow);
			
			//find horizontal angle from center of camera to tote, place text
			toteAngle = (int) (((((2 * rec1.tl().x + rec1.width)) / original.width()) - 1) * (_fieldOfViewH.getValue()/2));
			Core.putText(original, Integer.toString(toteAngle), new Point(0, yellowFrame.size().height-10), Core.FONT_HERSHEY_PLAIN, 1, Blue);
			
			//activate boolean
			toteDetected = true;
		} else {
			//deactivate boolean
			toteDetected = false;
		}
		
		//send values to SmartDashboard
		table.putNumber("Tote angle", toteAngle);
		table.putBoolean("Tote detected", toteDetected);
	}
	
	public static void updateFrame(){
		//update image
		String filename = _userLoc + "/frame2.png";
		//write to disk
		Highgui.imwrite(filename, original);
	}
}
