package backend.weka;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import common.PDFContainer;
import common.Scientific;
import backend.model.PDF;
import weka.clusterers.Cobweb;

public class LearningDataSet {

	/**
	* Contains the attributes(structure) of a row(Instance).
	* In our case one row/Instance means all the information of a single PDF file.
	*/
	private ArrayList<Attribute> atts = null;
	/**
	* Contains all the information in multiple rows(Instance) by following the
	* structure of attributes generated by the method generateFormat().
	*/
	private Instances data = null;
	/**
	* This variable defines the 'true'/'false' boolean values as a nominal attribute.
	* With this variable you can easily derive an attribute value from a boolean 
	* variable.
	*/
	private ArrayList<String> attVals;
	DateFormat generalDateFormat;
	
	/**
	* Initialize required essentials.
	*/
	private void init() {
		DateFormat generalDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		
		attVals = new ArrayList<String>();
	    attVals.add("true");
	    attVals.add("false");
	}
	
	/**
	* Generate the format(structure) of a single instance(row) by building up 
	* the attributes from a class via reflection.
	* <br /><br />
	* <b>Handled types:</b> int, float, double, long, Date, boolean, Boolean,
	* String, int[], float[], double[], long[], String[].
	*/
	private void generateFormat() {
		atts = new ArrayList<Attribute>();
		
		// 1. set up attributes
		for(int i=0;i<PDFContainer.attrNo;++i){
			
			if(PDFContainer.PDFAttrTypes[i] == int.class || 
					PDFContainer.PDFAttrTypes[i] == float.class ||
						PDFContainer.PDFAttrTypes[i] == double.class ||
							PDFContainer.PDFAttrTypes[i] == long.class)
			{
				// - numeric
				atts.add(new Attribute(PDFContainer.PDFAttrNames[i]));
			}else if(PDFContainer.PDFAttrTypes[i] == Date.class){
				// - date
			    atts.add(new Attribute(PDFContainer.PDFAttrNames[i], "yyyy-MM-dd"));
			}else if(PDFContainer.PDFAttrTypes[i] == boolean.class ||
					PDFContainer.PDFAttrTypes[i] == Boolean.class ){
				// - nominal
			    atts.add(new Attribute(PDFContainer.PDFAttrNames[i], attVals));
			}else if(PDFContainer.PDFAttrTypes[i] == String.class){
				// - string
			    atts.add(new Attribute(PDFContainer.PDFAttrNames[i], 
			    		(ArrayList<String>) null));
			}else if(PDFContainer.PDFAttrTypes[i] == int[].class || 
						PDFContainer.PDFAttrTypes[i] == float[].class ||
							PDFContainer.PDFAttrTypes[i] == double[].class ||
								PDFContainer.PDFAttrTypes[i] == long[].class){
				// - relational
				ArrayList<Attribute> attsRel = new ArrayList<Attribute>();
			    // -- numeric
			    attsRel.add(new Attribute("Value"));
			    Instances dataRel = new Instances(PDFContainer.PDFAttrNames[i], attsRel, 0);
			    atts.add(new Attribute(PDFContainer.PDFAttrNames[i], dataRel, 0));
			}else if(PDFContainer.PDFAttrTypes[i] == String[].class){
				// - relational
				ArrayList<Attribute> attsRel = new ArrayList<Attribute>();
				// - string
				attsRel.add(new Attribute("Value", 
						(ArrayList<String>) null));;
			    Instances dataRel = new Instances(PDFContainer.PDFAttrNames[i], attsRel, 0);
			    atts.add(new Attribute(PDFContainer.PDFAttrNames[i], dataRel, 0));
			}	  
		}
		
	    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
	    Date date = new Date();
	    
		// create Instances object
	    data = new Instances("PDFCollection_"+dateFormat.format(date).toString(), atts, 0);
	}
	
