import java.io.*;
import java.util.*;

public class CongressionalSpeechExtractor {
	public static void processFiles() {
		final String INPUT_DIR = "input/";
		final String OUTPUT_DIR = "output/";
		final int NUM_FILENAME_PREFIX_CHARS = 5;	//"CREC-"
		final int FILENAME_EXTENSION_CHAR_COUNT = 4;	//".txt"
		
		ArrayList<File> inputFiles = new ArrayList<File>(Arrays.asList(new File(INPUT_DIR).listFiles()));
		BufferedReader reader = null;
		for(int i=0; i<inputFiles.size(); i++){
			try {
				reader = new BufferedReader(new FileReader(inputFiles.get(i)));
				String rawFileName = inputFiles.get(i).getName(); 
				String date = rawFileName .substring(NUM_FILENAME_PREFIX_CHARS, rawFileName.length() -FILENAME_EXTENSION_CHAR_COUNT);
//				System.out.println(date);
				
				File folder = new File(OUTPUT_DIR + date);
				if(!folder.exists()){
					folder.mkdir();
				}
				
				
				String speakerName = "";
				StringBuilder speech = new StringBuilder();
				boolean speechStarted=false;
				
				String prevousLine = reader.readLine();
				String line = "";	//assume 1st 2 lines are skippable & 2 lines exist
				int j=0;
				for(; (line = reader.readLine()) != null && j<4; j++){
					boolean isPreviousLineEndOfSentence  = false;
					if(prevousLine.length()>0){
						isPreviousLineEndOfSentence = prevousLine.substring(prevousLine.length()-1).matches("[\\.\\?!]");	//end of sentence = . ? ! 
					}
					// System.out.println(isPreviousLineEndOfSentence);
					// System.out.println(line);
					boolean titleDetected = line.matches("(?i:^((Mr\\.)|(Ms\\.)).*)");		//line starting with Mr. or Ms. with any text afterwards    (?i:X) is for case insensitive 
//					System.out.println(titleDetected);
					if(titleDetected){
						String[] words = line.split(" ");
						speakerName = words[0] + " " + words[1];
						if(words[2].matches("(?i:of)")){
							if(isState(words[3])){
								speakerName += " of " + words[3];
							}
							else if(isState(words[3] + " " + words[4])){
								speakerName += " of " + words[3] + " " + words[4];
							}
							//more cases if states names are > 2 words
						}
						System.out.println(speakerName);
					}
					// System.out.println("prev="+prevousLine+"\tline="+line);
					prevousLine = line;
					// System.out.println(line);
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			finally{
				try {
					reader.close();
				} catch (IOException e) {
					System.out.println("Failed to close reader");
					e.printStackTrace();
				}
			}
//			System.out.println(inputFiles.get(i).getName());
		}

		// PrintWriter writer = null;
		// try {
		// 	writer = new PrintWriter(OUTPUT_DIR + "asdf.txt", "UTF-8");
		// 	writer.println("The first line");
		// } catch (FileNotFoundException e) {
		// 	e.printStackTrace();
		// } catch (UnsupportedEncodingException e) {
		// 	e.printStackTrace();
		// }
		// finally{
		// 	writer.close();
		// }
	}
	
	//states with more than 2 words?
	private static ArrayList<String> states = new ArrayList<String>(Arrays.asList(new String[]{"alabama", "alaska", "arizona", "arkansas", "california", "colorado", "connecticut", "delaware", "florida", "georgia", "hawaii", "idaho", "illinois", "indiana", "iowa", "kansas", "kentucky", "louisiana", "maine", "maryland", "massachusetts", "michigan", "minnesota", "mississippi", "missouri", "montana", "nebraska", "nevada", "new hampshire", "new jersey", "new mexico", "new york", "north carolina", "north dakota", "ohio", "oklahoma", "oregon", "pennsylvania", "rhode island", "south carolina", "south dakota", "tennessee", "texas", "utah", "vermont", "virginia", "washington", "west virginia", "wisconsin", "wyoming", "district of columbia", "puerto rico", "guam", "american samoa", "u.s. virgin islands", "northern mariana islands"}));
	private static boolean isState(String state) {
		state = state.toLowerCase();
		return states.contains(state);
	}

	public static void main(String[] args) {
		CongressionalSpeechExtractor.processFiles();
	}

}