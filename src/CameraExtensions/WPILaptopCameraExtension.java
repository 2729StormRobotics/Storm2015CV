/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CameraExtensions;

import edu.wpi.first.smartdashboard.gui.StaticWidget;
import edu.wpi.first.smartdashboard.properties.Property;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * An extension that gets images from a webcam on the computer.
 * @author Greg Granito
 */
public class WPILaptopCameraExtension extends StaticWidget {

    /**
	 * 
	 */
	private static final long serialVersionUID = -8904912450344270421L;
	public static final String NAME = "Laptop Camera";
    public class GCThread extends Thread {

        boolean destroyed = false;

        @Override
        public void run() {
            while (!destroyed) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ex) {
                }
                System.gc();
            }
        }

        public void destroy() {
            destroyed = true;
            interrupt();
        }
    }

    public class BGThread extends Thread {

        boolean destroyed = false;

        public BGThread() {
            super("Camera Background");
        }

        @Override
        public void run() {
            WPIImage image;
            while (!destroyed) {
                if (cam == null) {
                    cam = new WPILaptopCamera();
                }
                try {
                    image = cam.getCurrentFrame();

                    if (image instanceof WPIColorImage) {
                        drawnImage = processImage((WPIColorImage) image).getBufferedImage();
                        repaint();
                    } else if (image instanceof WPIGrayscaleImage) {
                        drawnImage = processImage((WPIGrayscaleImage) image).getBufferedImage();
                        repaint();
                    }
                } catch (final Exception e) {
                    drawnImage = null;
                    repaint();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                    }
                }
            }

        }

        @Override
        public void destroy() {
            destroyed = true;
        }
    }
    private boolean resized = false;
    private WPILaptopCamera cam;
    private BufferedImage drawnImage;
    private BGThread bgThread = new BGThread();
    private GCThread gcThread = new GCThread();
  
    @Override
    public void init() {
        setPreferredSize(new Dimension(100, 100));
        bgThread.start();
        gcThread.start();
        revalidate();
        repaint();
    }

    @Override
    public void propertyChanged(Property property) {

    }

    @Override
    public void disconnect() {
        bgThread.destroy();
        gcThread.destroy();
        cam.dispose();
        super.disconnect();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (drawnImage != null) {
            if (!resized) {
                setPreferredSize(new Dimension(drawnImage.getWidth(), drawnImage.getHeight()));
                revalidate();
            }
            int width = getBounds().width;
            int height = getBounds().height;
            double scale = Math.min((double) width / (double) drawnImage.getWidth(), (double) height / (double) drawnImage.getHeight());
            g.drawImage(drawnImage, (int) (width - (scale * drawnImage.getWidth())) / 2, (int) (height - (scale * drawnImage.getHeight())) / 2,
                    (int) ((width + scale * drawnImage.getWidth()) / 2), (int) (height + scale * drawnImage.getHeight()) / 2,
                    0, 0, drawnImage.getWidth(), drawnImage.getHeight(), null);
        } else {
            g.setColor(Color.PINK);
            g.fillRect(0, 0, getBounds().width, getBounds().height);
            g.setColor(Color.BLACK);
            g.drawString("NO CONNECTION", 10, 10);
        }
    }

    public WPIImage processImage(WPIColorImage rawImage) {
        return rawImage;
    }

    public WPIImage processImage(WPIGrayscaleImage rawImage) {
        return rawImage;
    }
}
