import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

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

import edu.wpi.first.smartdashboard.properties.IntegerProperty;
import edu.wpi.first.smartdashboard.xml.SmartDashboardXMLReader;

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
 */

public class StormCV2015{

	public static final Scalar
		Red = new Scalar(0, 0, 255),
		Blue = new Scalar(255, 0, 0),
		Green = new Scalar(0, 255, 0),
		Yellow = new Scalar(0, 255, 255),
		
		greenThreshLower = new Scalar(40,5,0),
		greenThreshHigher = new Scalar(55,80,25),
		
		yellowThreshLower = new Scalar(40,0,0),
		yellowThreshHigher = new Scalar(100,255,255);
	
	public static Mat greenFrame, yellowFrame, original, clone;
	public static ArrayList<MatOfPoint> greenContours = new ArrayList<>(), yellowContours = new ArrayList<>();
	
	static JFrame window = new JFrame();
	static JLabel outputImage = new JLabel();
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		window.setSize(256, 218);
		window.setLocationRelativeTo(null);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
		window.add(outputImage);
		
		while(true){
			processImage();
			processBin();
			processTote();
			updateFrame();
			
			//reset
			original.release();
			greenFrame.release();
			yellowFrame.release();
			clone.release();
			greenContours.clear();
			yellowContours.clear();
		}
	}
	
	public static void processImage(){
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
		
		//without selecting best fit rectangle
		for(int i = 0; i < greenContours.size(); i++){
			Rect rec = Imgproc.boundingRect(greenContours.get(i));
			Core.rectangle(original, rec.tl(), rec.br(), Green);	
		}
	}
	
	public static void processTote(){
		//clone image
		yellowFrame = clone.clone();

		//apply threshold
		Core.inRange(clone, yellowThreshLower, yellowThreshHigher, yellowFrame);
		
		//apply contours
		Imgproc.findContours(yellowFrame, yellowContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		//remove objects with too small
		for(Iterator<MatOfPoint> iterator = yellowContours.iterator(); iterator.hasNext();){
			MatOfPoint matOfPoint = (MatOfPoint) iterator.next();
			if(matOfPoint.width() * matOfPoint.height() < 150){
				iterator.remove();
			}
		}
		
		//without selecting best fit rectangle
		for(int i = 0; i < yellowContours.size(); i++){
			Rect rec = Imgproc.boundingRect(yellowContours.get(i));
			Core.rectangle(original, rec.tl(), rec.br(), Yellow);
		}
	}
	
	public static void updateFrame(){
		/*
		if(contours.size() == 1){
			Rect rec1 = Imgproc.boundingRect(contours.get(0));
			Core.rectangle(original, rec1.tl(), rec1.br(), Green);
			String string = "TargetFound at X:" + (rec1.tl().x + rec1.br().x) / 2 + "Y:" + (rec1.tl().y + rec1.br().y) / 2;
			Core.putText(original, string, new Point(0,frame.size().height-10), Core.FONT_HERSHEY_PLAIN, 1, Red);
						
			System.out.println("here2");
		} else if(contours.size() > 1){
			int bestDifference = 10;
			MatOfPoint bestfit = null;
			
			for(Iterator<MatOfPoint> iterator = contours.iterator(); iterator.hasNext();){
				MatOfPoint matOfPoint = (MatOfPoint) iterator.next();

				if(Math.abs(matOfPoint.width() / matOfPoint.height() - (75/46)) < bestDifference){
					bestfit = matOfPoint;
					bestDifference = Math.abs(matOfPoint.width() / matOfPoint.height() - (75/46));
				}
			}
			
			contours.clear();
			contours.add(bestfit);
			
			Rect rec1 = Imgproc.boundingRect(contours.get(0));
			Core.rectangle(original, rec1.tl(), rec1.br(), Green);
			String string = "TargetFound at X:" + (rec1.tl().x + rec1.br().x) / 2 + "Y:" + (rec1.tl().y + rec1.br().y) / 2;
			Core.putText(original, string, new Point(0,frame.size().height-10), Core.FONT_HERSHEY_PLAIN, 1, Red);
			
			
			System.out.println("here3");
		}
		*/
		
		//update image
		String filename = "frame2.png";
		Highgui.imwrite(filename, original);  // write to disk
		try {
			outputImage.setIcon(new ImageIcon(ImageIO.read(new File(filename))));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
