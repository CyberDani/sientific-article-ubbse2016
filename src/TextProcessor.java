import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;


public class TextProcessor {

	private static int pageNumber;
	private static float avgWordsInRow;
	private static int line=0;
	private static int wordInLine;
	private static PDDocument pd = null;
	private static BufferedWriter wr;
	private static StringBuilder sb = null;
	private static String text;
	private static boolean bibliography;
	private static String titleFontSize;
	private static String titleFontFamily;
	
	private static float mostUsedSubTitleFontSize;
	private static float mostUsedFontSizeInPDF;
	private static float[][] pdfFontsWithRows;
	private static String[][] subtitleFontSizeAndRow;
	
	
	private static List<FontAndRow> pdfData = new ArrayList<FontAndRow>();
	
	public static void printStatistics(){
		System.out.println("Page number:" + pageNumber);
		System.out.println("Average words/row:" + avgWordsInRow);
		System.out.println("Bibliography available:" + bibliography);
		System.out.println("Title font-family:"+titleFontFamily);
		System.out.println("Title font-size:"+titleFontSize);
		System.out.println("Most used font-size:"+mostUsedFontSizeInPDF);
	}
	
	public static boolean bibliographyExistence(String [] rows){

		for (int i = 0; i< rows.length; i++) {
			if(rows[i].contains("References")){
				//an extra character is added at the end of the row, we need the text without it
				String row=rows[i].substring(0,rows[i].length()-1); 
				if(row.matches("\\[.*\\]References"))
					return true;
			}
		}
		
		return false;
		
	}
	
	/*
	 * Extracts data between [] and splits it by
	 * @param row row to be processed
	 * Output param: string[] with 1st data: result[0]-> font family, result[1] -> font size
	 */
	public static String[] extractData(String row){
		//String result = row.substring(row.indexOf("[")+1,row.lastIndexOf("]"));
		String result = row.substring(row.lastIndexOf("[")+1,row.lastIndexOf("]"));
		return result.split(",");
	}
	
	/*
	 * The data is number+fontFamily, we only need the font-family
	 * @param data data to process
	 */
	public static String getFontFamily(String data){
		String[] fontFamily=data.split("\\+");
		return fontFamily[1];
	}
	
	public static void printRows(String[] rows){
		for(int i=0; i<rows.length; i++)
			System.out.println(rows[i]);
	}
	
	public static void getFontSizeWithNumberOfRows(String[] allRows){
		int length, iterator, counter, iterHelp, rowCounter;
		String forSubtitles, replacement, cleanedRow;
		Pattern patternForRegex;
		Matcher match;
		
		forSubtitles = "";
		replacement = " ";
		length = allRows.length;
		rowCounter = 0;
		iterator = 0;
		counter = 0;

		patternForRegex = Pattern.compile("\\[[a-zA-Z0-9+-]+,[0-9]+\\.+[0-9]+\\]");		// containing one or more font entries
		
		for(int i=0; i<length; i++){										// counting the number of rows till another font data entry
			iterator = i;
			counter = 0;
			match = patternForRegex.matcher(allRows[i].toString());
			
			if(match.find())
			{
				// checking if the followed row is not starting or containing another font entry => so that row belongs to the previous font entry
				if((i+1<length) && (patternForRegex.matcher(allRows[i+1].toString()).find() == false)){

					iterHelp = i+1;
					while((patternForRegex.matcher(allRows[iterHelp].toString()).find() == false)){
						
						rowCounter++;
						counter++;
						if(counter <= 3)	// to store the first 3 row
						{
							cleanedRow = allRows[i].replaceAll(patternForRegex.toString(), replacement);	// cleaning the row from font entries
							forSubtitles += cleanedRow + " ";
						}
						
						i++;
						iterHelp = i;
					}

					match = patternForRegex.matcher(allRows[iterator].toString());
					if(match.find())
					{
						String data = match.group(match.groupCount());
						String[] fontData = extractData(data);
						titleFontSize = fontData[1];
						
						FontAndRow fontAndRow = new FontAndRow();
						fontAndRow.setFontSize(titleFontSize);
						fontAndRow.setSomeRows(forSubtitles);
						fontAndRow.setNumberOfRows(rowCounter);
						pdfData.add(fontAndRow);
					}
					rowCounter = 0;				// for reusing them
					if (counter != 0)			// to get the row before
						i--;
					
					forSubtitles = "";
				}
				else {	// if the row which contains font entry isn't followed by a row without font entry
					match = patternForRegex.matcher(allRows[iterator].toString());
					if(match.find())
					{
						rowCounter++;
						String data = match.group(match.groupCount());
						String[] fontData = extractData(data);
						titleFontSize = fontData[1];
					
						FontAndRow fontAndRow = new FontAndRow();
						fontAndRow.setFontSize(titleFontSize);
						
						cleanedRow = allRows[i].replaceAll(patternForRegex.toString(), replacement);
						
						fontAndRow.setSomeRows(cleanedRow);
						fontAndRow.setNumberOfRows(rowCounter);
						pdfData.add(fontAndRow);
					}
					rowCounter = 0;
				}
			}
		}
		System.out.println("--------------------------------------------------------------------------------------");
		for(FontAndRow fr : pdfData){
			System.out.println("Font-meret: " + fr.getFontSize() + "  Elso harom sor: " + fr.getSomeRows() + "  Sorok szama: " + fr.getNumberOfRows());
		}
		System.out.println("--------------------------------------------------------------------------------------");
	}
	
