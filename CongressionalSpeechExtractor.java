import java.io.*;
import java.util.*;

public class CongressionalSpeechExtractor {
	public static void processFiles(){
		String INPUT_DIR = "input/";
		String OUTPUT_DIR = "output/";
		
		ArrayList<File> inputFiles = new ArrayList<File>(Arrays.asList(new File(INPUT_DIR).listFiles()));
		for(int i=0; i<inputFiles.size(); i++){
			try {
				Scanner fileReader = new Scanner(inputFiles.get(i));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println(inputFiles.get(i).getName());
		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(OUTPUT_DIR + "asdf.txt", "UTF-8");
			writer.println("The first line");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		finally{
			writer.close();
		}
	}
	
	public static void main(String[] args) {
		CongressionalSpeechExtractor.processFiles();
	}

}