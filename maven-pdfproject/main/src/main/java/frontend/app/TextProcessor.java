package frontend.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

import backend.repository.DAOFactory;
import backend.repository.PDFDAO;
import common.PDFContainer;
import common.Scientific;
import common.StopWords;
import common.Tools;
import backend.model.PDF;

public class TextProcessor {

	private static int pageNumber;
	private static float avgWordsInRow;
	private static PDDocument pd = null;
	private static StringBuilder sb = null;
	private static String text;
	private static boolean bibliography;
	private static String titleFontSize;
	private static String titleFontFamily;

	private static float mostUsedSubTitleFontSize;
	private static float mostUsedFontSizeInPDF;
	private static float[][] pdfFontsWithRows;
	private static String[][] subtitleFontSizeAndRow;
	private static int averageNumberOfRowsInParagraph;
	private static int numOfImages;

	private static String subTitles[]; 
	private String path;
	private PDF pdfObj;
	private PDFDAO pdfDAO;

	private static List<FontAndRow> pdfData = new ArrayList<FontAndRow>();
	private static String[] rows;

	private int wordsInserted = 0;

	public void setPDF(PDF pdf) {
		this.pdfObj = pdf;
	}

	public PDF getPDF() {
		return pdfObj;
	}
	
	public TextProcessor(File file)
	{
		int state = processText(file);
	}
	
