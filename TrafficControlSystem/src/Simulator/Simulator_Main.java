package Simulator;

public class Simulator_Main {
	public static void main(String[]args) {
		//call Display to create a JFrame window with inputs title, width, height
		simulatorSetUp simulator = new simulatorSetUp("Traffic Controller Simulator", 600, 600);
		simulator.start();
	}
}
