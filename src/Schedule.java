import java.util.ArrayList;
import java.util.HashMap;

enum HappensBefore {
	N, D, Y
}

class ScheduleUtil {
	public static boolean isSafeToReplaceWithD(ArrayList<int[]> diff, MTSchedule schedule1, MTSchedule schedule2) {
		boolean[] diffLine = new boolean[diff.size()];

		while (diffLine != null) {
			MTSchedule temp1 = (MTSchedule) schedule1.clone();
			MTSchedule temp2 = (MTSchedule) schedule2.clone();
			for (int i = 0; i < diff.size(); i++) {
				temp1.constraintsMatrix[diff.get(i)[0]][diff.get(i)[1]] = (diffLine[i]) ? (HappensBefore.Y) : (HappensBefore.N);
				temp2.constraintsMatrix[diff.get(i)[0]][diff.get(i)[1]] = (diffLine[i]) ? (HappensBefore.Y) : (HappensBefore.N);
			}
			if (temp1.isValid() && temp1.getOneWayDistanceIgnoringD(schedule1).size() > 0 && temp1.getOneWayDistanceIgnoringD(schedule2).size() > 0)
				return false;
			// if (temp2.isValid() &&
			// temp2.getOneWayDistanceIgnoringD(schedule2).size() > 0 ) return
			// false;
			diffLine = add(diffLine);
		}
		return true;

	}

	private static boolean[] add(boolean[] diff) {
		int i = 0;
		while (i < diff.length && diff[i]) {
			diff[i] = false;
			i++;
		}
		if (i < diff.length) {
			diff[i] = true;
			return diff;
		}
		return null;
	}

}

abstract class Schedule {

	public abstract boolean isValid();

	/*
	 * public abstract ArrayList<int[]> getDifference(Schedule otherSchedule);
	 * 
	 * public abstract ArrayList<int[]> getRealDistance(Schedule otherSchedule);
	 * 
	 * public abstract ArrayList<int[]> getOneWayDistanceIgnoringD(Schedule
	 * otherSchedule);
	 */
	public abstract Schedule clone();

}

class MTSchedule extends Schedule {
	Event[] events1;
	Event[] events2;
	HappensBefore[][] constraintsMatrix;

	public Schedule clone() {
		HappensBefore[][] copyMatrix = new HappensBefore[events1.length][events2.length];
		for (int i = 0; i < events1.length; i++) {
			for (int j = 0; j < events2.length; j++) {
				copyMatrix[i][j] = constraintsMatrix[i][j];
			}
		}
		return new MTSchedule(events1.clone(), events2.clone(), copyMatrix);
	}

	public MTSchedule(Event[] events1, Event[] events2, HappensBefore[][] matrix) {
		this.events1 = events1;
		this.events2 = events2;
		this.constraintsMatrix = matrix;

	}

	public MTSchedule(ArrayList<MTEvent> events1, ArrayList<MTEvent> events2) {
		this.events1 = new MTEvent[events1.size()];
		this.events2 = new MTEvent[events2.size()];
		events1.toArray(this.events1);
		events2.toArray(this.events2);
		constraintsMatrix = new HappensBefore[events1.size()][events2.size()];
		for (int i = 0; i < events1.size(); i++) {
			for (int j = 0; j < events2.size(); j++) {

				constraintsMatrix[i][j] = HappensBefore.N;
			}
		}
	}

	public boolean isValid() {
		for (int i = 0; i < events1.length; i++) {
			for (int j = 1; j < events2.length; j++) {
				if (constraintsMatrix[i][j].ordinal() < constraintsMatrix[i][j - 1].ordinal())
					return false;
			}
		}
		for (int j = 0; j < events2.length; j++) {
			for (int i = 0; i < events1.length - 1; i++) {
				if (constraintsMatrix[i][j].ordinal() < constraintsMatrix[i + 1][j].ordinal())
					return false;
			}
		}
		return true;
	}

	public ArrayList<int[]> getDifference(Schedule otherSchedule) {
		ArrayList<int[]> differences = new ArrayList<int[]>();
		for (int i = 0; i < events1.length; i++) {
			for (int j = 0; j < events2.length; j++) {
				if (!compareConstraints(this.constraintsMatrix[i][j], ((MTSchedule) otherSchedule).constraintsMatrix[i][j]))
					differences.add(new int[] { i, j });
			}
		}
		return differences;
	}

