package frontend.app;

import java.lang.reflect.Field;

import backend.repository.jdbc.ConnectionManager;
import common.PDFContainer;
import backend.model.PDF;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;



public class Main extends Application {
	
	public static void initializeData() {
		/**
		 * Define name and type of PDF class attributes
		 */
		Field[] fields = PDF.class.getDeclaredFields();
		PDFContainer.attrNo = fields.length;
		PDFContainer.PDFAttrNames = new String[PDFContainer.attrNo];
		PDFContainer.PDFAttrTypes = new Class[PDFContainer.attrNo];
		
		for(int i = 0;i<PDFContainer.attrNo;i++) {
			fields[i].setAccessible(true);
			PDFContainer.PDFAttrNames[i] = fields[i].getName();
			PDFContainer.PDFAttrTypes[i] = fields[i].getType();
		}
	}

	public static void main(String[] args) {
		/**
		 * Define name and type of PDF class attributes
		 */
		initializeData();
		
		/**
		 * Close application
		 */
		Runtime.getRuntime().addShutdownHook(new Thread() 
		{            
			public void run() {                
				ConnectionManager.getInstance().closeConnection();
			}        
		});
		
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(Main.class.getResource("../gui/ScientificArticleApp2.fxml"));
		AnchorPane myApp = (AnchorPane) loader.load();
		Scene scene = new Scene(myApp);
		primaryStage.setScene(scene);
		primaryStage.show();
		scene.getStylesheets().add(getClass().getResource("../gui/styles.css").toExternalForm());	
	}
}
