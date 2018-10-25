import java.io.*;
import java.util.*;
import java.util.regex.*;

public class CongressionalSpeechExtractor {
	public static void processFiles() {
		final String INPUT_DIR = "input/";
		final String OUTPUT_DIR = "output/";
		final int NUM_FILENAME_PREFIX_CHARS = 5;	//"CREC-"
		final int FILENAME_EXTENSION_CHAR_COUNT = 4;	//".txt"
		
		Matcher regexMatch;
		Pattern speakerNamePattern = Pattern.compile("^((?:(?:Mrs\\.)|(?:Ms\\.)|(?:Mr\\.)) *[A-Z-]*(?: *[A-Z-]*)*)(?:\\.)");


		ArrayList<File> inputFiles = new ArrayList<File>(Arrays.asList(new File(INPUT_DIR).listFiles()));
		BufferedReader reader = null;
		PrintWriter writer = null;
		for(int i=0; i<inputFiles.size(); i++){
			try {
				reader = new BufferedReader( new InputStreamReader( new FileInputStream(inputFiles.get(i)), "UTF8"));
				String rawFileName = inputFiles.get(i).getName(); 
				String date = rawFileName .substring(NUM_FILENAME_PREFIX_CHARS, rawFileName.length() -FILENAME_EXTENSION_CHAR_COUNT);
				
				File folder = new File(OUTPUT_DIR + date);
				if(!folder.exists()){
					folder.mkdir();
				}
				
				
				StringBuilder allSpeechesForDay = new StringBuilder();

				int speechCount = 0;
				String speakerName = "";
				StringBuilder speech = new StringBuilder();
				boolean speechStarted=false;
				boolean specialTermination = false;
				
				String previousLine = reader.readLine();
				String line = reader.readLine();	//assume 1st 2 lines are skippable & 2 lines exist
				int j=2;		//line numbers start from 1 & previousLine uses up line 1
				for(; line != null && j<1000000000; previousLine=line, line=reader.readLine(), j++){
					if(line.isEmpty()){		//skip empty lines
						continue;
					}
					
					String[] words = line.split(" ");	//split line on spaces
					
					regexMatch = speakerNamePattern.matcher(line);
					boolean titleDetected=false;
					if(regexMatch.find()){
						speakerName = regexMatch.group(1);
						titleDetected = true;
					}
					
					boolean clerkDetected = line.matches("(?i:^The *CLERK\\..*)");	//"The CLERK" as start of line & anything afterwards
					
					//line.matches("(?i:)")
					if(line.matches("f") || line.matches("(?i:The Clerk read.*)") || line.matches("(?i:^\\[Roll .*)") || line.matches("(?i:SWEARING IN OF MEMBERS.*)") || line.matches("(?i:MAJORITY LEADER.*)") || line.matches("(?i:MINORITY LEADER.*)") || line.matches("(?i:MAJORITY WHIP.*)") || line.matches("(?i:AMENDMENT OFFERED BY.*)")){
						specialTermination = true;
					}
					if(speechStarted && (clerkDetected || titleDetected || specialTermination)){	//speech ended, write to file
						allSpeechesForDay.append(speech.toString());
						speech.setLength(0);
						speechStarted = false;
					}
					specialTermination= false;

					if(titleDetected){	//end current speech & start new
						speechStarted = true;
						speechCount++;
						speech.append("\n\tSpeech #"+speechCount + " " + speakerName +"\n");
						speech.append(line+"\n");
					}
					else if(speechStarted){
						if(line.split(" ")[0].matches("(?i:VerDate)")){
							//skip 3 column page breaks with dates
							for(; !line.split(" ")[0].matches("(?i:Jkt)"); previousLine=line, line=reader.readLine(), j++){
							}
						}
						else if(line.matches("^([A-Z]:).*")){	//Starts with E: or other capital		E:\RECORD15\H06JA5.REC
							//Skip 5 lines
							for(int k=0; k<5; previousLine=line, line=reader.readLine(), j++, k++){
							}
						}
						else if(line.matches("(?i:^PO 0.*)")){
							for(; !line.matches("(?i:^Sfmt.*)"); previousLine=line, line=reader.readLine(), j++){
							}
						}
						else {	//normal line
							speech.append(line+"\n");
						}
					}

				}
				
				System.out.println(allSpeechesForDay.toString());
				try {
					writer = new PrintWriter(folder + "/" + "asdf.txt", "UTF-8");
					writer.println(allSpeechesForDay.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
				finally{
					writer.close();
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
	private static ArrayList<String> states = new ArrayList<String>(Arrays.asList(new String[]{"alabama", "alaska", "arizona", "arkansas", "california", "colorado", "connecticut", "delaware", "florida", "georgia", "hawaii", "idaho", "illinois", "indiana", "iowa", "kansas", "kentucky", "louisiana", "maine", "maryland", "massachusetts", "michigan", "minnesota", "mississippi", "missouri", "montana", "nebraska", "nevada", "new hampshire", "new jersey", "new mexico", "new york", "north carolina", "north dakota", "ohio", "oklahoma", "oregon", "pennsylvania", "rhode island", "south carolina", "south dakota", "tennessee", "texas", "utah", "vermont", "virginia", "washington", "west virginia", "wisconsin", "wyoming", 
		"district of columbia", "puerto rico", "guam", "american samoa", "u.s. virgin islands", "northern mariana islands"}));
	private static boolean isState(String state) {
		state = state.toLowerCase();
		return states.contains(state);
	}

	public static void main(String[] args) {
		CongressionalSpeechExtractor.processFiles();
	}

}