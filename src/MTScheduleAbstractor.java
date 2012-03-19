import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MTScheduleAbstractor extends ScheduleAbstractor {

	void exatractScheulesFromFile(String input, HashMap<String, ArrayList<Schedule>> resultToSchedulesMap) {
		String[] tids = { "", "" };// { args[1], args[2] };
		BufferedReader reader;
		String line = "";

		try {
			reader = new BufferedReader(new FileReader(input));
			while ((line = reader.readLine()) != null) {
				while ((line = reader.readLine()) != null && !line.startsWith("("))
					;
				if (line != null) {
					ArrayList<Event> trace = new ArrayList<Event>();
					MTEvent event = (MTEvent) parseLineForEvent(line);
					if (!event.tid.equals("Thread-0")) {
						if (!trace.contains(event))
							trace.add(event);
						else
							trace.add(new MTEvent(event.tid, event.line + ".5"));
					}
					while ((line = reader.readLine()) != null && !line.startsWith("RESULT")) {
						event = (MTEvent) parseLineForEvent(line);
						if (!event.tid.equals("Thread-0")) {
							if (!trace.contains(event))
								trace.add(event);
							else
								trace.add(new MTEvent(event.tid, event.line + ".5"));
						}
					}
					Schedule schedule = getSchedule(trace, tids);
					if (resultToSchedulesMap.containsKey(line)) {
						ArrayList<Schedule> schedules = (ArrayList<Schedule>) resultToSchedulesMap.get(line);
						schedules.add(schedule);
						resultToSchedulesMap.put(line, schedules);
					} else {
						ArrayList<Schedule> schedules = new ArrayList<Schedule>();
						schedules.add(schedule);
						resultToSchedulesMap.put(line, schedules);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void summarizeDistances(HashMap<String, ArrayList<Schedule>> resultToSchedulesMap, String resultFile) {
		try {
			BufferedWriter resultWriter = new BufferedWriter(new FileWriter(resultFile, false));

			for (String key : resultToSchedulesMap.keySet()) {
				ArrayList<Schedule> schedules = resultToSchedulesMap.get(key);
				System.out.println(key + " " + ((MTSchedule) schedules.get(0)).constraintsMatrix.length + " " + ((MTSchedule) schedules.get(0)).events1.length
						* ((MTSchedule) schedules.get(0)).events2.length);
				resultWriter.write(key + " " + ((MTSchedule) schedules.get(0)).constraintsMatrix.length + " " + ((MTSchedule) schedules.get(0)).events1.length
						* ((MTSchedule) schedules.get(0)).events2.length + "\n");
				for (int i = 0; i < schedules.size() - 1; i++) {
					for (int j = i + 1; j < schedules.size(); j++) {
						System.out.println(i + " and " + j + "=" + ((MTSchedule) schedules.get(i)).getDifference(schedules.get(j)).size());
						resultWriter.write(i + " and " + j + "=" + ((MTSchedule) schedules.get(i)).getDifference(schedules.get(j)).size() + "\n");

					}
				}
				System.out.println("=============================================");
				resultWriter.write("=============================================\n");
			}
			resultWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub

	}

	void clusterSchedules(HashMap<String, ArrayList<Schedule>> resultToSchedulesMap) {
		String resultFile = "temp";
		try {
			BufferedWriter resultWriter = new BufferedWriter(new FileWriter(resultFile, false));

			for (String key : resultToSchedulesMap.keySet()) {
				ArrayList<Schedule> schedules = resultToSchedulesMap.get(key);
				int i = 0;
				resultWriter.write(key + " " + "\n");
				while (i < schedules.size() - 1) {
					boolean breakLoop = false;
					for (i = 0; i < schedules.size() - 1; i++) {
						for (int j = i + 1; j < schedules.size(); j++) {
							ArrayList<int[]> differences = ((MTSchedule) schedules.get(i)).getDifference(schedules.get(j));
							ArrayList<int[]> realDifferences = ((MTSchedule) schedules.get(i)).getRealDistance(schedules.get(j));
							if (differences.size() <= 1) {
								System.out.println("Merge is Valid ");
								if (differences.size() == 1) {
									for (int[] diff : realDifferences) {
										((MTSchedule) schedules.get(i)).constraintsMatrix[diff[0]][diff[1]] = HappensBefore.D;
									}
									resultWriter.write(i + " and " + j + ":" + " are clustered." + differences.get(0)[0] + "," + differences.get(0)[1] + "= D \n");
								} else {
									resultWriter.write(i + " and " + j + ":" + " are clustered. \n");
								}
								schedules.remove(j);
								breakLoop = true;
								break;
							}
						}

						if (breakLoop)
							break;
					}
				}
				resultWriter.write("number of clusters=" + schedules.size() + "\n");
				for (Schedule schedule : schedules)
					resultWriter.write(schedule.toString() + "\n");
				resultWriter.write("=============================================\n");
			}
			resultWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub

	}

	private void clusterSchedules3(HashMap<String, ArrayList<Schedule>> resultToSchedulesMap, String resultFile) {
		try {
			BufferedWriter resultWriter = new BufferedWriter(new FileWriter(resultFile, false));

			for (String key : resultToSchedulesMap.keySet()) {
				ArrayList<Schedule> schedules = resultToSchedulesMap.get(key);
				int i = 0;
				resultWriter.write(key + " " + "\n");
				while (i < schedules.size() - 1) {
					boolean breakLoop = false;
					for (i = 0; i < schedules.size() - 1; i++) {
						System.out.println(schedules.get(i).toString());
						for (int j = i + 1; j < schedules.size(); j++) {
							MTSchedule schedule1 = (MTSchedule) schedules.get(i);
							MTSchedule schedule2 = (MTSchedule) schedules.get(j);
							ArrayList<int[]> differences = schedule1.getDifference(schedule2);
							ArrayList<int[]> realDifferences = schedule1.getRealDistance(schedule2);
							ArrayList<int[]> oneWayDiff1 = schedule1.getOneWayDistanceIgnoringD(schedule2);
							ArrayList<int[]> oneWayDiff2 = schedule2.getOneWayDistanceIgnoringD(schedule1);
							boolean needCheckingForReplacementWithD = realDifferences.size() > 1; // &&
																									// differences.size()>0
							// ;//(oneWayDiff1.size()>0)&&
							// (oneWayDiff2.size() >0);
							if (differences.size() <= 1) {
								if (!needCheckingForReplacementWithD || ScheduleUtil.isSafeToReplaceWithD(realDifferences, schedule1, schedule2)) {
									System.out.println("Merge is Valid ");
									if (differences.size() == 1) {
										for (int[] diff : realDifferences) {
											((MTSchedule) schedules.get(i)).constraintsMatrix[diff[0]][diff[1]] = HappensBefore.D;
										}
										resultWriter.write(i + " and " + j + ":" + " are clustered." + differences.get(0)[0] + "," + differences.get(0)[1] + "= D \n");
									} else {
										resultWriter.write(i + " and " + j + ":" + " are clustered. \n");
									}
									schedules.remove(j);
									breakLoop = true;
									break;
								} else {
									if (oneWayDiff1.size() == 1 && schedule1.constraintsMatrix[oneWayDiff1.get(0)[0]][oneWayDiff1.get(0)[1]] != HappensBefore.D) {
										schedule1.constraintsMatrix[oneWayDiff1.get(0)[0]][oneWayDiff1.get(0)[1]] = HappensBefore.D;
										resultWriter.write(" a constraint is removed:  " + oneWayDiff1.get(0)[0] + "," + oneWayDiff1.get(0)[1] + "= D \n");
										breakLoop = true;
										break;
									}
									if (oneWayDiff2.size() == 1 && schedule2.constraintsMatrix[oneWayDiff2.get(0)[0]][oneWayDiff2.get(0)[1]] != HappensBefore.D) {
										schedule2.constraintsMatrix[oneWayDiff2.get(0)[0]][oneWayDiff2.get(0)[1]] = HappensBefore.D;
										resultWriter.write(" a constraint is removed:  " + oneWayDiff2.get(0)[0] + "," + oneWayDiff2.get(0)[1] + "= D \n");
										breakLoop = true;
										break;
									}
								}
							}
						}
						if (breakLoop)
							break;
					}
				}
				resultWriter.write("number of clusters=" + schedules.size() + "\n");
				for (Schedule schedule : schedules)
					resultWriter.write(schedule.toString() + "\n");
				resultWriter.write("=============================================\n");
			}
			resultWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub

	}

	private void clusterSchedules2(HashMap<String, ArrayList<Schedule>> resultToSchedulesMap, String resultFile) {
		try {
			BufferedWriter resultWriter = new BufferedWriter(new FileWriter(resultFile, false));

			for (String key : resultToSchedulesMap.keySet()) {
				ArrayList<Schedule> schedules = resultToSchedulesMap.get(key);
				int i = 0;
				boolean breakLoop = false;
				resultWriter.write(key + " " + "\n");
				int scheduleSize = schedules.size();
				int initialNumberOfSchedules = scheduleSize;
				while (i < scheduleSize - 1) {
					breakLoop = false;
					for (i = 0; i < scheduleSize - 1; i++) {
						for (int j = i + 1; j < scheduleSize; j++) {
							ArrayList<int[]> differencesIgnoringWild1 = ((MTSchedule) schedules.get(i)).getOneWayDistanceIgnoringD(schedules.get(j));
							ArrayList<int[]> differencesIgnoringWild2 = ((MTSchedule) schedules.get(j)).getOneWayDistanceIgnoringD(schedules.get(i));
							ArrayList<int[]> realDifferences = ((MTSchedule) schedules.get(i)).getRealDistance(schedules.get(j));
							if (differencesIgnoringWild1.size() <= 1 || differencesIgnoringWild2.size() <= 1) {
								resultWriter
										.write("RealDistance= " + realDifferences.size() + ", VirtualDistances=" + differencesIgnoringWild1.size() + " " + differencesIgnoringWild2.size() + "%%\n");
								if (realDifferences.size() == 1) {
									if (((MTSchedule) schedules.get(i)).constraintsMatrix[realDifferences.get(0)[0]][realDifferences.get(0)[1]] != HappensBefore.D) {
										((MTSchedule) schedules.get(i)).constraintsMatrix[realDifferences.get(0)[0]][realDifferences.get(0)[1]] = HappensBefore.D;
										resultWriter.write(i + " and " + j + ":" + " are clustered." + realDifferences.get(0)[0] + "," + realDifferences.get(0)[1] + "= D; \n");
										schedules.remove(j);
										breakLoop = true;
										break;
									} else if (((MTSchedule) schedules.get(j)).constraintsMatrix[realDifferences.get(0)[0]][realDifferences.get(0)[1]] != HappensBefore.D) {
										((MTSchedule) schedules.get(i)).constraintsMatrix[realDifferences.get(0)[0]][realDifferences.get(0)[1]] = HappensBefore.D;
										resultWriter.write(j + " and " + i + ":" + " are clustered." + realDifferences.get(0)[0] + "," + realDifferences.get(0)[1] + "= D; \n");
										schedules.remove(i);
										breakLoop = true;
										break;
									}

								} else if (realDifferences.size() == 0) {
									resultWriter.write("One of the " + i + " and " + j + ":" + " is removed. \n");
									schedules.remove(j);
									breakLoop = true;
									break;
								} else {
									if (differencesIgnoringWild1.size() <= 1) {
										if (differencesIgnoringWild1.size() == 0) {
											schedules.remove(i);
											resultWriter.write(i + " matched with " + j + " and removed \n");
											breakLoop = true;
											break;
										} else {
											if (((MTSchedule) schedules.get(i)).constraintsMatrix[differencesIgnoringWild1.get(0)[0]][differencesIgnoringWild1.get(0)[1]] != HappensBefore.D) {
												((MTSchedule) schedules.get(i)).constraintsMatrix[differencesIgnoringWild1.get(0)[0]][differencesIgnoringWild1.get(0)[1]] = HappensBefore.D;
												resultWriter.write(i + " is generalized " + differencesIgnoringWild1.get(0)[0] + "," + differencesIgnoringWild1.get(0)[1] + "= D; \n");
												breakLoop = true;
											} else if (realDifferences.size() == 2) {
												((MTSchedule) schedules.get(i)).constraintsMatrix[realDifferences.get(0)[0]][realDifferences.get(0)[1]] = HappensBefore.D;
												((MTSchedule) schedules.get(i)).constraintsMatrix[realDifferences.get(1)[0]][realDifferences.get(1)[1]] = HappensBefore.D;
												resultWriter.write(j + "is removed and " + i + " is generalized " + realDifferences.get(0)[0] + "," + realDifferences.get(0)[1] + "= D; and "
														+ realDifferences.get(1)[0] + "," + realDifferences.get(1)[1] + "= D; \n");
												schedules.remove(j);
												breakLoop = true;
												break;
											}

										}
									}
									if (differencesIgnoringWild2.size() <= 1) {
										if (differencesIgnoringWild2.size() == 0) {
											schedules.remove(j);
											resultWriter.write(j + " matched with " + i + " and removed \n");
											breakLoop = true;
											break;
										} else {
											if (((MTSchedule) schedules.get(j)).constraintsMatrix[differencesIgnoringWild2.get(0)[0]][differencesIgnoringWild2.get(0)[1]] != HappensBefore.D) {
												((MTSchedule) schedules.get(j)).constraintsMatrix[differencesIgnoringWild2.get(0)[0]][differencesIgnoringWild2.get(0)[1]] = HappensBefore.D;
												resultWriter.write(j + " is generalized " + differencesIgnoringWild2.get(0)[0] + "," + differencesIgnoringWild2.get(0)[1] + "= D; \n");
												breakLoop = true;
											}
										}
									}
									if (breakLoop)
										break;
								}
								// breakLoop = true;
								// break;
							}
						}
						if (breakLoop)
							break;
					}
					if (schedules.size() < scheduleSize && schedules.size() > 0)
						i--;
					scheduleSize = schedules.size();
				}
				resultWriter.write("---------------------------------------------\n");
				resultWriter.write("number of clusters=" + schedules.size() + ", out of " + initialNumberOfSchedules + " schedules \n");
				for (Schedule schedule : schedules)
					resultWriter.write(schedule.toString() + "\n");
				resultWriter.write("=============================================\n");
			}
			resultWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub

	}

	private String extractminimalConstraintsFromSchedule(MTSchedule schedule) {
		ArrayList<ArrayList<Event>> minimalConstraints = new ArrayList<ArrayList<Event>>();
		for (int i = schedule.events1.length - 1; i >= 0; i--) {
			for (int j = 0; j < schedule.events2.length; j++) {
				// if (schedule.constrains[i][j] == )
			}
		}
		return null;
	}

	Schedule getSchedule(ArrayList<Event> trace, String[] args) {
		ArrayList<MTEvent> events1 = new ArrayList<MTEvent>();
		ArrayList<MTEvent> events2 = new ArrayList<MTEvent>();
		String[] tids = { args[1], args[2] };
		for (Event event : trace) {
			MTEvent mtEvent = (MTEvent) event;
			if (tids[0].equals(mtEvent.tid)) {
				events1.add(mtEvent);
			} else {
				events2.add(mtEvent);
			}

		}
		MTSchedule schedule = new MTSchedule(events1, events2);
		MTEvent prevEvent = null;
		int r = -1;
		int c = -1;
		for (Event event : trace) {
			MTEvent mtEvent = (MTEvent) event;
			if (mtEvent.tid.equals(tids[0])) {
				r++;
			} else {

				c++;
				if (prevEvent != null && prevEvent.tid.equals(tids[0])) {
					for (int i = 0; i <= r; i++) {
						for (int j = c; j < events2.size(); j++) {
							schedule.constraintsMatrix[i][j] = HappensBefore.Y;
						}
					}
				}
			}
			prevEvent = mtEvent;
		}

		return schedule;

	}

	Event parseLineForEvent(String line) {

		String tid = line.substring(1, line.indexOf(","));
		String lineNumber = line.substring(line.indexOf(",") + 1, line.lastIndexOf(")")).trim();
		return new MTEvent(tid, lineNumber);
	}

	@Override
	Schedule getSchedule(ArrayList<Event> trace) {
		// TODO Auto-generated method stub
		return null;
	}

}