	public static float getTheMostUsedFont(){
		
		int length, actualLength;
		float sum, mostUsedF;
		float[][] temp;
		boolean exists;
		
		sum = 0;
		mostUsedF = 0;
		length = 0;
		actualLength = 0;
		exists = false;
		
		length = pdfData.size();
		
		temp = new float[pdfData.size()][2];					// we need only two columns for size and number of rows
		
		for (FontAndRow fdata: pdfData){
			
			for(int i=0; i<length; i++){						// if the 2D array contains the new font size
				if(temp[i][0] == Float.parseFloat(fdata.getFontSize())){
					temp[i][1] += fdata.getNumberOfRows();
					exists = true;
					break;
				}
			}
			if(!exists){										// if doesn't contains the font size
				for(int i=0; i<length; i++){
					if(temp[i][0] == 0.0){
						temp[i][0] = Float.parseFloat(fdata.getFontSize());
						temp[i][1] += fdata.getNumberOfRows();
						actualLength++;
						break;
					}
				}
			}
			exists = false;
		}
		
		pdfFontsWithRows = new float[actualLength][2];
		
		sum = temp[0][1];
		for(int i=0; i<actualLength; i++){						// saving the data to a global 2D array
			pdfFontsWithRows[i][0] = temp[i][0];
			pdfFontsWithRows[i][1] = temp[i][1];
			
			if((i+1 < actualLength) && (sum < temp[i+1][1])){	// and searching for the most used font-size
				sum = temp[i+1][1];
				mostUsedF = temp[i+1][0];
			}
		}

		return mostUsedF;
	}
	
	public static float getSubtitleFontSize(){
		float subFontSize;
		int numberOfRows, numberOfChars, length, index;
		String[][] temp;
		
		length = pdfData.size();
		temp = new String[length][2];
		
		index = 0;
		subFontSize = 0;
		numberOfRows = 3;
		numberOfChars = 5;
		
		for (FontAndRow fData: pdfData){
			
			if(mostUsedFontSizeInPDF < Float.parseFloat(fData.getFontSize())){	// the subtitle's font-size must be bigger than the most used font-size
				if(numberOfRows >= fData.getNumberOfRows()){					// the subtitle's row's number can be maximum 3	
					if(numberOfChars <= fData.getSomeRows().length()){			// the subtitle must be at least 5 character long
						temp[index][0] = fData.getFontSize().toString();
						temp[index][1] = fData.getSomeRows();
						index++;
					}
				}
			}
		}
		
		subtitleFontSizeAndRow = new String[index][2];
		
		for(int i=0; i<index; i++){
			subtitleFontSizeAndRow[i][0] = temp[i][0];
			subtitleFontSizeAndRow[i][1] = temp[i][1];
		}
		
		System.out.println("///////////////////////////////////////////////////////////////////////////////////////////");
		System.out.println("Cimek: ");
		for(int j=0; j<index; j++){
			System.out.println("Font-meret: "+subtitleFontSizeAndRow[j][0]+" Tartalom: "+ subtitleFontSizeAndRow[j][1]);
		}
		System.out.println("///////////////////////////////////////////////////////////////////////////////////////////");
		
		return subFontSize;
	}
	
	public static void processTextByRow(){
		String[] rows = text.split("\n");
		//printRows(rows);
		String[] fontData=extractData(rows[0]);
		titleFontFamily=getFontFamily(fontData[0]);
		titleFontSize=fontData[1];
		
		getFontSizeWithNumberOfRows(rows);
		mostUsedFontSizeInPDF = getTheMostUsedFont();
		mostUsedSubTitleFontSize = getSubtitleFontSize();
		
		avgWordsInRow=numberOfWords(rows);
		bibliography=bibliographyExistence(rows);
	}
	
	public static float numberOfWords(String[] rows){
		char c;
		int count=1;
		float sum=(float) 0.0;

		for (int i = 0; i< rows.length; i++) {
			//number of words=number of spaces+1
			count=1;
			for(int j=0;j<rows[i].length();j++){
				c = rows[i].charAt(j);
				if(c == ' '){
					count++;
				}
			}
			
			sum+=count;
		}
		//Every page has a number at the end, that don't counts as a row
		sum=sum - pageNumber;
		int rowNumber=rows.length - pageNumber;
		
		return sum / rowNumber;
	}
	
	public static void processText(){
	
		try {
				File inputFile = new File("E:/enyim/III. EV/2016-2017 I. felev/Csoportos projekt/Canvasrol/CiteSeerX.pdf");
				pd = PDDocument.load(inputFile);
				pageNumber=pd.getNumberOfPages();
				PDFTextStripper stripper = new PDFTextStripper() {
				    String prevBaseFont;

				    protected void writeString(String text, List<TextPosition> textPositions) throws IOException
				    {
				        StringBuilder builder = new StringBuilder();

				        for (TextPosition position : textPositions)
				        {
				            String baseFont = position.getFont().getName();
				            if (baseFont != null && !baseFont.equals(prevBaseFont))
				            {
				            	float size=position.getFontSizeInPt();
				                builder.append('[').append(baseFont).append(',').append(size+"").append(']');
				                prevBaseFont = baseFont;
				            }
				            builder.append(position);
				        }

				        writeString(builder.toString());
				    }
				};
				
				sb = new StringBuilder();
				// Add text to the StringBuilder from the PDF
				stripper.setStartPage(1); // Start extracting from page 3
				stripper.setEndPage(pageNumber); // Extract till page 4
				sb.append(stripper.getText(pd));
				text=sb.toString();

				if (pd != null) {
					pd.close();
				}
				
				processTextByRow();
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}	
}