package scientificArticle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class TextProcessor {
	private List<String> words = new ArrayList<String>();
	private HashMap<String, Integer> wordsOccurence = new HashMap<String, Integer>();
	private int numberOfWordsToInsert=0;
	private int pageNumber;
	private float avgWordsInRow;
	private PDDocument pd = null;
	private StringBuilder sb = null;
	private String text;
	private boolean bibliography;
	private String titleFontSize;
	private String titleFontFamily;
	private File file;
	private float mostUsedSubTitleFontSize;
	private float mostUsedFontSizeInPDF;
	private float[][] pdfFontsWithRows;
	private String[][] subtitleFontSizeAndRow=new String[500][500];
	private int averageNumberOfRowsInParagraph;
	private int numOfImages;

	private String subTitles[]; 
	private String path;
	private PDF pdfObj;

	private List<FontAndRow> pdfData = new ArrayList<FontAndRow>();
	private String[] rows;

	private int wordsInserted = 0;
	private HashMap<String, List<String>> stopWordList;
	
	
	public HashMap<String, List<String>> getStopWordList() {
		return stopWordList;
	}

	public void setStopWordList(HashMap<String, List<String>> stopWordList) {
		this.stopWordList = stopWordList;
	}

	public void setPDF(PDF pdf){
		this.pdfObj = pdf;
	}

	public PDF getPDF(){
		return pdfObj;
	}

	public TextProcessor() {
		System.out.println("TextProcessor objektum letrehozva...");
		setStopWordList(StopWords.stopWordList);
	}
	
	public TextProcessor(File file, Scientific scientific){
		setStopWordList(StopWords.stopWordList);
		processText(file);
		this.file = file;
		subTitles = new String[subtitleFontSizeAndRow.length];

		for(int i = 0; i<subTitles.length;++i){
			subTitles[i] = subtitleFontSizeAndRow[i][2];
		}

		try {
			path = this.file.getAbsolutePath();
			long fileSize = file.length()/1024;
			PDF pdf = new PDF(path, subTitles, pageNumber, avgWordsInRow, mostUsedFontSizeInPDF ,numOfImages,averageNumberOfRowsInParagraph,bibliography, fileSize, scientific);
			setPDF(pdf);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			//
		}
	}

	public void printStatistics(){
		System.out.println("Page number:" + pageNumber);
		System.out.println("Average words/row:" + avgWordsInRow);
		System.out.println("Bibliography available:" + bibliography);
		System.out.println("Title font-family:"+titleFontFamily);
		System.out.println("Title font-size:"+titleFontSize);
		System.out.println("Most used font-size:"+mostUsedFontSizeInPDF);
		System.out.println("Most used subtitle font-size:"+mostUsedSubTitleFontSize);
		System.out.println("Average number of rows:"+averageNumberOfRowsInParagraph);
	}

	public boolean bibliographyExistence(){

		for (int i = 0; i< rows.length; i++) {
			if(rows[i].contains("References") || rows[i].contains("Bibliography") || 
					rows[i].contains("References and notes") || rows[i].contains("Reference and note")){
				//an extra character is added at the end of the row, we need the text without it
				String row=rows[i].substring(0,rows[i].length()-1); 
				if(row.matches("\\[.*\\]References") || row.matches("\\[.*\\]Bibliography") ||
						row.matches("\\[.*\\]References and notes") || row.matches("\\[.*\\]Reference and note"))
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
	public String[] extractData(String row){
		//String result = row.substring(row.indexOf("[")+1,row.lastIndexOf("]"));
		String result = row.substring(row.lastIndexOf("[")+1,row.lastIndexOf("]"));
		return result.split(",");
	}

	/*
	 * The data is number+fontFamily, we only need the font-family
	 * @param data data to process
	 */
	public String getFontFamily(String data){
		String[] fontFamily=data.split("\\+");
		return fontFamily[1];
	}

	public void printRows(){
		for(int i=0; i<rows.length; i++)
			System.out.println(rows[i]);
	}

	public void getFontSizeWithNumberOfRows(){
		int length, iterator, counter, iterHelp, rowCounter;
		String forSubtitles, replacement, cleanedRow, rowFontSize;
		Pattern patternForRegex;
		Matcher match;

		forSubtitles = "";
		replacement = " ";
		length = rows.length;
		rowCounter = 0;
		iterator = 0;
		counter = 0;

		patternForRegex = Pattern.compile("\\[[a-zA-Z0-9+-]+,[0-9]+\\.+[0-9]+\\]");		// containing one or more font entries

		for(int i=0; i<length; i++){										// counting the number of rows till another font data entry
			iterator = i;
			counter = 0;
			match = patternForRegex.matcher(rows[i].toString());

			if(match.find())
			{
				// checking if the followed row is not starting or containing another font entry => so that row belongs to the previous font entry
				if((i+1<length) && (patternForRegex.matcher(rows[i+1].toString()).find() == false)){

					iterHelp = i+1;
					while((iterHelp<length) && (patternForRegex.matcher(rows[iterHelp].toString()).find() == false)){

						rowCounter++;
						counter++;
						if(counter <= 3)	// to store the first 3 row
						{
							cleanedRow = rows[i].replaceAll(patternForRegex.toString(), replacement);	// cleaning the row from font entries
							forSubtitles += cleanedRow + " ";
						}

						i++;
						iterHelp = i;
					}

					match = patternForRegex.matcher(rows[iterator].toString());
					if(match.find())
					{
						String data = match.group(match.groupCount());
						String[] fontData = extractData(data);
						rowFontSize = fontData[1];

						FontAndRow fontAndRow = new FontAndRow();
						fontAndRow.setFontSize(rowFontSize);
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
					match = patternForRegex.matcher(rows[iterator].toString());
					if(match.find())
					{
						rowCounter++;
						String data = match.group(match.groupCount());
						String[] fontData = extractData(data);
						rowFontSize = fontData[1];

						FontAndRow fontAndRow = new FontAndRow();
						fontAndRow.setFontSize(rowFontSize);

						cleanedRow = rows[i].replaceAll(patternForRegex.toString(), replacement);

						fontAndRow.setSomeRows(cleanedRow);
						fontAndRow.setNumberOfRows(rowCounter);
						pdfData.add(fontAndRow);
					}
					rowCounter = 0;
				}
			}
		}
		/*System.out.println("--------------------------------------------------------------------------------------");
		for(FontAndRow fr : pdfData){
			System.out.println("Font-meret: " + fr.getFontSize() + "  Elso/elso harom sor: " + fr.getSomeRows() + "  Sorok szama: " + fr.getNumberOfRows());
		}
		System.out.println("--------------------------------------------------------------------------------------");*/
	}

	public float getTheMostUsedFont(){

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

	public float getTheMostUsedSubtitleFontSize(){
		float sum, mostUsedFontForSubtitles;
		int numberOfRows, numberOfChars, length, index, actualLength;
		boolean exists;
		String[][] temp;
		float[][] subTitleFontSizeData;

		length = pdfData.size();
		temp = new String[length][3];	// [][0] font-size, [][1] number of rows, [][2] rows

		exists = false;
		index = 0;
		actualLength = 0;
		mostUsedFontForSubtitles = 0;
		//numberOfRows = 3;
		numberOfRows = 1;			// temporary trying to eliminate to much words
		numberOfChars = 5;

		for (FontAndRow fData: pdfData){

			if(mostUsedFontSizeInPDF < Float.parseFloat(fData.getFontSize())){	// the subtitle's font-size must be bigger than the most used font-size
				if(numberOfRows >= fData.getNumberOfRows()){					// the subtitle's row's number can be maximum 3	
					if(numberOfChars <= fData.getSomeRows().length()){			// the subtitle must be at least 5 character long
						temp[index][0] = fData.getFontSize().toString();
						temp[index][1] = Integer.toString(fData.getNumberOfRows());
						temp[index][2] = fData.getSomeRows();
						index++;
					}
				}
			}
		}

		subtitleFontSizeAndRow = new String[index][3];
		subTitleFontSizeData = new float[index][2];
		sum = Float.parseFloat(temp[0][1]);
		for(int i=0; i<index; i++){

			subtitleFontSizeAndRow[i][0] = temp[i][0];
			subtitleFontSizeAndRow[i][1] = temp[i][1];
			subtitleFontSizeAndRow[i][2] = temp[i][2];

			for(int k=0; k<index; k++){						// if the 2D array contains the new font size
				if(subTitleFontSizeData[k][0] == Float.parseFloat(temp[i][0])){
					subTitleFontSizeData[k][1] += Float.parseFloat(temp[i][1]);
					exists = true;
					break;
				}
			}
			if(!exists){										// if doesn't contains the font size
				for(int j=0; j<length; j++){
					if(subTitleFontSizeData[j][0] == 0.0){
						subTitleFontSizeData[j][0] = Float.parseFloat(temp[i][0]);
						subTitleFontSizeData[j][1] += Float.parseFloat(temp[i][1]);
						actualLength++;
						break;
					}
				}
			}
			exists = false;
		}
		sum = subTitleFontSizeData[0][1];
		for(int i=0; i<actualLength; i++){
			if((i+1<actualLength) && (sum < subTitleFontSizeData[i+1][1])){
				sum = subTitleFontSizeData[i+1][1];
				mostUsedFontForSubtitles = Float.parseFloat(temp[i+1][0]);
			}
		}

		/*System.out.println("///////////////////////////////////////////////////////////////////////////////////////////");
		System.out.println("Cimek: ");
		for(int j=0; j<index; j++){
			System.out.println("Font-meret: "+subtitleFontSizeAndRow[j][0]+" Tartalom: "+ subtitleFontSizeAndRow[j][2]);
		}
		System.out.println("///////////////////////////////////////////////////////////////////////////////////////////");*/

		return mostUsedFontForSubtitles;
	}

	public int getTheAverageRowFromParagraphs(){

		int avgRowByParagraph;
		int numberOfRowsInPDF = rows.length;
		int numberOfSubtitles = subtitleFontSizeAndRow.length-1;// one of the subTitle is the title so we don't need to count that in
		int sum = 0;

		sum = numberOfRowsInPDF - subtitleFontSizeAndRow.length;
		avgRowByParagraph = sum / numberOfSubtitles;

		return avgRowByParagraph;
	}

	/**
	* Tells if a given word is a stopword.
	*/
	public boolean isStopWord(String word){
		
		String firstLetter = word.substring(0, 1);
		List<String> list = stopWordList.get(firstLetter);
		
		if (list != null) {
			return list.contains(word);
		} else {
		    return false;
		}
	}
	
	private void putInHashMap(String word){
		if(!isStopWord(word)){
			Integer freq = wordsOccurence.get(word);

			if (freq == null) {
				if(wordsInserted<numberOfWordsToInsert){
					wordsOccurence.put(word,1);
					wordsInserted++;
				}
				else if(numberOfWordsToInsert==0){
					wordsOccurence.put(word,1);
				}
			} else {
				wordsOccurence.put(word,freq+1);
			}
		}	
	}

	private void countWordOccurence(String line){
		String[] words=line.split(" .!,;?><");
		for(String word:words){

			if(word.matches("[a-zA-z?]{4,}")){ //if it is a word or a world with ? in it, min 4 character words
				putInHashMap(word);
			}else if(word.matches("^[a-zA-z?]{4,}.*")) {  //%if the word has .;%" after it
				word=word.substring(0,word.length()-1);
				putInHashMap(word);
			}
		}
	}


	private void processWordsByRow(List<String> lines){

		for(String line:words){
			countWordOccurence(line);
		}
	}

	public void processTextByRow(){
		rows = text.split("\n");
		//printRows(rows);
		String[] fontData = {};
		try{
			fontData=extractData(rows[0]);
		}catch(Exception ex){
			System.out.println("Az adott PDF nem elemezheto! (A programnak nem lathato.)");
			System.exit(1);
		}

		if(fontData[1].equals("0.0")){				// if the first row doesn't have a font-size, which means it's 0.0
			System.out.println("Az adott PDF nem elemezheto! (A programnak nem lathato.)");
			System.exit(1);
		}

		processWordsByRow(words);
		System.out.println(wordsOccurence);

		getFontSizeWithNumberOfRows();
		mostUsedFontSizeInPDF = getTheMostUsedFont();
		mostUsedSubTitleFontSize = getTheMostUsedSubtitleFontSize();
		averageNumberOfRowsInParagraph = getTheAverageRowFromParagraphs();

		avgWordsInRow=numberOfWords();
		bibliography=bibliographyExistence();
	}

	public float numberOfWords(){
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

	private int getImagesFromResources(PDResources resources){

		int num=0;
		for (COSName xObjectName : resources.getXObjectNames()) {
			PDXObject xObject;
			try {
				xObject = resources.getXObject(xObjectName);
				if (xObject instanceof PDFormXObject || xObject instanceof PDImageXObject ) {
					num++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return num;
	}

	public int getImageNumberFromPDF(PDDocument doc) {

		int overallNum=0;

		for (PDPage page : doc.getPages()) {
			overallNum+=getImagesFromResources(page.getResources());	
		}

		return overallNum;
	}

	public void processText(File file){

		try {
			File inputFile = new File(file.getAbsolutePath());
			pd = PDDocument.load(inputFile);

			numOfImages=getImageNumberFromPDF(pd);

			pageNumber=pd.getNumberOfPages();
			PDFTextStripper stripper = new PDFTextStripper() {
				String prevBaseFont;

				protected void writeString(String text, List<TextPosition> textPositions) throws IOException
				{
					StringBuilder builder = new StringBuilder();
					StringBuilder wordBuilder = new StringBuilder();

					for (TextPosition position : textPositions)
					{
						String baseFont = position.getFont().getName();
						if (baseFont != null && !baseFont.equals(prevBaseFont))
						{
							float size=position.getFontSizeInPt();

							builder.append('[').append(baseFont).append(',').append(size+"").append(']');
							prevBaseFont = baseFont;
						}
						wordBuilder.append(position);
						builder.append(position);
					}
					words.add(wordBuilder.toString());
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