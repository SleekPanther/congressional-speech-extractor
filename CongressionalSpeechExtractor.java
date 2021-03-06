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
		final Pattern speakerNamePattern = Pattern.compile("^((?:(?:Mrs\\.)|(?:Ms\\.)|(?:Mr\\.)) +[A-Z-]*(?: [A-Z]\\.)*(?: +[A-Z-]*)*)(?:(?:\\. )|( *[oO][fF]))");
		/*													1 |       honorific                |     2  | opt  initial|        3     |5  |period| |space "of"|
		1=capture group for name & start of line
		2=multiple last name
		3=optional multiple last names (hyphen of 2nd last name also included though the trailing LAST-NAME- shouldn't exist)
		4=capture group for of (period group is NON-capturing so 2 groups means there is a state coming) */


		ArrayList<File> inputFiles = new ArrayList<File>(Arrays.asList(new File(INPUT_DIR).listFiles()));
		BufferedReader reader = null;
		PrintWriter writer = null;
		for(int i=0; i<inputFiles.size(); i++){
			try {
				reader = new BufferedReader( new InputStreamReader( new FileInputStream(inputFiles.get(i)), "UTF8"));
				String rawFileName = inputFiles.get(i).getName(); 
				String date = rawFileName .substring(NUM_FILENAME_PREFIX_CHARS, rawFileName.length() -FILENAME_EXTENSION_CHAR_COUNT);
				
				File folder = new File(OUTPUT_DIR + date);		//create folder name but don't make it unless there are speeches
				// StringBuilder allSpeechesForDay = new StringBuilder();

				int speechCount = 0;
				String speakerName = "";
				String outputFileName = "";
				StringBuilder speech = new StringBuilder();
				boolean speechStarted=false;
				boolean specialTermination = false;
				
				String line = reader.readLine();	//assume 1st 2 lines are skippable & 2 lines exist
				int j=1;		//line numbers start from 1
				for(; line != null; line=reader.readLine(), j++){
					if(line.matches("Daily Digest") && speechCount>0){
						CongressionalSpeechExtractor.writeToFile(writer, folder + "/" + outputFileName, speech.toString());
						// allSpeechesForDay.append(speech.toString());	//like printing
						speech.setLength(0);
						break;
					}

					//Weird "∑ " bullets weren't spoken but still count as speeches https://www.congress.gov/crec/2016/12/30/CREC-2016-12-30.pdf
					if(line.matches("^∑ .*")){
						line = line.substring(2, line.length());
					}
					
					//	<0x0c> \f	unreadable form feed character cleanup
					if(line.matches("^\f.*")){
						line = line.substring(1, line.length());
					}
					
					if(	   line.isEmpty()
						|| (isUpperCase(line) && !line.matches("(?i:^PO 0\\d*.*)") && !line.matches("NOMINATIONS.*"))
						|| line.matches("(?i:^(?:January|February|March|April|May|June|July|August|September|October|November|December) \\d.*)")		//any date not caught with VerDate
						|| line.matches("(?i:^(?:Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday), .*)")		//any date not caught with VerDate
						|| line.matches("(?i:.*CONGRESSIONAL RECORD.*)")		//CONGRESSIONAL RECORD		anywhere in a line
						|| line.matches("(?i:.*CONG-REC-ONLINE.*)")
						|| line.matches(".*DSK.{7}PROD.*")		//SSpencer on DSK4SPTVN1PROD with HOUSE
						){
						continue;
					}
					
					regexMatch = speakerNamePattern.matcher(line);
					boolean titleDetected=false;
					if(regexMatch.find()){
						if(regexMatch.group(2) != null){	//2nd capture group is optional "of"
							//concat next line in case long name and state
							line += " " + reader.readLine();
							j++;
							//regexMatch.group(1) is usually speakerName
							String restOfLine = line.substring(regexMatch.group(1).length()+3);		//" of" = 3 characters
							int periodIndex = restOfLine.indexOf(".");
							int commaIndex = restOfLine.indexOf(",");
							int colonIndex = restOfLine.indexOf(":");		//Mrs. MILLER of Michigan: Committee on
							if(colonIndex==-1 && periodIndex < commaIndex){
								speakerName = regexMatch.group(1) + " of " + extractState(restOfLine);
								titleDetected = true;
							}
						}
						else{
							speakerName = regexMatch.group(1);
							titleDetected = true;
						}
					}
					
					if(	!titleDetected &&
					   	(	line.matches("f")	//strange horizontal diamond in PDF
						|| line.matches("(?i:The Clerk read.*)")
						|| line.matches("(?i:^The *CLERK\\..*)")
						|| line.matches("(?i:The SPEAKER\\..*)")
						|| line.matches("(?i:The SPEAKER pro tempore\\..*)")
						|| line.matches("(?i:^\\[Roll.*)")
						|| line.matches("(?i:^\\[Quorum.*)")
						|| line.matches("(?i:^ADJOURNMENT.*)")
						|| line.matches("(?i:^The PRESIDING OFFICER\\..*)")
						|| line.matches("(?i:^The PRESIDING OFFICER \\(.*)")
						|| line.matches("(?i:^The CHAIR\\..*)")
						|| line.matches("(?i:^The Acting CHAIR\\..*)")
						|| line.matches("(?i:^The Acting CHAIR \\(.*)")
						|| line.matches("(?i:SWEARING IN OF MEMBERS.*)")
						|| line.matches("(?i:MAJORITY LEADER.*)")
						|| line.matches("(?i:MINORITY LEADER.*)")
						|| line.matches("(?i:MAJORITY WHIP.*)")
						|| line.matches("NOMINATIONS.*")
						|| line.matches("Committee on \\w+.*")	//not perfect, cuts of valid speeches of lines starting with "Committee on "
						//|| line.matches("Committee on \\w+\\.")
						|| line.matches("^∑ .*")
						|| line.matches("(?i:AMENDMENT OFFERED BY.*)")
						)
						){
						specialTermination = true;
					}
					if(speechStarted && (titleDetected || specialTermination)){	//speech ended, write to file
						CongressionalSpeechExtractor.writeToFile(writer, folder + "/" + outputFileName, speech.toString());
						// allSpeechesForDay.append(speech.toString());	//like printing
						speech.setLength(0);
						speechStarted = false;
					}
					specialTermination= false;

					if(titleDetected){	//end current speech & start new
						speechStarted = true;
						speechCount++;
						if(speechCount==1){
							if(!folder.exists()){
								folder.mkdir();
							}
						}
						outputFileName = date+" Speech #"+CongressionalSpeechExtractor.leftPadNumber(speechCount) + " " + speakerName +".txt";
						// speech.append("\n\t"+outputFileName +"\n");
						speech.append(line+"\n");
					}
					else if(speechStarted){
						if(line.matches("(?i:^VerDate.*)")){
							//skip 3 column page breaks with dates
							for(; !line.matches("(?i:^Jkt.*)"); line=reader.readLine(), j++){
							}
						}
						else if(line.matches("^([A-Z]:).*")){	//Starts with E: or other capital		E:\RECORD15\H06JA5.REC
							//Skip 5 lines
							for(int k=0; k<5; line=reader.readLine(), j++, k++){
							}
						}
						else if(line.matches("(?i:^PO 0\\d*.*)")){	//PO 00000 etc. 	column breaks
							for(; !line.matches("(?i:^Sfmt.*)"); line=reader.readLine(), j++){
							}
						}
						else {	//normal line
							speech.append(line+"\n");
						}
					}

				}
				
				System.out.println(date + " " + speechCount +" speeches");
				// CongressionalSpeechExtractor.writeToFile(writer, folder + "/" + date+".txt", allSpeechesForDay.toString());
				
				File newDir = new File(folder.getParent() + "/" + folder.getName() + " Speech Count = " + speechCount);
				folder.renameTo(newDir);
				

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
		}

	}
	
	private static boolean isUpperCase(String string) {
		if(string.length()>=5 && string.substring(0, 5).equalsIgnoreCase("SEC. ")){		//SEC. lll. 	hack
			return true;
		}
		return string.equals(string.toUpperCase());
	}

	private static String extractState(String endOfLine) {
		String state = endOfLine;
		int periodIndex = endOfLine.indexOf(".");
		if(periodIndex>1){
			state = endOfLine.substring(0, periodIndex);
		}
		//else if no period is found, assume end of line is enough of the state (long names/states)

		return state;
	}

	private static void writeToFile(PrintWriter writer, String path, String contents){
		try {
			writer = new PrintWriter(path);
			writer.println(contents);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally{
			writer.close();
		}
	}

	private static String leftPadNumber(int number){
		return leftPadNumber(number, 5);
	}
	private static String leftPadNumber(int number, int desiredLength){
		int numPaddingZeros = desiredLength - (number+"").length();
		return String.join("", Collections.nCopies(numPaddingZeros, "0")) + number;
	}
	

	public static void main(String[] args) {
		CongressionalSpeechExtractor.processFiles();
	}

}