package simulator;

import java.awt.Canvas;
import java.awt.Dimension;

import javax.swing.JFrame;

public class Display {
	private String title;
	private int width;
	static int height;
	private int posX;
	private int posY;
	public static JFrame frame;
	public static Canvas canvas;
	
	public Display (String title, int width, int height) {
		this(title, width, height, 0, 0);
	}
	
	public Display (String title, int width, int height, int posX, int posY) {
		this.title = title;
		this.width = width;
		this.height = height;
		this.posX = posX;
		this.posY = posY;
		createDisplay();
	}
	
	public void createDisplay (){
		frame = new JFrame(title);
		frame.setSize(width, height);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setLocation(posX, posY);
		canvas = new Canvas();
		canvas.setPreferredSize(new Dimension(width,height));
		//canvas.setBackground(Color.blue);
		canvas.setFocusable(false);
		frame.add(canvas);
		frame.pack();
	}
}
