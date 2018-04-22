package application;
	
import java.awt.Dimension;
import java.awt.Toolkit;

import org.opencv.core.Core;

import clientserver.ServerManager;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import simulator.SimConfig;
import simulator.simulatorSetUp;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try
		{
			// load the FXML resource
			FXMLLoader loader = new FXMLLoader(getClass().getResource("SystemUI.fxml"));
			// store the root element so that the controllers can use it
			BorderPane rootElement = (BorderPane) loader.load();
			// create and style a scene
			Scene scene = new Scene(rootElement, SimConfig.videoDisplayWidth, SimConfig.videoDisplayHeight);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			// create the stage with the given title and the previously created
			// scene
			primaryStage.setTitle("Video Processing");
			primaryStage.setScene(scene);
			
			// get screen size
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			
			// set window position
			primaryStage.setX(screenSize.getWidth() / 2);
			primaryStage.setY(screenSize.getHeight() / 2 - scene.getHeight() / 2);
			
			// show the GUI
			primaryStage.show();
			
			//Config.runSimulator = false;
			if (SimConfig.runSimulator)
			{
				// create a new thread for the simulator
				Thread simulator = new Thread()
				{
					public void run()
					{
						int width = (int)SimConfig.simDisplayWidth;
						int height = (int)SimConfig.simDisplayHeight;
						int posX = (int) (screenSize.getWidth() / 2 - width);
						int posY = (int) (screenSize.getHeight() / 2 - height / 2);
						simulatorSetUp simulator = new simulatorSetUp("Traffic Controller Simulator", width, height, posX, posY);
						simulator.start();
					}
				};
				
				
				// start the simulator
				simulator.start();
			}
			
			// start the server for the database
			Thread serverThread = new Thread()
					{
						public void run()
						{
							ServerManager.getInstance().run();
						}
					};
			serverThread.start();
			
			
			// set the proper behavior on closing the application
			SystemUIController controller = loader.getController();
			primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we)
				{
					controller.setClosed();
				}
			}));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}
}
