package simulator;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Point;

import application.TrafficController;
import config.SimConfig;
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
	public static JLabel nLabel, sLabel, eLabel, wLabel;
	public static JButton addN1Car, addN2Car, addS1Car, addS2Car, addE1Car, addE2Car, addW1Car, addW2Car,
	addN1Fire, addN2Fire, addS1Fire, addS2Fire, addE1Fire, addE2Fire, addW1Fire, addW2Fire;
	public static JCheckBox setOnDemand;
	
	
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
		
		nLabel = new JLabel("<html><font size=4><b>NORTHBOUND</b></html>");
		nLabel.setBounds(450, 450, 125, 50);
		setOnDemand = new JCheckBox("Use On Demand Logic", false);
		setOnDemand.setBounds(435, 420, 200, 50);
		addN1Car = new JButton("add N1 Car");
		addN1Car.setBounds(400, 500, 100, 50);
		addN2Car = new JButton("add N2 Car");
		addN2Car.setBounds(400, 550, 100, 50);
		addN1Fire = new JButton("add N1 Fire");
		addN1Fire.setForeground(Color.red);
		addN1Fire.setBounds(500, 500, 100, 50);
		addN2Fire = new JButton("add N2 Fire");
		addN2Fire.setForeground(Color.red);
		addN2Fire.setBounds(500, 550, 100, 50);
		
		sLabel = new JLabel("<html><font size=4><b>SOUTHBOUND</b></html>");
		sLabel.setOpaque(true);
		sLabel.setBounds(50, 100, 110, 50);
		addS1Car = new JButton("add S1 Car");
		addS1Car.setBounds(0, 0, 100, 50);
		addS2Car = new JButton("add S2 Car");
		addS2Car.setBounds(0, 50, 100, 50);
		addS1Fire = new JButton("add S1 Fire");
		addS1Fire.setForeground(Color.red);
		addS1Fire.setBounds(100, 0, 100, 50);
		addS2Fire = new JButton("add S2 Fire");
		addS2Fire.setForeground(Color.red);
		addS2Fire.setBounds(100, 50, 100, 50);
		
		eLabel = new JLabel("<html><font size=4><b>EASTBOUND</b></html>");
		eLabel.setBounds(50, 450, 125, 50);
		addE1Car = new JButton("add E1 Car");
		addE1Car.setBounds(0, 500, 100, 50);
		addE2Car = new JButton("add E2 Car");		
		addE2Car.setBounds(0, 550, 100, 50);
		addE1Fire = new JButton("add E1 Fire");
		addE1Fire.setForeground(Color.red);
		addE1Fire.setBounds(100, 500, 100, 50);
		addE2Fire = new JButton("add E2 Fire");
		addE2Fire.setForeground(Color.red);
		addE2Fire.setBounds(100, 550, 100, 50);
		
		wLabel = new JLabel("<html><font size=4><b>WESTBOUND</b></html>");
		wLabel.setBounds(450, 100, 125, 50);
		addW1Car = new JButton("add W1 Car");
		addW1Car.setBounds(400, 0, 100, 50);
		addW2Car = new JButton("add W2 Car");
		addW2Car.setBounds(400, 50, 100, 50);
		addW1Fire = new JButton("add W1 Fire");
		addW1Fire.setForeground(Color.red);
		addW1Fire.setBounds(500, 0, 100, 50);
		addW2Fire = new JButton("add W2 Fire");
		addW2Fire.setForeground(Color.red);
		addW2Fire.setBounds(500, 50, 100, 50);

		setOnDemand.addActionListener(this);
		
		addN1Car.addActionListener(this);
		addN2Car.addActionListener(this);
		addN1Fire.addActionListener(this);
		addN2Fire.addActionListener(this);
		
		addS1Car.addActionListener(this);
		addS2Car.addActionListener(this);
		addS1Fire.addActionListener(this);
		addS2Fire.addActionListener(this);
		
		addE1Car.addActionListener(this);
		addE2Car.addActionListener(this);
		addE1Fire.addActionListener(this);
		addE2Fire.addActionListener(this);
		
		addW1Car.addActionListener(this);
		addW2Car.addActionListener(this);
		addW1Fire.addActionListener(this);
		addW2Fire.addActionListener(this);
		
		frame.add(setOnDemand);
		frame.add(nLabel);
		frame.add(addN1Car);
		frame.add(addN2Car);
		frame.add(addN1Fire);
		frame.add(addN2Fire);
		
		frame.add(sLabel);
		frame.add(addS1Car);
		frame.add(addS2Car);
		frame.add(addS1Fire);
		frame.add(addS2Fire);
		
		frame.add(eLabel);
		frame.add(addE1Car);
		frame.add(addE2Car);
		frame.add(addE1Fire);
		frame.add(addE2Fire);
		
		frame.add(wLabel);
		frame.add(addW1Car);
		frame.add(addW2Car);
		frame.add(addW1Fire);
		frame.add(addW2Fire);
		
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
		} else if (e.getSource() == addN1Fire) {
				SimulatorManager.addCar(
						1-1, 
						Direction.NORTH, 
						new SimulatedTrack(
								new Point(0,SimConfig.simDisplayHeight), 
								SimulatorManager.simulatedCarsCounter++, 
								Direction.NORTH,
								SimConfig.speed,
								3),
						true);
			} else if (e.getSource() == addN2Fire) {
				SimulatorManager.addCar(
						2-1, 
						Direction.NORTH, 
						new SimulatedTrack(
								new Point(0,SimConfig.simDisplayHeight), 
								SimulatorManager.simulatedCarsCounter++, 
								Direction.NORTH,
								SimConfig.speed,
								3),
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
		} else if (e.getSource() == addS1Fire){
				SimulatorManager.addCar(
						1-1, 
						Direction.SOUTH, 
						new SimulatedTrack(
								new Point(0,0), 
								SimulatorManager.simulatedCarsCounter++, 
								Direction.SOUTH,
								SimConfig.speed,
								3),
						true);
		} else if (e.getSource() == addS2Fire) {
			SimulatorManager.addCar(
					2-1, 
					Direction.SOUTH, 
					new SimulatedTrack(
							new Point(0,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.SOUTH,
							SimConfig.speed,
							3),
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
		} else if (e.getSource() == addE1Fire) {
			SimulatorManager.addCar(
					1-1, 
					Direction.EAST, 
					new SimulatedTrack(
							new Point(0,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.EAST,
							SimConfig.speed,
							3),
					true);
		} else if (e.getSource() == addE2Fire) {
			SimulatorManager.addCar(
					2-1, 
					Direction.EAST, 
					new SimulatedTrack(
							new Point(0,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.EAST,
							SimConfig.speed,
							3),
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
		} else if (e.getSource() == addW1Fire) {
			SimulatorManager.addCar(
					1-1, 
					Direction.WEST, 
					new SimulatedTrack(
							new Point(SimConfig.simDisplayWidth,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.WEST,
							SimConfig.speed,
							3),
					true);
		} else if (e.getSource() == addW2Fire) {
			SimulatorManager.addCar(
					2-1, 
					Direction.WEST, 
					new SimulatedTrack(
							new Point(SimConfig.simDisplayWidth,0), 
							SimulatorManager.simulatedCarsCounter++, 
							Direction.WEST,
							SimConfig.speed,
							3),
					true);
		} else if (e.getSource() == setOnDemand)
		{
			if (setOnDemand.isSelected())
			{
				SimulatorManager.trafficController.ChangeSignalLogicConfiguration(TrafficController.SignalLogicConfiguration.OnDemand);
				System.out.println("Setting controller logic to: OnDemand");
			}
			else
			{
				SimulatorManager.trafficController.ChangeSignalLogicConfiguration(TrafficController.SignalLogicConfiguration.FixedTimers);
				System.out.println("Setting controller logic to: FixedTimers");
			}
		}
		
	}
}