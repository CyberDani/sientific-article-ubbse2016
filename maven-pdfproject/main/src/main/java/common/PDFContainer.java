package common;

import java.util.Arrays;
import java.util.List;

import backend.weka.DataLearnerPredictor;
import backend.weka.LearningDataSet;

/**
* Contains global variables and settings used in the application.
*/
public class PDFContainer {
	/**
	* Contains the name of the PDF attributes.
	*/
	public static String PDFAttrNames[];
	/**
	* Contains the type of the PDF attributes.
	*/
	public static Class PDFAttrTypes[];
	/**
	* Contains the number of attributes in the PDF class.
	*/
	public static int attrNo;
	/**
	* Global LearningDataSet variable.
	*/
	public static LearningDataSet lds = null;
	/**
	* Global DataLearnerPredictor variable.
	*/
	public static DataLearnerPredictor dlp = null;
	/**
	* If a PDF has no subtitles at all, it will contain a single element 
	* array with this string value;
	*/
	public static String nullString = "NO DATA";
	/**
	* Contains that attributes from the PDF entity which we doesn`t 
	* like to be loaded in the training set(<i>e.g. string values or 
	* useless information</i>). The last value has to be the
	* subtitles.
	*/
	public static List<String> unused = Arrays.asList("_id","path","subtitles");
}
