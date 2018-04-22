package simulator;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.opencv.core.Point;

import simulator.Constants.Direction;
import tracking.SimulatedTrack;

public class Display implements ActionListener{
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
		addN1Car.setBounds(0, 500, 100, 50);
		addN2Car = new JButton("add N2 Car");
		addN2Car.setBounds(0, 550, 100, 50);
		addS1Car = new JButton("add S1 Car");
		addS1Car.setBounds(100, 500, 100, 50);
		addS2Car = new JButton("add S2 Car");
		addS2Car.setBounds(100, 550, 100, 50);
		addE1Car = new JButton("add E1 Car");
		addE1Car.setBounds(400, 500, 100, 50);
		addE2Car = new JButton("add E2 Car");		
		addE2Car.setBounds(400, 550, 100, 50);
		addW1Car = new JButton("add W1 Car");
		addW1Car.setBounds(500, 500, 100, 50);
		addW2Car = new JButton("add W2 Car");
		addW2Car.setBounds(500, 550, 100, 50);

		addN1Car.addActionListener(this);
		addN2Car.addActionListener(this);
		addS1Car.addActionListener(this);
		addS2Car.addActionListener(this);
		addE1Car.addActionListener(this);
		addE2Car.addActionListener(this);
		addW1Car.addActionListener(this);
		addW2Car.addActionListener(this);
		
		frame.add(addN1Car);
		frame.add(addN2Car);
		frame.add(addS1Car);
		frame.add(addS2Car);
		frame.add(addE1Car);
		frame.add(addE2Car);
		frame.add(addW1Car);
		frame.add(addW2Car);
		
		canvas = new Canvas();
		canvas.setPreferredSize(new Dimension(width,height));
		//canvas.setBackground(Color.blue);
		canvas.setFocusable(false);
		

		frame.add(canvas);
		frame.pack();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (e.getSource() == addN1Car) {
			SimulatorManager.addCar(
					1-1, 
					Direction.NORTH, 
					new SimulatedTrack(
							new Point(0,SimConfig.simDisplayHeight), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.NORTH,
							SimConfig.speed),
					true);
		} else if (e.getSource() == addN2Car) {
			SimulatorManager.addCar(
					2-1, 
					Direction.NORTH, 
					new SimulatedTrack(
							new Point(0,SimConfig.simDisplayHeight), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.NORTH,
							SimConfig.speed),
					true);
		} else if (e.getSource() == addS1Car){
			SimulatorManager.addCar(
					1-1, 
					Direction.SOUTH, 
					new SimulatedTrack(
							new Point(0,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.SOUTH,
							SimConfig.speed),
					true);
		} else if (e.getSource() == addS2Car) {
			SimulatorManager.addCar(
					2-1, 
					Direction.SOUTH, 
					new SimulatedTrack(
							new Point(0,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.SOUTH,
							SimConfig.speed),
					true);
		} else if (e.getSource() == addE1Car) {
			SimulatorManager.addCar(
					1-1, 
					Direction.EAST, 
					new SimulatedTrack(
							new Point(0,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.EAST,
							SimConfig.speed),
					true);
		} else if (e.getSource() == addE2Car) {
			SimulatorManager.addCar(
					2-1, 
					Direction.EAST, 
					new SimulatedTrack(
							new Point(0,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.EAST,
							SimConfig.speed),
					true);
		} else if (e.getSource() == addW1Car) {
			SimulatorManager.addCar(
					1-1, 
					Direction.WEST, 
					new SimulatedTrack(
							new Point(SimConfig.simDisplayWidth,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.WEST,
							SimConfig.speed),
					true);
		} else if (e.getSource() == addW2Car) {
			SimulatorManager.addCar(
					2-1, 
					Direction.WEST, 
					new SimulatedTrack(
							new Point(SimConfig.simDisplayWidth,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.WEST,
							SimConfig.speed),
					true);
		}
		
	}
}
