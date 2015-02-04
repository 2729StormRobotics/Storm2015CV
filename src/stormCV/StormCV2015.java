package stormCV;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
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

import dashfx.lib.controls.Control;
import dashfx.lib.data.DataCoreProvider;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

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
 * Whether bin is detected or not
 * Angle relative to center of camera to bin
 */

public class StormCV2015 implements Control{

	public static final Scalar
		Red = new Scalar(0, 0, 255),
		Blue = new Scalar(255, 0, 0),
		Green = new Scalar(0, 255, 0),
		Yellow = new Scalar(0, 255, 255),
		
		greenThreshLower = new Scalar(30,40,0),
		greenThreshHigher = new Scalar(60,250,255),
		
		yellowThreshLower = new Scalar(90, 200, 0),
		yellowThreshHigher = new Scalar(120, 250, 255);
	
	public static Mat greenFrame, yellowFrame, original, clone;
	public static ArrayList<MatOfPoint> greenContours = new ArrayList<>(), yellowContours = new ArrayList<>();
	
	static JFrame window = new JFrame();
	static JLabel outputImage = new JLabel();
	
	static boolean binDetected = false, toteDetected = false;
	static int binAngle = 0, toteAngle = 0, fieldOfView = 47;
	
	public static NetworkTable table;
	
	private Timeline line;
	
	Timeline t;
	public StormCV2015(){
		this.t = new Timeline(new KeyFrame(Duration.millis(1000/60), ae -> updateImage()));
	}
	
	public static void main(String[] args) {
		//load native library for OpenCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		//start SmartDashboard
		NetworkTable.setClientMode();
		NetworkTable.setIPAddress("roborio-2729.local");
		table = NetworkTable.getTable("SmartDashboard");
		
		//establish settings for debug frame
		window.setSize(256, 218);
		window.setLocationRelativeTo(null);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
		window.add(outputImage);
		
		while(true){
			//take in and convert image 
			processImage();
			//recognize green bin
			processBin();
			//recognize yellow tote
			processTote();
			//update debug frame
			updateFrame();
			
			//reset variables
			original.release();
			greenFrame.release();
			yellowFrame.release();
			clone.release();
			greenContours.clear();
			yellowContours.clear();
			Image output = new Image("frame.png");
			ImageView view = new ImageView(output);
			HBox box = new HBox();
			Group root = new Group();
			box.getChildren().add(box);
			root.getChildren().add(box);
		}
	}
	
	public static void processImage(){
		//input image from camera
		try {
			ImageIO.write(ImageIO.read((new URL("http://10.27.29.11/axis-cgi/jpg/image.cgi")).openConnection().getInputStream()), "png", new File("frame.png"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//convert image from RGB to HSV
		original = Highgui.imread("frame.png");
		clone = original.clone();
		Imgproc.cvtColor(original, clone, Imgproc.COLOR_RGB2HSV);
	}
	
	public static void processBin(){
		//clone image
		greenFrame = clone.clone();

		//apply threshold
		Core.inRange(clone, greenThreshLower, greenThreshHigher, greenFrame);
		
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
				if(Math.abs(matOfPoint.width() / matOfPoint.height() - (46/75)) < bestDifference){
					bestfit = matOfPoint;
					bestDifference = Math.abs(matOfPoint.width() / matOfPoint.height() - (46/75));
				}
			}
			greenContours.clear();
			greenContours.add(bestfit);
			
			//create rectangle which bounds contours
			Rect rec1 = Imgproc.boundingRect(greenContours.get(0));
			Core.rectangle(original, rec1.tl(), rec1.br(), Green);
			
			//find horizontal angle from center of camera to bin, place text
			binAngle = (int) (((((2 * rec1.tl().x + rec1.width)) / original.width()) - 1) * (fieldOfView/2));
			Core.putText(original, Integer.toString(binAngle), new Point(0,greenFrame.size().height-10), Core.FONT_HERSHEY_PLAIN, 1, Red);
			
			//activate boolean
			binDetected = true;
			
			//send values to SmartDashboard
			table.putNumber("Bin angle", binAngle);
		}else{
			binDetected = false;
		}
		table.putBoolean("Bin detected", binDetected);
	}
	
	public static void processTote(){
		//clone image
		yellowFrame = clone.clone();

		//apply threshold
		Core.inRange(clone, yellowThreshLower, yellowThreshHigher, yellowFrame);
		
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
				if(Math.abs(matOfPoint.width() / matOfPoint.height() - (68/30)) < bestDifference){
					bestfit = matOfPoint;
					bestDifference = Math.abs(matOfPoint.width() / matOfPoint.height() - (68/30));
				}
			}
			yellowContours.clear();
			yellowContours.add(bestfit);
			
			//create rectangle which bounds contours
			Rect rec1 = Imgproc.boundingRect(yellowContours.get(0));
			Core.rectangle(original, rec1.tl(), rec1.br(), Yellow);
			
			//find horizontal angle from center of camera to tote, place text
			toteAngle = (int) (((((2 * rec1.tl().x + rec1.width)) / original.width()) - 1) * (fieldOfView/2));
			Core.putText(original, Integer.toString(toteAngle), new Point(30,yellowFrame.size().height-10), Core.FONT_HERSHEY_PLAIN, 1, Blue);
			
			//activate boolean
			toteDetected = true;
			
			//send values to SmartDashboard
			table.putBoolean("Tote detected", toteDetected);
			table.putNumber("Tote angle", toteAngle);
		}
	}
	
	public static void updateFrame(){
		//update image
		String filename = "frame2.png";
		//write to disk
		Highgui.imwrite(filename, original);
		try {
			outputImage.setIcon(new ImageIcon(ImageIO.read(new File(filename))));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void registered(DataCoreProvider arg0) {
		//load native library for OpenCV
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
				
		//start SmartDashboard
		NetworkTable.setClientMode();
		NetworkTable.setIPAddress("roborio-2729.local");
		table = NetworkTable.getTable("SmartDashboard");
		t.play();
	}
	
	public void updateImage(){
		//take in and convert image 
		processImage();
		//recognize green bin
		processBin();
		//recognize yellow tote
		processTote();
		Highgui.imwrite("frame.png", original);
		Image output = new Image("frame.png");
		ImageView view = new ImageView(output);
		HBox box = new HBox();
		Group root = new Group();
		box.getChildren().add(box);
		root.getChildren().add(box);
	}

	@Override
	public Node getUi() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