	public ArrayList<int[]> getRealDistance(Schedule otherSchedule) {
		ArrayList<int[]> differences = new ArrayList<int[]>();
		for (int i = 0; i < events1.length; i++) {
			for (int j = 0; j < events2.length; j++) {
				if (this.constraintsMatrix[i][j] != ((MTSchedule) otherSchedule).constraintsMatrix[i][j])
					differences.add(new int[] { i, j });
			}
		}
		return differences;
	}

	public ArrayList<int[]> getOneWayDistanceIgnoringD(Schedule otherSchedule) {
		ArrayList<int[]> differences = new ArrayList<int[]>();
		for (int i = 0; i < events1.length; i++) {
			for (int j = 0; j < events2.length; j++) {
				if (((MTSchedule) otherSchedule).constraintsMatrix[i][j] != HappensBefore.D
						&& this.constraintsMatrix[i][j] != ((MTSchedule) otherSchedule).constraintsMatrix[i][j])
					differences.add(new int[] { i, j });
			}
		}
		return differences;
	}

	private boolean compareConstraints(HappensBefore c1, HappensBefore c2) {
		return (c1 == HappensBefore.D || c2 == HappensBefore.D || (c1 == c2));
	}

	public String toString() {
		String result = "";
		for (int i = 0; i < events1.length; i++) {
			result += "(" + ((MTEvent) events1[i]).tid + "," + ((MTEvent) events1[i]).line + ") :";
			for (int j = 0; j < events2.length; j++) {
				result += "(" + ((MTEvent) events2[j]).tid + "," + ((MTEvent) events2[j]).line + ")=" + constraintsMatrix[i][j];
			}
			result += "\n";
		}
		return result;
	}
}

class ActorSchedule extends Schedule {

	HashMap<String, Constraint> actorToConstraintMap = new HashMap<String, Constraint>();

	public ActorSchedule(HashMap<String, Constraint> actorToConstraintMap) {
		this.actorToConstraintMap = actorToConstraintMap;
	}

	public ActorSchedule() {

	}

	public boolean isValid() {
		return true;
	}

	public boolean hasTheSameEvents(ActorSchedule otherSchedule) {
		for (String actor : this.actorToConstraintMap.keySet()) {
			Constraint thisConst = (Constraint) this.actorToConstraintMap.get(actor);
			Constraint otherConst = (Constraint) otherSchedule.actorToConstraintMap.get(actor);
			if (otherConst == null || thisConst.events1.size() != otherConst.events1.size()) {
				return false;
			}

			for (Event event : thisConst.events1) {
				if (!otherConst.events1.contains(event)) {
					return false;
				}
			}
		}
		return true;

	}

	public boolean hasTheSameEventsByIgnoringSomeActors(ActorSchedule otherSchedule, ArrayList<String> ignoringActors) {
		for (String actor : this.actorToConstraintMap.keySet()) {
			if (!ignoringActors.contains(actor)) {
				Constraint thisConst = (Constraint) this.actorToConstraintMap.get(actor);
				Constraint otherConst = (Constraint) otherSchedule.actorToConstraintMap.get(actor);
				if (otherConst == null || thisConst.events1.size() != otherConst.events1.size())
					return false;

				for (ActorEvent event : thisConst.events1) {
					if (ignoringActors.contains(event.sender)) {
						boolean findMatch = false;
						for (ActorEvent otherEvent : otherConst.events1) {
							if (otherEvent.receiver.equals(event.receiver) && otherEvent.content.equals(event.content)
									&& otherEvent.seqNum.equals(event.seqNum)) {
								findMatch = true;
								break;
							}
						}
						if (!findMatch)
							return false;
					} else if (!otherConst.events1.contains(event))
						return false;

				}
			}
		}

		return true;

	}

	public boolean hasTheSameSymmetricEvents(ActorSchedule otherSchedule, ArrayList<String> symmetricActors) {
		ArrayList<HashMap<String, String>> maps = createSymmetricMaps(symmetricActors);
		for (HashMap<String, String> map : maps) {
			// ActorSchedule thisCopy = this.clone();
			this.changeActorsInConstraints(map);
			if (this.hasTheSameEvents(otherSchedule)) {
				this.changeActorsInConstraints(map);
				return true;
			}
			this.changeActorsInConstraints(map);
		}
		return false;

	}

