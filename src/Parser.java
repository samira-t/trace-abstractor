import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Parser {

	public static void main(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		String line = "";
		ArrayList<String> buggyTraceContent = new ArrayList<String>();
		try {
			while ((line = reader.readLine()) != null && !line.startsWith("##BUG"));
			while ((line = reader.readLine()) != null && !line.startsWith("#"))
			{
				buggyTraceContent.add(line); 
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		simplifyTrace(buggyTraceContent, args[0]);
	}
	
	private static void simplifyTrace(ArrayList<String> traceContent,
			String fileName) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName
				+ "simplified", true));
//		writer.write(traceContent.get(0));
//		writer.newLine();
		ArrayList<String> lastLines = findLastRecievers(traceContent);
		for (String line: traceContent) {
			String[] vc = getVC(line);
			for (String lastLine : lastLines) {
				String[] lastVC = getVC(lastLine);
				if (isLessThanOrEqual(vc, lastVC)) {
					writer.write(line);
					writer.newLine();
					break;
				}
			}
		}
		writer.close();

	}

	private static ArrayList<String> findLastRecievers(
			ArrayList<String> traceContent) {
		ArrayList<String> result = new ArrayList<String>();
		ArrayList<Integer> receivers = new ArrayList<Integer>();

		for (int i =traceContent.size()-1; i>=0; i--) {
			
			int receiver = Integer.parseInt((traceContent.get(i).split(":"))[1]);
			//if (!receivers.contains(receiver)) {
			if (receiver == 9){
				receivers.add(receiver);
				result.add(traceContent.get(i));
				break;
			}
		}

		return result;
	}

	private static String[] getVC(String line) {
		return line.substring(line.indexOf('['), line.indexOf(']')).split(",");
	}

	private static boolean isLessThanOrEqual(String[] vc1, String[] vc2) {
		for (int i = 0; i < vc1.length; i++) {
			if (vc1[i].compareTo(vc2[i]) > 0)
				return false;
		}
		return true;
	}

	public static void generateDelta(String fileName) {

	}

}
