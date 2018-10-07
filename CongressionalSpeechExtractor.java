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
				
				String previousLine = reader.readLine();
				String line = reader.readLine();	//assume 1st 2 lines are skippable & 2 lines exist
				int j=2;		//line numbers start from 1 & previousLine uses up line 1
				for(; line != null && j<958; previousLine=line, line=reader.readLine(), j++){
					boolean isPreviousLineEndOfSentence  = false;
					if(previousLine.length()>0){
						isPreviousLineEndOfSentence = previousLine.substring(previousLine.length()-1).matches("[\\.\\?!]");	//end of sentence = . ? ! 
					}
					String[] words = line.split(" ");	//split line on spaces
					boolean titleDetected = line.matches("(?i:^((Mr\\.)|(Ms\\.)).*)") && isUpperCase(words[1]) && isPreviousLineEndOfSentence;		//line starting with Mr. or Ms. with any text afterwards    (?i:X) is for case insensitive 
					boolean clerkDetected = line.matches("(?i:^(The *CLERK).*)");
					if(speechStarted && clerkDetected){	//speech ended, write to file
						//speech.append(j+"=clerk\n\n");
						speech.setLength(0);
						speechStarted = false;
					}
					if(titleDetected){
						if(speechStarted){	//end current speech & start new???
							speech.append(line+"\n");
						}
						else{
							speechStarted = true;
							speakerName = extractSpeakerNameAndOptionalState(words);
							speech.append(line+"\n");
						}
					}
					else if(speechStarted && !line.isEmpty()){
						speech.append(line+"\n");
					}
					else if(speechStarted && line.isEmpty()){	//skip 3 column page breaks with dates
						previousLine = line;	//update before skipping
						line = reader.readLine();
						j++;
						if(line.split(" ")[0].matches("(?i:VerDate)")){
							// speech.append("\tline="+j + " "+line+"\n");
							for(; !line.split(" ")[0].matches("(?i:Jkt)"); previousLine=line, line=reader.readLine(), j++){
								//Advance over page break date info
								// speech.append("prev="+previousLine+"\tline="+line+"\n");
							}
							//Advance 2 more lines
							previousLine=line;
							line=reader.readLine();
							j++;
							previousLine=line;
							line=reader.readLine();
							j++;
							// speech.append("prev="+previousLine+"\n");
							// speech.append(j+" line="+line+"\n");
						}

					}

					// System.out.println(line);
				}
				System.out.println(speech.toString());

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
	
	private static boolean isUpperCase(String string) {
		return string.equals(string.toUpperCase());
	}

	//TODO = deal with periods after names?
	private static String extractSpeakerNameAndOptionalState(String[] words) {
		String speakerName = words[0] + " " + words[1];
		if(words[2].matches("(?i:of)")){
			if(isState(words[3])){
				speakerName += " of " + words[3];
			}
			else if(isState(words[3] + " " + words[4])){
				speakerName += " of " + words[3] + " " + words[4];
			}
			else{
				speakerName += " NO STATE FOUND";
			}
			//more cases if states names are > 2 words
		}
		// System.out.println("Speaker="+speakerName);
		return speakerName;
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