	public ArrayList<HashMap<String, String>> createSymmetricMaps(ArrayList<String> symmetricActors) {
		ArrayList<HashMap<String, String>> maps = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < symmetricActors.size(); i++) {
			for (int j = i + 1; j < symmetricActors.size(); j++) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put(symmetricActors.get(i), symmetricActors.get(j));
				map.put(symmetricActors.get(j), symmetricActors.get(i));
				maps.add(map);
			}
		}
		return maps;
	}

	public boolean equals(ActorSchedule otherSchedule) throws Exception {
		if (!this.hasTheSameEvents(otherSchedule))
			return false;
		for (String actor : this.actorToConstraintMap.keySet()) {
			Constraint thisConst = (Constraint) this.actorToConstraintMap.get(actor);
			Constraint otherConst = (Constraint) otherSchedule.actorToConstraintMap.get(actor);
			ArrayList<Event[]> difference = thisConst.getDifference(otherConst);
			if (difference != null && difference.size() > 0) {
				return false;
			}
		}
		return true;
	}

	// public boolean equalsByApplyingSymmetry(ActorSchedule otherSchedule,
	// ArrayList<String> symmetricActors) throws Exception {
	// ActorSchedule thisCopy = this.clone();
	// ActorSchedule otherCopy = otherSchedule.clone();
	//
	// for (String actor : symmetricActors) {
	// thisCopy.actorToConstraintMap.remove(actor);
	// otherCopy.actorToConstraintMap.remove(actor);
	// }
	//
	// HashMap<String, String> actorMap = new HashMap<String, String>();
	// // Object[] ignoringActorsArray = symmetricActors.toArray();
	// for (int i = 0; i < symmetricActors.size() - 1; i++) {
	// for (int j = i + 1; j < symmetricActors.size(); j++) {
	// actorMap.put(symmetricActors.get(i), symmetricActors.get(j));
	// actorMap.put(symmetricActors.get(j), symmetricActors.get(i));
	// thisCopy.changeActorsInConstraints(actorMap);
	// if (thisCopy.equals(otherCopy)){
	// return true;
	// }
	// thisCopy.changeActorsInConstraints(actorMap);
	//
	// actorMap.clear();
	//
	// }
	// }
	//
	// return false;
	//
	// }

	public boolean isMatchedByApplyingSymmetry(ActorSchedule otherSchedule, ArrayList<String> symmetricActors) {
		if (/* this.hasTheSameEvents(otherSchedule) && */this.isMatchedWith(otherSchedule))
			return true;
		ArrayList<HashMap<String, String>> maps = createSymmetricMaps(symmetricActors);
		for (HashMap<String, String> map : maps) {
			// ActorSchedule thisCopy = this.clone();
			this.changeActorsInConstraints(map);
			if (/* this.hasTheSameEvents(otherSchedule) && */this.isMatchedWith(otherSchedule)) {
				this.changeActorsInConstraints(map);
				return true;
			}
			changeActorsInConstraints(map);
		}
		return false;

	}

	public boolean isMatchedWith(ActorSchedule otherSchedule) {
		for (String actor : this.actorToConstraintMap.keySet()) {
			Constraint thisConst = this.actorToConstraintMap.get(actor);
			Constraint otherConst = otherSchedule.actorToConstraintMap.get(actor);
			if (!thisConst.isMatchedWith(otherConst)) {
				return false;
			}
		}

		return true;

	}

	public void changeActorsInConstraints(HashMap<String, String> actorMap) {

		for (String actor : this.actorToConstraintMap.keySet()) {
			this.actorToConstraintMap.get(actor).changeActorsInEvents(actorMap);
		}

		HashMap<String, Constraint> constForActorsInMap = new HashMap<String, Constraint>();
		for (String actor : actorMap.keySet()) {
			if (this.actorToConstraintMap.containsKey(actor)) {
				constForActorsInMap.put(actorMap.get(actor), this.actorToConstraintMap.remove(actor));
			}
		}
		if (!constForActorsInMap.isEmpty())
			this.actorToConstraintMap.putAll(constForActorsInMap);

	}

	public int getNumOfRemovedConstraints() {
		int count = 0;
		for (String actor : this.actorToConstraintMap.keySet()) {
			count += this.actorToConstraintMap.get(actor).getNumOfRemovedConstraints();
		}
		return count;
	}

	public ActorSchedule clone() {
		HashMap<String, Constraint> copyConstraints = new HashMap<String, Constraint>();
		for (String actorRef : actorToConstraintMap.keySet()) {
			copyConstraints.put(actorRef, actorToConstraintMap.get(actorRef).clone());
		}
		return new ActorSchedule(copyConstraints);
	}

	public String toString() {
		String result = "";
		for (String actorRef : actorToConstraintMap.keySet()) {
			result += (actorRef + ":\n" + actorToConstraintMap.get(actorRef).toString());
		}
		return result;
	}
}