	public  PDDocument getPdDocument(){
		return pd;
	}
	/**
	 * Constructor
	 * @param file: imput file.
	 * @param scientific: types.
	 */
	public TextProcessor(File file, Scientific scientific) {
		
		PDFContainer.wordsOccurence.clear();
		PDFContainer.words.clear();

		int state;
		pdfDAO = DAOFactory.getInstance().getPDFDAO();

		state = processText(file);
		/**
		 *  If the pdf file contains a lot of pages (more than 40).
		 */
		if (state == -1) {
			System.out.println("Az adott PDF oldalszama sok, meglehet nem cikk!");
			return;
		}
		/**
		 *  If an the PDFBox doesn't recognise the given PDF file.
		 */
		if (state == -2) {
			System.out.println("Az adott PDF nem elemezheto! (A programnak nem lathato.)");
			return;
		}
		
		subTitles = new String[subtitleFontSizeAndRow.length];

		for (int i = 0; i<subTitles.length;++i) {
			subTitles[i] = subtitleFontSizeAndRow[i][2];
		}

		try {

			path = file.getAbsolutePath();
			long fileSize=file.length()/1024;
			PDF pdf = new PDF(path, PDFContainer.wordsOccurence, pageNumber, avgWordsInRow, mostUsedFontSizeInPDF ,numOfImages,averageNumberOfRowsInParagraph,bibliography, fileSize, scientific);
			setPDF(pdf);
			if(scientific != Scientific.UNKNOWN){
				pdfDAO.insertPDF("LearningData", pdf);
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			//
		}
	}

	/**
	 * Printout data
	 */
	public static void printStatistics() {
		System.out.println("Page number:" + pageNumber);
		System.out.println("Average words/row:" + avgWordsInRow);
		System.out.println("Bibliography available:" + bibliography);
		System.out.println("Title font-family:"+titleFontFamily);
		System.out.println("Title font-size:"+titleFontSize);
		System.out.println("Most used font-size:"+mostUsedFontSizeInPDF);
		System.out.println("Most used subtitle font-size:"+mostUsedSubTitleFontSize);
		System.out.println("Average number of rows:"+averageNumberOfRowsInParagraph);
	}
	
	/**
	 * Decide if there is bibliography. 
	 * @return true if there is.
	 */
	public static boolean bibliographyExistence() {

		for (int i = 0; i< rows.length; i++) {
			if(rows[i].contains("References") || rows[i].contains("Bibliography") || 
					rows[i].contains("References and notes") || rows[i].contains("Reference and note")){
				/**
				 * An extra character is added at the end of the row, we need the text without it
				 */
				String row=rows[i].substring(0,rows[i].length()-1); 
				if(row.matches("\\[.*\\]References") || 
					row.matches("\\[.*\\]Bibliography") ||
					row.matches("\\[.*\\]References and notes") ||
					row.matches("\\[.*\\]Reference and note") ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Extracts data between [] and splits it by
	 * @param row row to be processed
	 * Output param: string[] with 1st data: result[0]-> font family, result[1] -> font size
	 */
	public static String[] extractData(String row) {
		//String result = row.substring(row.indexOf("[")+1,row.lastIndexOf("]"));
		String result = row.substring(row.lastIndexOf("[")+1,row.lastIndexOf("]"));
		return result.split(",");
	}

	/**
	 * The data is number+fontFamily, we only need the font-family
	 * @param data data to process
	 */
	public static String getFontFamily(String data) {
		String[] fontFamily = data.split("\\+");
		return fontFamily[1];
	}


	public static void printRows() {
		for(int i=0; i < rows.length; i++)
			System.out.println(rows[i]);
	}

	/**
	 * Aggregating data in rows by font size and counting the occurrence.
	 * Output @param: containing the font data, rows data and number of rows data.
	 */
	public static void getFontSizeWithNumberOfRows() {
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

		// Containing one or more font entries.
		patternForRegex = Pattern.compile("\\[[a-zA-Z0-9+-]+,[0-9]+\\.+[0-9]+\\]");

		// Counting the number of rows till another font data entry.
		for(int i = 0; i < length; i++) {
			iterator = i;
			counter = 0;
			match = patternForRegex.matcher(rows[i].toString());
			System.out.println(rows[i]);

			if (match.find()) {
				// Checking if the followed row is not starting or containing another font entry => so that row belongs to the previous font entry.
				if((i + 1 < length) && (patternForRegex.matcher(rows[i + 1].toString()).find() == false)){

					iterHelp = i + 1;
					while((iterHelp<length) && (patternForRegex.matcher(rows[iterHelp].toString()).find() == false)){

						rowCounter++;
						counter++;
						if(counter <= 3) {
							/**
							 *  To store the first 3 row
							 */
							// Cleaning the row from font entries.
							cleanedRow = rows[i].replaceAll(patternForRegex.toString(), replacement);
							forSubtitles += cleanedRow + " ";
						}

						i++;
						iterHelp = i;
					}

					match = patternForRegex.matcher(rows[iterator].toString());
					if (match.find()) {
						String data = match.group(match.groupCount());
						String[] fontData = extractData(data);
						rowFontSize = fontData[1];

						FontAndRow fontAndRow = new FontAndRow();
						fontAndRow.setFontSize(rowFontSize);
						fontAndRow.setSomeRows(forSubtitles);
						fontAndRow.setNumberOfRows(rowCounter);
						pdfData.add(fontAndRow);
					}
					
					// For reusing them.
					rowCounter = 0;
					// To get the row before.
					if (counter != 0) {			
						i--;
					}
					
					forSubtitles = "";
				}
				// If the row which contains font entry isn't followed by a row without font entry.
				else {
					match = patternForRegex.matcher(rows[iterator].toString());
					if (match.find()) {
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
	
	/**
	 * Searching for the most used font.
	 * @return: the most occurred font size.
	 */
	public static float getTheMostUsedFont() {

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

		// We need only two columns for size and number of rows.
		temp = new float[pdfData.size()][2];

		for (FontAndRow fdata: pdfData) {

			// If the 2D array contains the new font size.
			for (int i = 0; i < length; i++) {
				if(temp[i][0] == Float.parseFloat(fdata.getFontSize())) {
					temp[i][1] += fdata.getNumberOfRows();
					exists = true;
					break;
				}
			}
			// If doesn't contains the font size.
			if (!exists) {
				for (int i = 0; i < length; i++) {
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
		// Saving the data to a global 2D array.
		for (int i = 0; i < actualLength; i++) {
			pdfFontsWithRows[i][0] = temp[i][0];
			pdfFontsWithRows[i][1] = temp[i][1];
			
			// And searching for the most used font-size.
			if ( (i + 1 < actualLength) && (sum < temp[i + 1][1]) ) {
				sum = temp[i+1][1];
				mostUsedF = temp[i+1][0];
			}
		}

		return mostUsedF;
	}
	
	/**
	 * Searching for the subtitle's most used font size.
	 * @return: the most occurred subtitle font size.
	 */
	public static float getTheMostUsedSubtitleFontSize() {
		float sum, mostUsedFontForSubtitles;
		int numberOfRows, numberOfChars, length, index, actualLength;
		boolean exists;
		String[][] temp;
		float[][] subTitleFontSizeData;

		length = pdfData.size();
		// [][0] font-size, [][1] number of rows, [][2] rows.
		temp = new String[length][3];

		exists = false;
		index = 0;
		actualLength = 0;
		mostUsedFontForSubtitles = 0;
		numberOfRows = 1;
		numberOfChars = 5;

		for (FontAndRow fData: pdfData){
			// The subtitle's font-size must be bigger than the most used font-size.
			if (mostUsedFontSizeInPDF < Float.parseFloat(fData.getFontSize())) {
				// The subtitle's row's number can be maximum 1.
				if (numberOfRows >= fData.getNumberOfRows()) {
					// The subtitle must be at least 5 character long.
					if (numberOfChars <= fData.getSomeRows().length()) {
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
		for(int i = 0; i < index; i++) {

			subtitleFontSizeAndRow[i][0] = temp[i][0];
			subtitleFontSizeAndRow[i][1] = temp[i][1];
			subtitleFontSizeAndRow[i][2] = temp[i][2];

			// If the 2D array contains the new font size.
			for (int k = 0; k < index; k++) {
				if (subTitleFontSizeData[k][0] == Float.parseFloat(temp[i][0])) {
					subTitleFontSizeData[k][1] += Float.parseFloat(temp[i][1]);
					exists = true;
					break;
				}
			}
			// If doesn't contains the font size.
			if ( !exists ) {
				for (int j = 0; j < length; j++) {
					if (subTitleFontSizeData[j][0] == 0.0) {
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
		for (int i = 0; i < actualLength; i++) {
			if ( (i + 1 < actualLength) && (sum < subTitleFontSizeData[i + 1][1]) ) {
				sum = subTitleFontSizeData[i + 1][1];
				mostUsedFontForSubtitles = Float.parseFloat(temp[i + 1][0]);
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

	/**
	 * Calculating the the average row in paragraphs.
	 * @return: the average row by paragraphs.
	 */
	public static int getTheAverageRowFromParagraphs() {

		int avgRowByParagraph;
		int numberOfRowsInPDF = rows.length;
		// One of the subTitle is the title so we don't need to count that in.
		int numberOfSubtitles = subtitleFontSizeAndRow.length  -1;
		int sum = 0;

		sum = numberOfRowsInPDF - subtitleFontSizeAndRow.length;
		avgRowByParagraph = sum / numberOfSubtitles;

		return avgRowByParagraph;
	}
	
	/**
	 * Inserting the words in HashMap<String, Integer> wordsOccurence.
	 * @param word: word from the data.
	 */
	private void putInHashMap(String word) {
		if(!StopWords.isStopWord(word)){
			Integer freq = PDFContainer.wordsOccurence.get(word);		
			
			if (freq == null) {
				if(PDFContainer.numberOfWordsToInsert != 0){
					if(wordsInserted < PDFContainer.numberOfWordsToInsert){
						PDFContainer.wordsOccurence.put(word,1);
						wordsInserted++;
					}
				}else{
					PDFContainer.wordsOccurence.put(word,1);
					wordsInserted++;
				}
				
			} else {
				PDFContainer.wordsOccurence.put(word,freq+1);
			}
		}	
	}
	/**
	 * Insert subtitle in HashMap<String, Integer> wordsOccurence.
	 * @param word: word from the data.
	 */
	private void putSubtitleInHashMap(String word) {
		if(!StopWords.isStopWord(word)){
			Integer freq = PDFContainer.wordsOccurence.get(word);

			if (freq == null) {
				wordsInserted++;
				PDFContainer.wordsOccurence.put(word,0);
			}
		}	
	}
	
	/**
	 * Count word occurrence.
	 * @param line: a row with words from the extracted data from pdf file.
	 */
	public void countWordOccurence(String line) {
		String[] words=line.split(" ");
		String cleanedWord = "";

		// Get subtitles.
		subTitles = new String[subtitleFontSizeAndRow.length];

		for (int i = 0; i<subTitles.length;++i) {
			subTitles[i] = subtitleFontSizeAndRow[i][2];
		}
		
		// Firstly we will put the subtitles to the hashmap at the beginning of it.
		for (String subTitle : subTitles){
			
			if(PDFContainer.numberOfWordsToInsert > 0){
				if(wordsInserted >= PDFContainer.numberOfWordsToInsert){
					break;
				}
			}
			
			String[] subTitleWords=subTitle.split(" ");
			for (String subTitleWord : subTitleWords) {
				subTitleWord = Tools.simplifyWord(subTitleWord);
				if ( subTitleWord.length() > 0 ) {
					cleanedWord = subTitleWord.toLowerCase();
					// If it is a word or a world with ? in it, min 4 character words.
					if (cleanedWord.matches("[a-zA-z?-]{4,}")) {
						putSubtitleInHashMap(cleanedWord);
					// If the word has .;*" after it.	
					} else if (cleanedWord.matches("^[a-zA-z?-]{4,}.*")) {
						cleanedWord = cleanedWord.replaceAll("([\\.\\,\\;])", "");
						putSubtitleInHashMap(cleanedWord);
					}
				}
			}
		}
		
		// Now we add the rest of the words to the hashmap.
		for (String word : words) {
			word = Tools.simplifyWord(word);

			if ( word.length() > 0 ) {
				cleanedWord = word.toLowerCase();
				// If it is a word or a world with ? in it, min 4 character words.
				if (cleanedWord.matches("[a-zA-z?-]{4,}")) {
					putInHashMap(cleanedWord);
				// If the word has .;*" after it.
				} else if (cleanedWord.matches("^[a-zA-z?-]{4,}.*")) {
					cleanedWord = cleanedWord.replaceAll("([\\.\\,\\;])", "");
					putInHashMap(cleanedWord);
				}
			}
		}
	}

	/**
	 * Separating the lines for countWordOccurence.
	 * @param lines: from extracted pdf data.
	 */
	private void processWordsByRow(List<String> lines){

		for (String line:PDFContainer.words) {
			countWordOccurence(line);
		}
	}
	
	/**
	 * Process the text by row.
	 * @return: true if no problem occur else false.
	 */
	public boolean processTextByRow() {
		rows = text.split("\n");
		//printRows(rows);
		String[] fontData = {};
		try {
			fontData=extractData(rows[0]);
		} catch(Exception ex) {
			return false;
		}

		// If the first row doesn't have a font-size, which means it's 0.0 .
		if (fontData[1].equals("0.0")) {
			return false;
		}

		//processWordsByRow(PDFContainer.words);
		//System.out.println(PDFContainer.wordsOccurence);

		getFontSizeWithNumberOfRows();
		mostUsedFontSizeInPDF = getTheMostUsedFont();
		mostUsedSubTitleFontSize = getTheMostUsedSubtitleFontSize();
		averageNumberOfRowsInParagraph = getTheAverageRowFromParagraphs();

		avgWordsInRow=numberOfWords();
		bibliography=bibliographyExistence();

		processWordsByRow(PDFContainer.words);
		
		return true;
	}

	/**
	 * Calculate number of rows.
	 * @return: number of rows occurred in pdf.
	 */
	public static float numberOfWords() {
		char c;
		int count = 1;
		float sum = (float) 0.0;

		for (int i = 0; i < rows.length; i++) {
			// Number of words=number of spaces+1.
			count=1;
			for (int j = 0; j < rows[i].length(); j++) {
				c = rows[i].charAt(j);
				if (c == ' ') {
					count++;
				}
			}

			sum += count;
		}
		// Every page has a number at the end, that don't counts as a row.
		sum = sum - pageNumber;
		int rowNumber = rows.length - pageNumber;

		return sum / rowNumber;
	}

	/**
	 * Counting the occurrence of images by page.
	 * @param resources: a page.
	 * @return: number of images from a page.
	 */
	private static int getImagesFromResources(PDResources resources) {

		int num=0;
		for (COSName xObjectName : resources.getXObjectNames()) {
			PDXObject xObject;
			try {
				xObject = resources.getXObject(xObjectName);
				if (xObject instanceof PDFormXObject || 
						xObject instanceof PDImageXObject ) {
					num++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return num;
	}

	/**
	 * Count number of images from param doc.
	 * @param doc: data from given pdf file.
	 * @return: number of images in given pdf.
	 */
	public static int getImageNumberFromPDF(PDDocument doc) {

		int overallNum=0;

		for (PDPage page : doc.getPages()) {
			overallNum+=getImagesFromResources(page.getResources());	
		}

		return overallNum;
	}
	
	/**
	 * Process the extracted text from pdf.
	 * @param file: selected pdf file.
	 * @return: 1 if there are no problems.
	 */
	public int processText(File file){

		try {
			File inputFile = new File(file.getAbsolutePath());
			pd = PDDocument.load(inputFile);

			numOfImages=getImageNumberFromPDF(pd);
			pageNumber=pd.getNumberOfPages();
			
			PDFTextStripper stripper = new PDFTextStripper() {
				String prevBaseFont;

				protected void writeString(String text, 
						List<TextPosition> textPositions) throws IOException {
					StringBuilder builder = new StringBuilder();
					StringBuilder wordBuilder = new StringBuilder();

					for (TextPosition position : textPositions) {
						String baseFont = position.getFont().getName();
						if (baseFont != null && !baseFont.equals(prevBaseFont)) {
							float size = position.getFontSizeInPt();

							builder.append('[').append(baseFont).append(',').append(size+"").append(']');
							prevBaseFont = baseFont;
						}

						wordBuilder.append(position);
						builder.append(position);
					}

					PDFContainer.words.add(wordBuilder.toString());
					writeString(builder.toString());
				}
			};

			sb = new StringBuilder();
			/**
			 * Add text to the StringBuilder from the PDF
			 */
			stripper.setStartPage(1); // Start extracting from page 3
			stripper.setEndPage(pageNumber); // Extract till page 4
			sb.append(stripper.getText(pd));
			text = sb.toString();

			if (pd != null) {
				pd.close();
			}

			/**
			 *  If the given PFD is has more than 40 pages (if it's not an article).
			 */
			if( pageNumber > 40) {
				return -1;
			}

			/**
			 *  If the PDFBox doens't recognise the given PDF.
			 */
			if ( !processTextByRow() ) {
				return -2;
			}


		} catch (Exception e) {
			e.printStackTrace();
		}
		return 1;
	}	
}