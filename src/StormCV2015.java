import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;

/*
 * @author: Storm 2729
 * 
 * Based on code by FRC team 3019, Firebird Robotics
 */

public class StormCV2015{
	
	public static void processImage(){
		try {
			ImageIO.write(ImageIO.read((new URL("http://10.27.29.11/axis-cgi/jpg/image.cgi")).openConnection().getInputStream()),
					"png", new File("frame.png"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
