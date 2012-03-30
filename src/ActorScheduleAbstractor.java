import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ActorScheduleAbstractor extends ScheduleAbstractor {

	String subject = "";
	ArrayList<String> symmetricActors = null;

	void exatractScheulesFromFile(String traceFile, HashMap<String, ArrayList<Schedule>> resultToSchedulesMap) {
		BufferedReader reader;
		String line = "";
		subject = traceFile.substring(traceFile.lastIndexOf("\\") + 1);
		setSymmetricActors();

		try {
			reader = new BufferedReader(new FileReader(traceFile));
			String prevLine = line;
			while ((line = reader.readLine()) != null && !line.startsWith("##"))
				;
			if (line != null) {
				prevLine = line;
				String result = line.split(":")[1];
				while ((line = reader.readLine()) != null) {
					ArrayList<Event> trace = new ArrayList<Event>();
					ActorEvent event = (ActorEvent) parseLineForEvent(line);
					if (event == null)
						Logger.logInfo(prevLine);
					trace.add(event);
					while ((line = reader.readLine()) != null && !line.startsWith("##")) {
						event = (ActorEvent) parseLineForEvent(line);
						if (event == null)
							Logger.logInfo(prevLine);
						ActorEvent eventCustomSeq = event.clone();
						eventCustomSeq.seqNum = "0";
						if (!trace.contains(eventCustomSeq)) {
							// // // System.out.println("repeated event");
							// // int seqNum =
							// Integer.parseInt(event.senderSeqNum);
							// // seqNum++;
							// // event.senderSeqNum = String.valueOf(seqNum);
							trace.add(eventCustomSeq);
						} else {
							int seq = 0;
							while (trace.contains(eventCustomSeq)) {
								eventCustomSeq.seqNum = String.valueOf(++seq);
							}
							trace.add(eventCustomSeq);
						}
					}
					Schedule schedule = getSchedule(trace);
					if (resultToSchedulesMap.containsKey(result)) {
						ArrayList<Schedule> schedules = (ArrayList<Schedule>) resultToSchedulesMap.get(result);
						schedules.add(schedule);
						resultToSchedulesMap.put(result, schedules);
					} else {
						ArrayList<Schedule> schedules = new ArrayList<Schedule>();
						schedules.add(schedule);
						resultToSchedulesMap.put(result, schedules);
					}

					while (line != null && !line.startsWith("##BUG:")) {
						line = reader.readLine();
					}
					if (line != null)
						result = line.split(":")[1];
					prevLine = line;
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setSymmetricActors() {
		if (subject.contains("procreg"))
			symmetricActors = new ArrayList<String>(Arrays.asList("1", "2"));
		else if (subject.contains("procreg1"))
			symmetricActors = new ArrayList<String>(Arrays.asList("1", "2", "3"));
		else if (subject.contains("leader_election_3_1"))
			symmetricActors = new ArrayList<String>(Arrays.asList("3", "4", "5"));
		else if (subject.contains("leader_election_b"))
			symmetricActors = new ArrayList<String>(Arrays.asList("3", "4", "5", "6"));
		else if (subject.contains("minesweeper"))
			symmetricActors = new ArrayList<String>(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9"));
		else if (subject.contains("sleepingbarber"))
			symmetricActors = new ArrayList<String>(Arrays.asList("3", "4", "5", "6"));
		else if (subject.contains("music"))
			symmetricActors = new ArrayList<String>(Arrays.asList("3", "4", "5"));

	}

	@Override
	void clusterSchedules(HashMap<String, ArrayList<Schedule>> resultToSchedulesMap) throws Exception {
		HashMap<String, ArrayList<int[]>> resultToSetMap = new HashMap<String, ArrayList<int[]>>();
		for (String result : resultToSchedulesMap.keySet()) {
			ArrayList<int[]> setsInfo = new ArrayList<int[]>();
			ArrayList<Schedule> schedulesWithSameResult = resultToSchedulesMap.get(result);
			int totalSize = schedulesWithSameResult.size();
			ArrayList<ArrayList<Schedule>> scheudlesWithSameEvents = getScheudlesWithSameEvents(schedulesWithSameResult);

			Logger.reportOriginalSchedulesInfo(result, totalSize, scheudlesWithSameEvents);

			for (ArrayList<Schedule> schedules : scheudlesWithSameEvents) {
				int originalSize = schedules.size();
				applyScheduleAbstraction(setsInfo, schedules, originalSize);
			}

			ArrayList<ArrayList<Schedule>> finalReducedSchedules = applySymmetryReduction(setsInfo, scheudlesWithSameEvents);

			Logger.reportSchedulesOfResult(result, setsInfo, scheudlesWithSameEvents, finalReducedSchedules);
			resultToSetMap.put(result, setsInfo);

		}
		Logger.reportSummary(subject, resultToSetMap);

	}

	private void applyScheduleAbstraction(ArrayList<int[]> setsInfo, ArrayList<Schedule> schedules, int originalSize) throws Exception {
		int i = 0;
		boolean breakLoop = true;
		while (i < schedules.size() - 1 && breakLoop) {
			removeEquivalentSchedules(schedules);
			for (i = 0; i < schedules.size(); i++) {
				breakLoop = false;
				for (int j = i + 1; j < schedules.size(); j++) {
					ActorSchedule schedule1 = (ActorSchedule) schedules.get(i);
					ActorSchedule schedule2 = (ActorSchedule) schedules.get(j);
					HashMap<String, ArrayList<Event[]>> differences = new HashMap<String, ArrayList<Event[]>>();
					HashMap<String, ArrayList<Event[]>> matchingdifferences1 = new HashMap<String, ArrayList<Event[]>>();
					HashMap<String, ArrayList<Event[]>> matchingdifferences2 = new HashMap<String, ArrayList<Event[]>>();
					boolean canBeMerged = true;
					for (String actor : schedule1.actorToConstraintMap.keySet()) {
						Constraint const1 = (Constraint) schedule1.actorToConstraintMap.get(actor);
						Constraint const2 = (Constraint) schedule2.actorToConstraintMap.get(actor);
						ArrayList<Event[]> difference = const1.getDifference(const2);
						if (difference == null) {
							canBeMerged = false;
							break;
						} else if (difference.size() > 0) {
							if (const1.isMatchedWith(const2) || const2.isMatchedWith(const1)) {
								differences.put(actor, difference);
								if (const1.isMatchedWith(const2))
									matchingdifferences1.put(actor, difference);
								else
									matchingdifferences2.put(actor, difference);
							} else if (difference.size() == 1) {
								differences.put(actor, difference);
							} else if (difference.size() >= 2) {
								ArrayList<Event[]> diffIgnoringD = const1.getDifferenceIgnoringD(const2);
								if (diffIgnoringD.size() <= 1)
									differences.put(actor, difference);
								else {
									canBeMerged = false;
									break;
								}
							}

						}
					}
					int matchSizes = matchingdifferences1.size() + matchingdifferences2.size();

					if (canBeMerged && differences.size() <= 1) {
						if (differences.size() == 0) {
							schedules.remove(j);
							// merge them together
							breakLoop = true;
							break;
						}
						for (String actor : differences.keySet()) {
							ArrayList<Event[]> differenceArray = differences.get(actor);
							Constraint const1 = (Constraint) schedule1.actorToConstraintMap.get(actor);
							Constraint const2 = (Constraint) schedule2.actorToConstraintMap.get(actor);
							boolean isSafeToMerge = differenceArray.size() == 1 || isSafeToRelpceAllDiffWithD(const1, const2, differenceArray);
							if (isSafeToMerge) {
								// boolean constraintRemoved = false;
								for (Event[] diff : differenceArray) {
									int c1i = const1.events1.indexOf(diff[0]);
									int c1j = const1.events2.indexOf(diff[1]);
									int c2i = const2.events1.indexOf(diff[0]);
									int c2j = const2.events2.indexOf(diff[1]);
									// if ((const1.HBRel[c1i][c1j] !=
									// HappensBefore.D || const2.HBRel[c2i][c2j]
									// != HappensBefore.D))
									// constraintRemoved = true;
									const1.HBRel[c1i][c1j] = HappensBefore.D;
									const1.HBRel[c1j][c1i] = HappensBefore.D;
								}
								schedules.remove(j);
								breakLoop = true;
								break;
							} else
								System.err.println("is not safe: schedules " + i + " and " + j + " for actor " + actor + " =" + differenceArray.size());
						}
					} else {
						boolean onlyOneIsMatched = (matchingdifferences1.size() == 0 || matchingdifferences2.size() == 0);

						if (canBeMerged && onlyOneIsMatched && (differences.size() - matchSizes) <= 1) {
							int nonMatchingDiffSize = differences.size() - matchSizes;
							if (nonMatchingDiffSize == 0) {
								schedules.remove(j);
								// merge them together
								breakLoop = true;
								break;
							}
							differences.keySet().removeAll(matchingdifferences1.keySet());
							differences.keySet().removeAll(matchingdifferences2.keySet());
							for (String actor : differences.keySet()) {
								ArrayList<Event[]> differenceArray = differences.get(actor);
								Constraint const1 = matchingdifferences1.size() > 0 ? schedule1.actorToConstraintMap.get(actor) : schedule2.actorToConstraintMap.get(actor);
								Constraint const2 = matchingdifferences2.size() > 0 ? schedule2.actorToConstraintMap.get(actor) : schedule1.actorToConstraintMap.get(actor);
								boolean isSafeToMerge = differenceArray.size() == 1 || isSafeToRelpceAllDiffWithD(const1, const2, differenceArray);
								if (isSafeToMerge) {
									// boolean constraintRemoved = false;
									for (Event[] diff : differenceArray) {
										int c1i = const1.events1.indexOf(diff[0]);
										int c1j = const1.events2.indexOf(diff[1]);
										// if (const1.HBRel[c1i][c1j] !=
										// HappensBefore.D)
										// constraintRemoved = true;
										const1.HBRel[c1i][c1j] = HappensBefore.D;
										const1.HBRel[c1j][c1i] = HappensBefore.D;
									}
									// if (constraintRemoved)
									// numOfRemovedConstraints++;
									breakLoop = true;
									break;
								} else
									System.err.println("is not safe: schedules " + i + " and " + j + " for actor " + actor + " =" + differenceArray.size());
							}
						}
					}

				}
				if (breakLoop)
					break;

			}

		}

		int removedConstraints = 0;
		for (Schedule schedule : schedules) {
			removedConstraints += ((ActorSchedule) schedule).getNumOfRemovedConstraints();
		}
		setsInfo.add(new int[] { originalSize, schedules.size(), removedConstraints, 0 });
	}

	public ArrayList<ArrayList<Schedule>> applySymmetryReduction(ArrayList<int[]> setsInfo, ArrayList<ArrayList<Schedule>> scheudlesWithSameEvents) throws Exception {

		if (symmetricActors != null) {

			/**/
			ArrayList<ArrayList<Schedule>> schedulesAfterSymmetryReduction = new ArrayList<ArrayList<Schedule>>();
			ArrayList<ArrayList<Integer>> mergedIndexesArrays = new ArrayList<ArrayList<Integer>>();
			ArrayList<int[]> setsInfoAfterSymmetryReduction = new ArrayList<int[]>();

			ArrayList<ArrayList<Schedule>> schedulesWithSameSymmetricEvents = getScheudlesWithSameSymmetricEvents2(scheudlesWithSameEvents, symmetricActors, mergedIndexesArrays);

			for (ArrayList<Schedule> schedules : schedulesWithSameSymmetricEvents) {
				ArrayList<Schedule> symmetryRemovedSchedules = getSchedulesByRemovingSymmetricEqulivalnetSchedules(schedules, symmetricActors);
				System.out.println(symmetryRemovedSchedules.size() + " " + schedules.size());

				schedulesAfterSymmetryReduction.add(symmetryRemovedSchedules);
				int schedulesIndex = schedulesWithSameSymmetricEvents.indexOf(schedules);
				ArrayList<Integer> mergedIndexArray = mergedIndexesArrays.get(schedulesIndex);
				/*
				 * updating the sets information when they are merged by symmetry policies
				 */
				int[] newInfo = new int[] { 0/* original size */, 0/*
																 * reduced size
																 */, 0/*
																	 * removed constraints
																	 */, 0 /*
																			 * removed symmetric
																			 */};
				for (Integer index : mergedIndexArray) {
					newInfo[0] += setsInfo.get(index)[0];
					newInfo[2] += setsInfo.get(index)[2];
					newInfo[3] += setsInfo.get(index)[1];
				}
				newInfo[1] = symmetryRemovedSchedules.size();
				newInfo[3] -= symmetryRemovedSchedules.size();
				setsInfoAfterSymmetryReduction.add(newInfo);

			}
			setsInfo.clear();
			setsInfo.addAll(setsInfoAfterSymmetryReduction);
			// finalReducedSchedules = schedulesAfterSymmetryReduction;
			return schedulesAfterSymmetryReduction;
		}
		return scheudlesWithSameEvents;
	}

	// public ArrayList<ArrayList<Schedule>>
	// applySymmetryReduction2(ArrayList<int[]> setsInfo,
	// ArrayList<ArrayList<Schedule>> scheudlesWithSameEvents)
	// throws Exception {
	//
	// if (symmetricActors != null) {
	//
	// /**/
	// ArrayList<ArrayList<Schedule>> schedulesAfterSymmetryReduction = new
	// ArrayList<ArrayList<Schedule>>();
	// ArrayList<ArrayList<Integer>> mergedIndexesArrays = new
	// ArrayList<ArrayList<Integer>>();
	// ArrayList<int[]> setsInfoAfterSymmetryReduction = new ArrayList<int[]>();
	//
	// ArrayList<ArrayList<Schedule>> schedulesWithSameSymmetricEvents =
	// getScheudlesWithSameSymmetricEvents(scheudlesWithSameEvents,
	// symmetricActors, mergedIndexesArrays);
	//
	// for (ArrayList<Schedule> schedules : schedulesWithSameSymmetricEvents) {
	// ArrayList<Schedule> symmetryRemovedSchedules =
	// getSchedulesByRemovingSymmetricEqulivalnetSchedules(schedules,
	// symmetricActors);
	// System.out.println(symmetryRemovedSchedules.size() + " " +
	// schedules.size());
	//
	// schedulesAfterSymmetryReduction.add(symmetryRemovedSchedules);
	// int schedulesIndex = schedulesWithSameSymmetricEvents.indexOf(schedules);
	// ArrayList<Integer> mergedIndexArray =
	// mergedIndexesArrays.get(schedulesIndex);
	// /*
	// * updating the sets information when they are merged by
	// * symmetry policies
	// */
	// int[] newInfo = new int[] { 0/* original size */, 0/*
	// * reduced size
	// */, 0/*
	// * removed
	// * constraints
	// */, 0 /*
	// * removed
	// * symmetric
	// */};
	// for (Integer index : mergedIndexArray) {
	// newInfo[0] += setsInfo.get(index)[0];
	// newInfo[2] += setsInfo.get(index)[2];
	// newInfo[3] += setsInfo.get(index)[1];
	// }
	// newInfo[1] = symmetryRemovedSchedules.size();
	// newInfo[3] -= symmetryRemovedSchedules.size();
	// setsInfoAfterSymmetryReduction.add(newInfo);
	//
	// }
	// setsInfo.clear();
	// setsInfo.addAll(setsInfoAfterSymmetryReduction);
	// // finalReducedSchedules = schedulesAfterSymmetryReduction;
	// return schedulesAfterSymmetryReduction;
	// }
	// return scheudlesWithSameEvents;
	// }

	private void removeEquivalentSchedules(ArrayList<Schedule> schedules) throws Exception {
		ArrayList<Schedule> redundantSchedules = new ArrayList<Schedule>();
		for (int i = 0; i < schedules.size(); i++) {
			ActorSchedule schedule1 = (ActorSchedule) schedules.get(i);
			if (!redundantSchedules.contains(schedule1)) {
				for (int j = i + 1; j < schedules.size(); j++) {
					ActorSchedule schedule2 = (ActorSchedule) schedules.get(j);
					if (!redundantSchedules.contains(schedule2)) {
						boolean equal = true;
						if (!schedule1.actorToConstraintMap.keySet().equals(schedule2.actorToConstraintMap.keySet())) {
							equal = false;
						} else {
							for (String actor : schedule1.actorToConstraintMap.keySet()) {
								Constraint const1 = (Constraint) schedule1.actorToConstraintMap.get(actor);
								Constraint const2 = (Constraint) schedule2.actorToConstraintMap.get(actor);
								ArrayList<Event[]> difference = const1.getDifference(const2);
								if (difference != null && difference.size() > 0) {
									equal = false;
									break;
								}
							}
						}
						if (equal) {
							redundantSchedules.add(schedule2);
						}
					}
				}
			}
		}
		for (Schedule schedule : redundantSchedules) {
			schedules.remove(schedule);
		}

	}

	private ArrayList<Schedule> getSchedulesByRemovingSymmetricEqulivalnetSchedules(ArrayList<Schedule> schedules, ArrayList<String> symmetricActors) throws Exception {
		ArrayList<Schedule> result = new ArrayList<Schedule>();
		ArrayList<Schedule> redundantSchedules = new ArrayList<Schedule>();
		for (int i = 0; i < schedules.size(); i++) {
			ActorSchedule schedule1 = (ActorSchedule) schedules.get(i);
			if (!redundantSchedules.contains(schedule1)) {
				result.add(schedule1);
				for (int j = i + 1; j < schedules.size(); j++) {
					ActorSchedule schedule2 = (ActorSchedule) schedules.get(j);
					if (!redundantSchedules.contains(schedule2)) {
						boolean matched1with2 = schedule1.isMatchedByApplyingSymmetry(schedule2, symmetricActors);
						boolean matched2with1 = schedule2.isMatchedByApplyingSymmetry(schedule1, symmetricActors);
						if (matched1with2 || matched2with1) {
							if (matched1with2 && !matched2with1) {
								result.remove(schedule1);
								result.add(schedule2);
								redundantSchedules.add(schedule1);
							}
							redundantSchedules.add(schedule2);
							Logger.logInfo("symmetric:" + schedule1 + "\n ----------- \n" + schedule2);
							Logger.logInfo("************");
						}
					}
				}
			}

		}
		return result;
	}

	private boolean isSafeToRelpceAllDiffWithD(Constraint const1, Constraint const2, ArrayList<Event[]> differenceArray) {
		HappensBefore[][] hbrCopy = const1.HBRel.clone();
		boolean[] assignment = new boolean[differenceArray.size()];
		Constraint temp = new Constraint(const1.events1, const1.events1);
		while (assignment != null) {
			for (int i = 0; i < differenceArray.size(); i++) {
				Event[] diff = differenceArray.get(i);
				int c1i = const1.events1.indexOf(diff[0]);
				int c1j = const1.events2.indexOf(diff[1]);
				hbrCopy[c1i][c1j] = assignment[i] ? HappensBefore.Y : HappensBefore.N;
			}
			temp.HBRel = hbrCopy;
			if (isValid(hbrCopy) && !temp.isMatchedWith(const1) && !temp.isMatchedWith(const2)) {
				System.err.println("is not safe to merge \n");
				return false;
			}
			assignment = increment(assignment);
		}

		return true;

	}

	private boolean[] increment(boolean[] assignment) {
		int index = 0;
		while (index < assignment.length && assignment[index]) {
			assignment[index] = false;
			index++;
		}
		if (index < assignment.length) {
			assignment[index] = true;
			return assignment;
		} else
			return null;
	}

	private boolean isValid(HappensBefore[][] hbrCopy) {
		for (int i = 0; i < hbrCopy.length; i++) {
			for (int j = 1; j < hbrCopy[i].length; j++) {
				if (hbrCopy[i][j].ordinal() < hbrCopy[i][j - 1].ordinal())
					return false;
			}
		}
		for (int i = 1; i < hbrCopy.length - 1; i++) {
			for (int j = 0; j < hbrCopy[i].length; j++) {
				if (hbrCopy[i - 1][j].ordinal() < hbrCopy[i][j].ordinal())
					return false;
			}
		}
		return true;
	}

	private ArrayList<ArrayList<Schedule>> getScheudlesWithSameEvents(ArrayList<Schedule> schedulesWithSameResult) {
		ArrayList<ArrayList<Schedule>> result = new ArrayList<ArrayList<Schedule>>();
		ArrayList<Schedule> removedSchedules = new ArrayList<Schedule>();
		for (int i = 0; i < schedulesWithSameResult.size(); i++) {
			ActorSchedule schedule1 = (ActorSchedule) schedulesWithSameResult.get(i);
			if (!removedSchedules.contains(schedule1)) {
				ArrayList<Schedule> schedulesWithSameEvents = new ArrayList<Schedule>();
				removedSchedules.add(schedule1);
				schedulesWithSameEvents.add(schedule1);
				for (int j = i + 1; j < schedulesWithSameResult.size(); j++) {
					ActorSchedule schedule2 = (ActorSchedule) schedulesWithSameResult.get(j);
					if (!removedSchedules.contains(schedule2)) {
						boolean haveSameEvents = schedule1.hasTheSameEvents(schedule2);
						if (haveSameEvents) {
							schedulesWithSameEvents.add(schedule2);
							removedSchedules.add(schedule2);
						}
					}
				}
				result.add(schedulesWithSameEvents);
			}
		}

		// TODO Auto-generated method stub
		return result;
	}

	private ArrayList<ArrayList<Schedule>> getScheudlesWithSameSymmetricEvents2(ArrayList<ArrayList<Schedule>> schedulesWithSameEvents, ArrayList<String> symmetricActors,
			ArrayList<ArrayList<Integer>> mergedIndexArrays) {
		ArrayList<ArrayList<Schedule>> result = new ArrayList<ArrayList<Schedule>>();
		ArrayList<Schedule> removedSchedules = new ArrayList<Schedule>();
		for (int i = 0; i < schedulesWithSameEvents.size(); i++) {
			ActorSchedule schedule1 = (ActorSchedule) schedulesWithSameEvents.get(i).get(0);
			if (!removedSchedules.contains(schedule1)) {

				removedSchedules.add(schedule1);

				ArrayList<Schedule> schedulesWithSameSymmetryEvents = new ArrayList<Schedule>();
				ArrayList<Integer> mergedIndexArray = new ArrayList<Integer>();

				mergedIndexArray.add(Integer.valueOf(i));
				schedulesWithSameSymmetryEvents.addAll(schedulesWithSameEvents.get(i));

				for (int j = i + 1; j < schedulesWithSameEvents.size(); j++) {
					ActorSchedule schedule2 = (ActorSchedule) schedulesWithSameEvents.get(j).get(0);
					if (!removedSchedules.contains(schedule2)) {
						boolean haveSameEvents = schedule1.hasTheSameSymmetricEvents(schedule2, symmetricActors);
						if (haveSameEvents) {
							schedulesWithSameSymmetryEvents.addAll(schedulesWithSameEvents.get(j));
							removedSchedules.add(schedule2);
							mergedIndexArray.add(Integer.valueOf(j));
						}
					}

				}
				result.add(schedulesWithSameSymmetryEvents);
				mergedIndexArrays.add(mergedIndexArray);
			}
		}
		return result;
	}

	// private ArrayList<ArrayList<Schedule>>
	// getScheudlesWithSameSymmetricEvents(ArrayList<ArrayList<Schedule>>
	// schedulesWithSameEvents,
	// ArrayList<String> symmetricActors, ArrayList<ArrayList<Integer>>
	// mergedIndexArrays) {
	// ArrayList<ArrayList<Schedule>> result = new
	// ArrayList<ArrayList<Schedule>>();
	// ArrayList<Schedule> removedSchedules = new ArrayList<Schedule>();
	// for (int i = 0; i < schedulesWithSameEvents.size(); i++) {
	// ActorSchedule schedule1 = (ActorSchedule)
	// schedulesWithSameEvents.get(i).get(0);
	// if (!removedSchedules.contains(schedule1)) {
	//
	// removedSchedules.add(schedule1);
	//
	// ArrayList<Schedule> schedulesWithSameSymmetryEvents = new
	// ArrayList<Schedule>();
	// ArrayList<Integer> mergedIndexArray = new ArrayList<Integer>();
	//
	// mergedIndexArray.add(Integer.valueOf(i));
	// schedulesWithSameSymmetryEvents.addAll(schedulesWithSameEvents.get(i));
	//
	// for (int j = i + 1; j < schedulesWithSameEvents.size(); j++) {
	// ActorSchedule schedule2 = (ActorSchedule)
	// schedulesWithSameEvents.get(j).get(0);
	// if (!removedSchedules.contains(schedule2)) {
	// boolean haveSameEvents =
	// schedule1.hasTheSameEventsByIgnoringSomeActors(schedule2,
	// symmetricActors);
	// if (haveSameEvents) {
	// schedulesWithSameSymmetryEvents.addAll(schedulesWithSameEvents.get(j));
	// removedSchedules.add(schedule2);
	// mergedIndexArray.add(Integer.valueOf(j));
	// }
	// }
	//
	// }
	// result.add(schedulesWithSameSymmetryEvents);
	// mergedIndexArrays.add(mergedIndexArray);
	// }
	// }
	// return result;
	// }

	// private ArrayList<ArrayList<Schedule>>
	// getEqulivalnetSchedulesByIgnoringSomeActors(ArrayList<Schedule>
	// schedulesWithSameEvents,
	// ArrayList<String> symmetricActors) throws Exception {
	// ArrayList<ArrayList<Schedule>> result = new
	// ArrayList<ArrayList<Schedule>>();
	// ArrayList<Schedule> removedSchedules = new ArrayList<Schedule>();
	// for (int i = 0; i < schedulesWithSameEvents.size(); i++) {
	// ActorSchedule schedule1 = (ActorSchedule) schedulesWithSameEvents.get(i);
	// if (!removedSchedules.contains(schedule1)) {
	// ArrayList<Schedule> symmetryEquivalentSchedules = new
	// ArrayList<Schedule>();
	// removedSchedules.add(schedule1);
	// symmetryEquivalentSchedules.add(schedule1);
	// for (int j = i + 1; j < schedulesWithSameEvents.size(); j++) {
	// ActorSchedule schedule2 = (ActorSchedule) schedulesWithSameEvents.get(j);
	// if (!removedSchedules.contains(schedule2)) {
	// boolean equivalent = schedule1.equalsByApplyingSymmetry(schedule2,
	// symmetricActors);
	// if (equivalent) {
	// symmetryEquivalentSchedules.add(schedule2);
	// removedSchedules.add(schedule2);
	// }
	// }
	// }
	// result.add(symmetryEquivalentSchedules);
	// }
	//
	// }
	//
	// // TODO Auto-generated method stub
	// return result;
	// }

	// private boolean reportSetOfSchedulesWithSameEvents(ArrayList<Schedule>
	// schedules) {
	// int clusterNum = 0;
	// ArrayList<Schedule> copySchedule = (ArrayList<Schedule>)
	// schedules.clone();
	// boolean loopBreak = false;
	// for (int i = 0; i < copySchedule.size(); i++) {
	// loopBreak = false;
	// ActorSchedule schedule1 = (ActorSchedule) copySchedule.get(i);
	// for (int j = i + 1; j < copySchedule.size(); j++) {
	// loopBreak = false;
	// ActorSchedule schedule2 = (ActorSchedule) copySchedule.get(j);
	// for (String actor : schedule1.actorToConstraintMap.keySet()) {
	// Constraint const1 = (Constraint)
	// schedule1.actorToConstraintMap.get(actor);
	// Constraint const2 = (Constraint)
	// schedule2.actorToConstraintMap.get(actor);
	// for (Event event : const1.events1) {
	// if (!const2.events1.contains(event)) {
	// clusterNum++;
	// loopBreak = true;
	// break;
	// }
	// }
	// if (loopBreak)
	// break;
	//
	// }
	// if (!loopBreak)
	// copySchedule.remove(j);
	// }
	// }
	// // TODO Auto-generated method stub
	// Logger.logSchedule("number of clusters with the same set of events = " +
	// copySchedule.size() + " \n");
	// if (clusterNum == 0)
	// return true;
	// else
	// return false;
	//
	// }

	@Override
	Event parseLineForEvent(String line) {
		String[] eventParts = line.split(":");
		String vc = eventParts[5].substring(1, eventParts[5].length() - 1);
		String[] msgVc = (vc.length() > 0) ? Arrays.copyOfRange(vc.split(", "), 0, 11) : null;
		if (eventParts.length > 6) {
			try {
				vc = eventParts[6].substring(1, eventParts[6].length() - 1);
				String[] recVc = (vc.length() > 0) ? Arrays.copyOfRange(vc.split(", "), 0, 11) : null;
				return new ActorEvent(eventParts[0], eventParts[1], eventParts[3], msgVc, recVc);
			} catch (Exception ex) {
				// String[] eventParts = line.split(":");
				Logger.logInfo(line);
				return null;
			}
		} else
			return new ActorEvent(eventParts[0], eventParts[1], eventParts[3], msgVc);

	}

	@Override
	Schedule getSchedule(ArrayList<Event> trace) {
		HashMap<String, ArrayList<ActorEvent>> actorToEventMap = new HashMap<String, ArrayList<ActorEvent>>();
		for (Event event : trace) {
			ActorEvent aEvent = (ActorEvent) event;
			if (actorToEventMap.containsKey(aEvent.receiver)) {
				actorToEventMap.get(aEvent.receiver).add(aEvent);
			} else {
				ArrayList<ActorEvent> events = new ArrayList<ActorEvent>();
				events.add(aEvent);
				actorToEventMap.put(aEvent.receiver, events);
			}

		}
		ActorSchedule schedule = new ActorSchedule();
		for (String actor : actorToEventMap.keySet()) {
			ArrayList<ActorEvent> actorTrace = actorToEventMap.get(actor);
			Constraint constraint = new Constraint(actorTrace, actorTrace);
			for (int i = 0; i < actorTrace.size(); i++) {
				constraint.HBRel[i][i] = HappensBefore.D;
				for (int j = i + 1; j < actorTrace.size(); j++) {
					// if (actorTrace.get(i).equals(actorTrace.get(j)))
					// constraint.HBRel[i][j] = HappensBefore.D;
					// else
					constraint.HBRel[i][j] = HappensBefore.Y;
				}
			}
			schedule.actorToConstraintMap.put(actor, constraint);
		}
		return schedule;
	}
}
