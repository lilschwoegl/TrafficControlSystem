package simulator;

import java.awt.Canvas;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;

public class Display {
	private String title;
	private int width;
	static int height;
	private int posX;
	private int posY;
	public static JFrame frame;
	public static Canvas canvas;
	public static JButton addN1Car, addN2Car, addS1Car, addS2Car, addE1Car, addE2Car, addW1Car, addW2Car;
	
	
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
		addN1Car = new JButton("add N1 Car");
		addN1Car.setBounds(100, 500, 100, 50);
		addN2Car = new JButton("add N2 Car");
		addS1Car = new JButton("add S1 Car");
		addS2Car = new JButton("add S2 Car");
		addE1Car = new JButton("add E1 Car");
		addE2Car = new JButton("add E2 Car");
		addW1Car = new JButton("add W1 Car");
		addW2Car = new JButton("add W2 Car");
		
		frame.add(addN1Car);
		canvas = new Canvas();
		canvas.setPreferredSize(new Dimension(width,height));
		//canvas.setBackground(Color.blue);
		canvas.setFocusable(false);
		

		frame.add(canvas);
		frame.pack();
	}
}
