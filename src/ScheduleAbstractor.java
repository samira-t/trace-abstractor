import java.util.ArrayList;
import java.util.HashMap;

abstract class ScheduleAbstractor {
	public void abstractSchedules(String[] args) throws Exception {
		HashMap<String, ArrayList<Schedule>> resultToSchedulesMap = new HashMap<String, ArrayList<Schedule>>();
		exatractScheulesFromFile(args[0], resultToSchedulesMap);

		// summarizeDistances(resultToSchedulesMap, args[0]+"_distancesummary");
		clusterSchedules(resultToSchedulesMap);
		// clusterSchedules2(resultToSchedulesMap, args[0] +
		// "_clustersummary2");

	}

	abstract void clusterSchedules(HashMap<String, ArrayList<Schedule>> resultToSchedulesMap) throws Exception;

	abstract void exatractScheulesFromFile(String tarceFile, HashMap<String, ArrayList<Schedule>> resultToSchedulesMap);

	abstract Event parseLineForEvent(String line);

	abstract Schedule getSchedule(ArrayList<Event> trace);

}