	/**
	* Building up the whole class from an arff(<b>A</b>ttribute 
	* <b>R</b>elational <b>F</b>ile <b>F</b>ormat) file.
	* <br />
	* The file has to reperesents informations about PDF files with the current 
	* using attributes, but this has been not verified by the application yet.
	* 
	* @param path Contains the path of an .arff file.
	*/
	public LearningDataSet(String path) {
		ArffLoader loader = new ArffLoader();
		try {
			loader.setSource(new File(path));
			data = loader.getDataSet();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	* Initialize an empty instance of a class by setting up the structure(format) 
	* of a single row(Instance) derived from the PDF class via reflection.
	* <br /><br />
	* <b>Handled types:</b> int, float, double, long, Date, boolean, Boolean,
	* String, int[], float[], double[], long[], String[].
	*/
	public LearningDataSet() {
		init();
		generateFormat();
	}
	
	/**
	* Add a single PDF instance to the training set via reflection.
	* <br /><br />
	* <b>Handled types:</b> int, float, double, long, Date, boolean, Boolean,
	* String, int[], float[], double[], long[], String[].
	* 
	* @param pdf this is an instance of PDF class containing information 
	* about a pdf file.
	*/
	public void addPDF(PDF pdf) {
		
		Field[] fields = PDF.class.getDeclaredFields();
		
		for(int j = 1; j< PDFContainer.attrNo; ++j){
			fields[j].setAccessible(true);
		}
		
		double[] vals = new double[data.numAttributes()];
		
		for(int i = 1; i< PDFContainer.attrNo; ++i){
			
			//fields[j].get(pdf);
			
			if(PDFContainer.PDFAttrTypes[i] == int.class || 
					PDFContainer.PDFAttrTypes[i] == float.class ||
						PDFContainer.PDFAttrTypes[i] == double.class ||
							PDFContainer.PDFAttrTypes[i] == long.class)
			{
				// - numeric
				try {
					vals[i-1] = ((Number)fields[i].get(pdf)).doubleValue();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}else if(PDFContainer.PDFAttrTypes[i] == Date.class){
				// - date
				try {
					try {
						vals[i-1] = data.attribute(i).parseDate(
								generalDateFormat.format(fields[i].get(pdf)).toString());
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}else if(PDFContainer.PDFAttrTypes[i] == boolean.class){
				// - nominal
				try {
					vals[i-1] = attVals.indexOf(fields[i].get(pdf).toString());
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}else if(PDFContainer.PDFAttrTypes[i] == Boolean.class){
				// - nominal
				try {
					Boolean boolVal = (Boolean)fields[i].get(pdf);
					if(boolVal != null){
						vals[i-1] = attVals.indexOf(boolVal.toString());
					}else{
						//...
					}
					
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}else if(PDFContainer.PDFAttrTypes[i] == String.class){
				// - string
				try {
					String strData = fields[i].get(pdf).toString();
					vals[i-1] = data.attribute(i-1).addStringValue(strData);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}else if(PDFContainer.PDFAttrTypes[i] == int[].class || 
						PDFContainer.PDFAttrTypes[i] == float[].class ||
							PDFContainer.PDFAttrTypes[i] == double[].class ||
								PDFContainer.PDFAttrTypes[i] == long[].class){
				// - relational
			    Instances dataRel = new Instances(data.attribute(i-1).relation(), 0);
			    double elements[] = null;
			    
			    try {
					elements = (double[]) fields[i].get(pdf);
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				}
			    
			    // -- add instances
			    double[] valsRel;
		
			    int n = elements.length;
			    
			    for(int j = 0;j<n;++j){
			    	valsRel = new double[1];
			    	valsRel[0] = elements[j];
				    dataRel.add(new DenseInstance(1.0, valsRel));
			    }
			    
			    vals[i-1] = data.attribute(i-1).addRelation(dataRel);
			}else if(PDFContainer.PDFAttrTypes[i] == String[].class){
				// - relational
			    Instances dataRel = new Instances(data.attribute(i-1).relation(), 0);
			    String elements[] = null;
			    
			    try {
					elements = (String[]) fields[i].get(pdf);
				} catch (IllegalArgumentException e1) {
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					e1.printStackTrace();
				}
			    
			    // -- add instances
			    double[] valsRel;
		
			    int n = elements.length;
			    
			    for(int j = 0;j<n;++j){
			    	valsRel = new double[1];
			    	valsRel[0] = dataRel.attribute(0).addStringValue(elements[j]);
				    dataRel.add(new DenseInstance(1.0, valsRel));
			    }
			    
			    vals[i-1] = data.attribute(i-1).addRelation(dataRel);
			    
			}else if(PDFContainer.PDFAttrTypes[i] == Scientific.class){
				///
			}
		}
		
		// add
	    data.add(new DenseInstance(1.0, vals));
	}
	
	/**
	* Add multiple PDF instances to the training set via reflection.
	* <br /><br />
	* <b>Handled types:</b> int, float, double, long, Date, boolean, Boolean,
	* String, int[], float[], double[], long[], String[].
	* 
	* @param pdf this is an array of PDF class containing information 
	* about multiple pdf files.
	*/
	public void addAllPDF(PDF[] pdf){
		int n = pdf.length;
		for(int i = 0;i<n;++i){
			addPDF(pdf[i]);
		}
	}
	
	/**
	* Writes the training set to the console in arff(<b>A</b>ttribute 
	* <b>R</b>elational <b>F</b>ile <b>F</b>ormat) format.
	*/
	public void write(){
		System.out.println(data);
	}
	
	/**
	* Convert training set into String in arff(<b>A</b>ttribute 
	* <b>R</b>elational <b>F</b>ile <b>F</b>ormat) format.
	*/
	@Override
	public String toString(){
		return data.toString();
	}
	
	/**
	* Get the subtitles of a single pdf from the training set. The \n \r ' characters 
	* will be removed. The spaces from the begginning of a subtitle will be also removed 
	* in the response.
	* 
	* @param index specifies the index of a row(Instance) in the training file.
	*/
	public ArrayList<String> getSubtitles(int index) {
	    ArrayList<String> answer = new ArrayList<String>();
	    
	    int subTitleAttrInd = 0;
	    while(common.PDFContainer.PDFAttrNames[++subTitleAttrInd].equals("subtitles")){}
	    
	    long l = data.size();

	    if(index<l && l >= 0)
	    {
	    	Instance inst = data.instance(index);
	    	Attribute attr = inst.attribute(subTitleAttrInd);
	    	String subtitles = inst.stringValue(attr);
	    	
	    	ArrayList<String> arrList = common.Tools.stringToArrList(subtitles);
	    	answer = arrList;
	    }

	    return answer;
	}
	
	/**
	* Get the subtitles of all the pdfs from the training set. The \n \r ' characters 
	* will be removed. The spaces from the begginning of a subtitle will be also removed 
	* in the response.
	* <br \><br \>
	* The response will contain <b>n</b> piece of <b>m</b> strings where <b>n</b> means 
	* the number of rows(Instance) in the training set and <b>m</b> means the number of 
	* subtitles in the nth row(Instance).
	*/
	public ArrayList<ArrayList<String>> getAllSubtitles() {
		
	    ArrayList<ArrayList<String>> answer = new ArrayList<ArrayList<String>>();
	    long l = data.size();
	    
	    for(long i = 0;i<l;++i)
	    {	    	
	    	ArrayList<String> arrList = getSubtitles((int)i);
	    	answer.add(arrList);
	    }

	    return answer;
	}
	
	/**
	* Get the number of rows(Instances) from the training set.
	*/
	public int getSize(){
		return data.size();
	}
	
	/**
	* Get the actual training set as an Instances class instance.
	*/
	public Instances getInstances(){
		return data;
	}
}
