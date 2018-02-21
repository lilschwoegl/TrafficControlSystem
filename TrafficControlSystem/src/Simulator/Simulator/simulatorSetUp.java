package Simulator;

import java.awt.Graphics;
import java.awt.image.BufferStrategy;

public class simulatorSetUp implements Runnable {
	
	private Thread thread;
	private Display display;
	private String title;
	private int width, height;
	private BufferStrategy buffer;
	private Graphics g;
	private simulatorManager manager;
	
	public simulatorSetUp(String title, int width, int height) {
		this.title = title;
		this.width = width;
		this.height = height;
	}
	
	public void init(){
		display = new Display(title,width,height);
		manager = new simulatorManager();
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
}
