
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class ImageDisplay {

	JFrame frame, frame_output;
	JLabel lbIm1, lbIm2;
	BufferedImage imgOne, subSampled, scaled;
	int width = 1920; // default image width and height
	int height = 1080;

	class YUV{
		double y,u,v;
		public YUV(double y, double u, double v) {
			this.y = y;
			this.u = u;
			this.v = v;
		}

		public String toString(){
			return "("+y+", "+u+" ,"+ v +")";
		}
	}

	class RGB{
		int r,g,b;
		public RGB(int r, int g, int b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		public String toString(){
			return "("+r+", "+g+" ,"+ b +")";
		}
	}
	
	RGB[][] inputRGB = new RGB[height][width];
	YUV[][] convertedYUV = new YUV[height][width];
	RGB[][] outputRGB = new RGB[height][width];

	private YUV rgbToYUV(RGB rgb){
		double y = 0.299*rgb.r + 0.587*rgb.g + 0.114*rgb.b;
		double u = 0.596*rgb.r - 0.274*rgb.g - 0.322*rgb.b;
		double v = 0.211*rgb.r - 0.523*rgb.g + 0.312*rgb.b;

		return new YUV(y,u,v);
	}

	private RGB yuvToRGB(YUV yuv){
		int r = (int)(1*yuv.y + 0.956*yuv.u + 0.621*yuv.v);
		int g = (int)(1*yuv.y - 0.272*yuv.u - 0.647*yuv.v);
		int b = (int)(1*yuv.y - 1.106*yuv.u + 1.703*yuv.v);

		r = r<0?0:r>255?255:r;
		g = g<0?0:g>255?255:g;
		b = b<0?0:b>255?255:b;

		return new RGB(r, g, b);
	}

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;

			// read input, covert to yuv space
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					int r = Byte.toUnsignedInt(bytes[ind]);
					int g = Byte.toUnsignedInt(bytes[ind+height*width]);
					int b = Byte.toUnsignedInt(bytes[ind+height*width*2]); 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					
					img.setRGB(x,y,pix);
					ind++;

					RGB rgb = new RGB(r,g,b);
					// System.out.println(tmp);

					inputRGB[y][x] = rgb;
					convertedYUV[y][x] = rgbToYUV(rgb);
				}
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	private void subsample(int width, int height, int y_sub, int u_sub, int v_sub, BufferedImage output){
		// subsampling
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				// subsampling-y
				int i = x % y_sub;
				if(i!=0)
				{
					double prevNonZeroY = convertedYUV[y][x-i].y;
					double nextNonZeroY = (x+y_sub-i)<width?convertedYUV[y][x+y_sub-i].y:prevNonZeroY;
					convertedYUV[y][x].y = (prevNonZeroY*(y_sub-i)+nextNonZeroY*(i))/y_sub;
				}
				// subsampling-u
				i = x % u_sub;
				if(i!=0)
				{
					double prevNonZeroU = convertedYUV[y][x-i].u;
					double nextNonZeroU = (x+y_sub-i)<width?convertedYUV[y][x+y_sub-i].u:prevNonZeroU;
					convertedYUV[y][x].u = (prevNonZeroU*(y_sub-i)+nextNonZeroU*(i))/y_sub;
				}
				// subsampling-v
				i = x % v_sub;
				if(i!=0)
				{
					double prevNonZeroV = convertedYUV[y][x-i].v;
					double nextNonZeroV = (x+y_sub-i)<width?convertedYUV[y][x+y_sub-i].v:prevNonZeroV;
					convertedYUV[y][x].v = (prevNonZeroV*(y_sub-i)+nextNonZeroV*(i))/y_sub;
				}

			}
		}		


		// covert to RGB 
		for(int y = 0; y < height; y++)
		{
			for(int x = 0; x < width; x++)
			{
				RGB rgb = yuvToRGB(convertedYUV[y][x]);
				int r = rgb.r;
				int g = rgb.g;
				int b = rgb.b; 

				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
				output.setRGB(x,y,pix);
				outputRGB[y][x] = new RGB(r,g, b);
			}
		}
	}

	private void scaling(int scaledWidth, int scaledHeight, float wStep, float hStep, int antialiasing, BufferedImage output){
		for(int y = 0; y < scaledHeight; y++) 
		{
			for(int x = 0; x < scaledWidth; x++)
			{
				int orig_x = Math.round(x*wStep);
				int orig_y = Math.round(y*hStep);
				int r,g,b, count = 1;
				r = outputRGB[orig_y][orig_x].r;
				g = outputRGB[orig_y][orig_x].g;
				b = outputRGB[orig_y][orig_x].b;
				if(antialiasing == 1)
				{					
					/* antialiasing with kernel 3x3
					 * __|__|__
					 * _#|__|__
					 *   |  |
					 */
					if(orig_x > 0)
					{
						count++;
						r += outputRGB[orig_y][orig_x-1].r;
						g += outputRGB[orig_y][orig_x-1].g;
						b += outputRGB[orig_y][orig_x-1].b;
					}
					/*
					 * __|_#|__
					 * __|__|__
					 *   |  |
					 */
					if(orig_y > 0)
					{
						count++;
						r += outputRGB[orig_y-1][orig_x].r;
						g += outputRGB[orig_y-1][orig_x].g;
						b += outputRGB[orig_y-1][orig_x].b;
					}
					/*
					 * _#|__|__
					 * __|__|__
					 *   |  |
					 */
					if(orig_x > 0 && orig_y > 0) 
					{
						count++;
						r += outputRGB[orig_y-1][orig_x-1].r;
						g += outputRGB[orig_y-1][orig_x-1].g;
						b += outputRGB[orig_y-1][orig_x-1].b;
					}
					/*
					 * __|__|__
					 * __|__|#_
					 *   |  |
					 */
					if(orig_x < width-1)
					{
						count++;
						r += outputRGB[orig_y][orig_x+1].r;
						g += outputRGB[orig_y][orig_x+1].g;
						b += outputRGB[orig_y][orig_x+1].b;
					}
					/*
					 * __|__|__
					 * __|__|__
					 *   | #|
					 */
					if(orig_y < height-1)
					{
						count++;
						r += outputRGB[orig_y+1][orig_x].r;
						g += outputRGB[orig_y+1][orig_x].g;
						b += outputRGB[orig_y+1][orig_x].b;
					}
					/*
					 * __|__|__
					 * __|__|__
					 *   |  |#
					 */
					if(orig_x < width+1 && orig_y < height-1)
					{
						count++;
						r += outputRGB[orig_y+1][orig_x+1].r;
						g += outputRGB[orig_y+1][orig_x+1].g;
						b += outputRGB[orig_y+1][orig_x+1].b;
					}
					/*
					 * __|__|#_
					 * __|__|__
					 *   |  |
					 */
					if(orig_x < width+1 && orig_y > 0)
					{
						count++;
						r += outputRGB[orig_y-1][orig_x+1].r;
						g += outputRGB[orig_y-1][orig_x+1].g;
						b += outputRGB[orig_y-1][orig_x+1].b;
					}
					/*
					 * __|__|__
					 * __|__|__
					 *  #|  |
					 */
					if(orig_x > 0 && orig_y < height-1)
					{
						count++;
						r += outputRGB[orig_y+1][orig_x-1].r;
						g += outputRGB[orig_y+1][orig_x-1].g;
						b += outputRGB[orig_y+1][orig_x-1].b;
					}

					r = r/count;
					g = g/count;
					b = b/count;
				}
				int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
				output.setRGB(x,y,pix);
			}
		}
	}

	public void showIms(String[] args){

		// Read a parameter from command line
		// String param1 = args[1];
		// System.out.println("The second parameter was: " + param1);

		int y_sub = Integer.parseInt(args[1]);
		int u_sub = Integer.parseInt(args[2]);
		int v_sub = Integer.parseInt(args[3]);

		float scaleW = Float.parseFloat(args[4]);
		float scaleH = Float.parseFloat(args[5]);
		int antialiasing = Integer.parseInt(args[6]);

		int scaledWidth = (int)Math.floor(scaleW * width);
		int scaledHeight = (int)Math.floor(scaleH * height);
		float wStep = 1/scaleW;
		float hStep = 1/scaleH;

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		subSampled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);
		subsample(width, height, y_sub, u_sub, v_sub, subSampled);
		scaling(scaledWidth, scaledHeight, wStep, hStep, antialiasing, scaled);

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));
		lbIm2 = new JLabel(new ImageIcon(scaled));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);

		frame_output = new JFrame();
		frame_output.getContentPane().setLayout(gLayout);
		frame_output.getContentPane().add(lbIm2,c);
		frame_output.pack();
		frame_output.setVisible(true);
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
