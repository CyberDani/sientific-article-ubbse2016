package frontend.gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import backend.model.PDF;
import frontend.app.Main;
import frontend.app.TextProcessor;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class StatisticsFXController {
	
	TextProcessor tp;
	
	@FXML
	private Button saveStat;
	
	@FXML
	private Label pdfNameValue;
	
	@FXML
	private Label isScientific;
	
	@FXML
	private Label avgWordsValue;
	
	@FXML
	private Label pageNumberValue;
	
	@FXML
	private Label mostUsedFontValue;
	
	@FXML
	private Label avgRowParagraphValue;
	
	@FXML
	private Label bibliographyValue;
	
	@FXML
	private Label numOfImgValue;
	
	@FXML
	private Button backButton;
	
	/**
	 * Initializes the page
	 * @param tp - needs the processed PDF
	 */
	void initializePage(TextProcessor tp){
		this.tp=tp;
		setLabels();
	}
	
	/**
	 * Builds the statistics into a string
	 * @return all statistics in a string
	 */
	private String buildStatisticsString(){
		PDF myPDF=tp.getPDF();
		String statistics="PDF name:"+myPDF.getPath().split("\\\\")[5]+System.getProperty("line.separator")
						+"Is scientific:"+System.getProperty("line.separator")
						+"Page number:"+myPDF.getPagesNr()+System.getProperty("line.separator")
						+"Average words in a row:"+myPDF.getWordsRow()+System.getProperty("line.separator")
						+"Average row/paragraph:"+myPDF.getAvgRowInParagraph()+System.getProperty("line.separator")
						+"Most used font size:"+myPDF.getFontSize()+System.getProperty("line.separator")
						+"Number of images:"+myPDF.getImgNum()+System.getProperty("line.separator")
						+"Bibliography available:"+myPDF.getBibliography();
		
		return statistics;
	}
	
	/**
	 * Save statistics in file
	 */
	@FXML
	public void saveStatistics(){
		 Stage stage= (Stage) saveStat.getScene().getWindow();
		 FileChooser fileChooser = new FileChooser();
		 
		 FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
		 fileChooser.getExtensionFilters().add(extFilter);
		 File file = fileChooser.showSaveDialog(stage);
		 
		 if(file != null){
			 try {
				String data=buildStatisticsString();
	            FileWriter fileWriter = null;
	            fileWriter = new FileWriter(file);
	            fileWriter.write(data);
	            fileWriter.close();
	        } catch (IOException ex) {
	        	//logger here later
	      }
		}
	}
	
	/**
	 * Set labels on GUI with the corresponding value
	 */
	private void setLabels(){
		
		PDF myPDF=tp.getPDF();
		
		String[] pdfName = myPDF.getPath().split("\\\\");
		pdfNameValue.setText(pdfName[pdfName.length-1]);
		//isScientific.setText();
		pageNumberValue.setText(Integer.toString(myPDF.getPagesNr()));
		avgWordsValue.setText(Double.toString(myPDF.getWordsRow()));
		avgRowParagraphValue.setText(Integer.toString(myPDF.getAvgRowInParagraph()));
		mostUsedFontValue.setText(Double.toString(myPDF.getFontSize()));
		numOfImgValue.setText(Integer.toString(myPDF.getImgNum()));
		bibliographyValue.setText(Boolean.toString(myPDF.getBibliography()));
	}
	
	/**
	 * Sets the mainpage scene
	 */
	@FXML
	private void backToMainPage(){
		 Stage stage= (Stage) backButton.getScene().getWindow();
		 FXMLLoader loader = new FXMLLoader();
		 loader.setLocation(Main.class.getResource("../gui/ScientificArticleApp2.fxml"));
		 AnchorPane myApp;
		try {
			 myApp = (AnchorPane) loader.load();
			 Scene scene = new Scene(myApp);
			 stage.setScene(scene);
			 stage.show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}