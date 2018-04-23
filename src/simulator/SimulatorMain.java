package simulator;

import java.awt.Graphics;
import java.awt.image.BufferStrategy;

public class SimulatorMain implements Runnable{
	
	private Thread thread;
	private Display display;
	private String title;
	private int width, height;
	private int posX, posY;
	private BufferStrategy buffer;
	private Graphics g;
	private SimulatorManager manager;
	
	public SimulatorMain(String title, int width, int height) {
		this(title, width, height, 0, 0);
	}
	
	public SimulatorMain(String title, int width, int height, int posX, int posY)
	{
		this.title = title;
		this.width = width;
		this.height = height;
		this.posX = posX;
		this.posY = posY;
	}
	
	public void init(){
		display = new Display(title,width,height,posX,posY);
		manager = new SimulatorManager();
		manager.init();
	}
	
	public synchronized void start(){
		if(thread == null){
			thread = new Thread(this);
			thread.start();
		}
	}
	
	public synchronized void stop(){
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void tick(){
		manager.tick();
	}
	
	public void render(){
		buffer = Display.canvas.getBufferStrategy();
		if(buffer == null){
			Display.canvas.createBufferStrategy(3);
			return;
		}
		g = buffer.getDrawGraphics();
		g.clearRect(0, 0, width, height);
		
		//draw
		manager.render(g);
		
		//draw end
		buffer.show();
		g.dispose();
	}
	
	public void run(){
		init();
		int fps = 50;
		double timePerTick = 1000000000/fps;
		double delta = 0;
		long current = System.nanoTime();
		
		
		//loop to draw square crossing the using tick method
		while(true){
			delta = delta + (System.nanoTime()-current)/timePerTick;
			current = System.nanoTime();
			if(delta>=1){
				tick();
				render();
				delta--;
			}
			
		}
		
	}
	
	public static void main(String[]args) {
		//call Display to create a JFrame window with inputs title, width, height
		SimulatorMain simulator = new SimulatorMain("Traffic Controller Simulator", 600, 600);
		simulator.start();
	}
}
