import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Logger {

	static boolean writeSchedules = true;
	static boolean writeSummary = true;
	static boolean writeInfo = true;

	static BufferedWriter scheduleWriter = null;
	static BufferedWriter summaryWriter = null;
	static BufferedWriter infoWriter = null;
	static int subjectColumnLen = 30;
	static int resultColumnLen = 15;
	static int numberColumnLen = 8;

	public static void setScheuleWriter(String outputStream) {
		try {
			scheduleWriter = new BufferedWriter(new FileWriter(outputStream, false));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void setSummaryWriter(String summaryFile) {
		try {
			summaryWriter = new BufferedWriter(new FileWriter(summaryFile, false));
			summaryWriter.write(padRight("subject", subjectColumnLen) + padRight("|result ", resultColumnLen) + padRight("|#sched")
					+ padRight("|#after") + padRight("|#sym") + padRight("|#sets") + " \n \n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void writeScheduleInfo(String msg) {
		if (scheduleWriter != null) {
			try {
				scheduleWriter.write(msg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void reportOriginalSchedulesInfo(String result, int totalSize, ArrayList<ArrayList<Schedule>> scheudlesWithSameEvents) {
		Logger.writeScheduleInfo("**************** RESULT = " + result + " ************************\n");
		Logger.writeScheduleInfo("Total number of schedules = " + totalSize + "\n");
		Logger.writeScheduleInfo("Num of clusters (sets) with same events = " + scheudlesWithSameEvents.size() + "\n");
		Logger.writeScheduleInfo("==============================================\n");
	}

	public static void reportSchedulesOfResult(String result, ArrayList<int[]> setsInfo, ArrayList<ArrayList<Schedule>> scheudlesWithSameEvents,
			ArrayList<ArrayList<Schedule>> finalReducedSchedules) {
		writeScheduleInfo("Num of clusters (sets) with same events  before symmetry = " + scheudlesWithSameEvents.size() + "\n");
		writeScheduleInfo("Num of clusters (sets) with same events  after symmetry = " + finalReducedSchedules.size() + "\n");
		writeScheduleInfo("==============================================\n");

		for (int i = 0; i < finalReducedSchedules.size(); i++) {
			writeScheduleInfo("================= Set " + (i + 1) + " =================\n");

			int[] info = setsInfo.get(i);
			ArrayList<Schedule> schedules = finalReducedSchedules.get(i);
			writeScheduleInfo("Reduction in number of schedules = " + info[1] + ", out of " + info[0] + " schedules \n");
			writeScheduleInfo("Reduction because of symmetry = " + info[3] + " schedules \n");
			writeScheduleInfo("Number of removed constraints =" + info[2] + " \n");

			int scheduleIndex = 0;
			for (Schedule schedule : schedules) {
				scheduleIndex++;
				writeScheduleInfo("---------------------------------------------\n");
				writeScheduleInfo("Schedule: " + scheduleIndex + "\n");
				writeScheduleInfo("Number of removed constraints =" + ((ActorSchedule) schedule).getNumOfRemovedConstraints() + "\n");
				writeScheduleInfo(schedule.toString() + "\n");
			}
		}
		writeScheduleInfo("********************************************\n");
	}

	public static void logInfo(String log) {
		System.out.println(log);
	}

	public static void reportSummary(String subject, HashMap<String, ArrayList<int[]>> resultToSetMap) throws IOException {
		if (summaryWriter != null) {

			for (String result : resultToSetMap.keySet()) {
				int totalBefore = 0;
				int totalAfter = 0;
				int symReduc = 0;
				String resultInfo = "";
				String setInfo = "";
				String[] subjectInfo = subject.replace(".txt", "").split("_");
				String limit = "";
				String hueristic = "";
				for (int i = 0; i < subjectInfo.length; i++) {
					if (subjectInfo[i].equals("limit"))
						limit = subjectInfo[i + 1];
					if (subjectInfo[i].equals("heuristic"))
						hueristic = subjectInfo[i + 1];

				}
				if (subject.contains("zeroleader"))
					subjectInfo[0] = subjectInfo[0] + "zero";
				if (subject.contains("twoleader"))
					subjectInfo[0] = subjectInfo[0] + "two";
				String shortSubject = subjectInfo[0] + "-" + hueristic + "-" + limit;
				String shortResult = result.trim();
				if (result.contains("EXCEPTION"))
					shortResult = "EXCEPTION";
				else if (result.contains("INVALID"))
					shortResult = "INVALID_STATE";
				else if (result.contains("DEADLOCK_DETECTED"))
					shortResult = "DEADLOCK";
				resultInfo += (padRight(shortSubject, subjectColumnLen) + padRight("|" + shortResult, resultColumnLen));
				int setIndex = 0;
				for (int[] setSchedules : resultToSetMap.get(result)) {
					totalBefore += setSchedules[0];
					totalAfter += setSchedules[1];
					symReduc += setSchedules[3];
					setInfo += padLeft("set", subjectColumnLen + resultColumnLen + numberColumnLen * 3 + 3) + (++setIndex) + ": bef#="
							+ setSchedules[0] + ", aft#=" + setSchedules[1] + ", remConst#=" + setSchedules[2] + "\n";

				}
				resultInfo += padRight("|" + String.valueOf(totalBefore))
						+ padRight("|"
								+ String.valueOf((totalAfter + symReduc) + "("
										+ Math.round((double) (totalAfter + symReduc) / (double) totalBefore * 100)) + "%)")
						+ padRight("|" + String.valueOf(totalAfter + "(" + Math.round((double) totalAfter / (double) totalBefore * 100) + "%)"))
						+ padRight("|" + String.valueOf(resultToSetMap.get(result).size())) + "\n";
				summaryWriter.write(resultInfo);
				summaryWriter.write(setInfo + "\n");
			}
			summaryWriter.write("==========================================\n");
		}
	}

	public static void closeScheduleWriter() {
		if (scheduleWriter != null) {
			try {
				scheduleWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void closeSummaryWriter() {
		if (summaryWriter != null) {
			try {
				summaryWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static String padRight(String input, int size) {
		// int numOfwhiteSpaces = size - input.length();
		return String.format("%1$-" + size + "s", input);
	}

	private static String padRight(String input) {
		// int numOfwhiteSpaces = columnLen - input.length();
		return String.format("%1$-" + numberColumnLen + "s", input);
	}

	private static String padLeft(String input, int n) {
		return String.format("%1$#" + n + "s", input);
	}
}
