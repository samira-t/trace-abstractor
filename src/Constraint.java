import java.util.ArrayList;
import java.util.HashMap;

public class Constraint {

	ArrayList<ActorEvent> events1;
	ArrayList<ActorEvent> events2;

	HappensBefore[][] HBRel;

	public Constraint(ArrayList<ActorEvent> events1, ArrayList<ActorEvent> events2) {
		this.events1 = events1;
		this.events2 = events2;

		HBRel = new HappensBefore[events1.size()][events2.size()];
		for (int i = 0; i < events1.size(); i++) {
			for (int j = 0; j < events2.size(); j++) {
				HBRel[i][j] = HappensBefore.N;
			}
		}

	}

	public ArrayList<Event[]> getDifference(Constraint otherConstrint) throws Exception {
		ArrayList<Event[]> differences = new ArrayList<Event[]>();
		for (int i = 0; i < events1.size(); i++) {
			Event event1 = events1.get(i);
			for (int j = i + 1; j < events2.size(); j++) {
				Event event2 = events2.get(j);
				HappensBefore otherValue = otherConstrint.getHBRelValue(event1, event2);
				if (otherValue != null && this.HBRel[i][j] != otherValue)
					differences.add(new Event[] { events1.get(i), events2.get(j) });
				else if (otherValue == null) {
					// if (oi == -1)
					System.err.println(events1.get(i).toString() + "not exits");
					// else
					System.err.println(events2.get(j).toString() + "not exits");
					throw new Exception("Event not exists");
					// return null;
				}
			}
		}
		return differences;
	}

	public void changeActorsInEvents(HashMap<String, String> actorMap) {
		for (ActorEvent event : events1) {
			String sender = event.sender;
			String receiver = event.receiver;
			if (actorMap.containsKey(sender))
				event.sender = actorMap.get(sender);
			if (actorMap.containsKey(receiver))
				event.receiver = actorMap.get(receiver);

		}
		// for (ActorEvent event : events2) {
		// String sender = event.sender;
		// String receiver = event.receiver;
		// if (actorMap.containsKey(sender))
		// event.sender = actorMap.get(sender);
		// if (actorMap.containsKey(receiver))
		// event.receiver = actorMap.get(receiver);
		//
		// }
	}

	public boolean isMatchedWith(Constraint otherConstrint) {
		for (int i = 0; i < events1.size(); i++) {
			Event event1 = events1.get(i);
			for (int j = i + 1; j < events2.size(); j++) {
				Event event2 = events2.get(j);
				HappensBefore otherValue = otherConstrint.getHBRelValue(event1, event2);
				if (otherValue == null || (otherValue != HappensBefore.D && this.HBRel[i][j] != otherValue))
					return false;
			}
		}
		return true;
	}

	// public ArrayList<Event[]> getOneWayNonMatchDifference(Constraint
	// otherConstrint) {
	// ArrayList<Event[]> differences = new ArrayList<Event[]>();
	// for (int i = 0; i < events1.size(); i++) {
	// Event event1 = events1.get(i);
	// for (int j = 0; j < events2.size(); j++) {
	// Event event2 = events2.get(j);
	// HappensBefore otherValue = otherConstrint.getHBRelValue(event1, event2);
	// if (otherValue != HappensBefore.D && this.HBRel[i][j] != otherValue)
	// differences.add(new Event[] { events1.get(i), events2.get(j) });
	// }
	// }
	// return differences;
	// }

	private boolean match(HappensBefore c1, HappensBefore c2) {
		return (c1 == HappensBefore.D || c2 == HappensBefore.D || (c1 == c2));
	}

	@Override
	public Constraint clone() {
		ArrayList<ActorEvent> newEvents1 = (ArrayList<ActorEvent>) events1.clone();
		ArrayList<ActorEvent> newEvents2 = (ArrayList<ActorEvent>) events2.clone();
		Constraint newConstraint = new Constraint(newEvents1, newEvents2);
		for (int i = 0; i < events1.size(); i++) {
			for (int j = 0; j < events2.size(); j++) {
				newConstraint.HBRel[i][j] = HBRel[i][j];
			}
		}
		return newConstraint;

	}

	public ArrayList<Event[]> getDifferenceIgnoringD(Constraint otherConstrint) throws Exception {
		ArrayList<Event[]> differences = new ArrayList<Event[]>();
		for (int i = 0; i < events1.size(); i++) {
			Event event1 = events1.get(i);
			for (int j = i + 1; j < events2.size(); j++) {
				Event event2 = events2.get(j);
				HappensBefore otherValue = otherConstrint.getHBRelValue(event1, event2);
				if (otherValue != null) {
					if (!match(this.HBRel[i][j], otherValue))
						differences.add(new Event[] { events1.get(i), events2.get(j) });
				} else {
					// if (oi == -1)
					System.err.println(events1.get(i).toString() + "not exits");
					// else
					System.err.println(events2.get(j).toString() + "not exits");
					throw new Exception("Event not exists");

					// return null;
				}
			}
		}
		return differences;
	}

	public HappensBefore getHBRelValue(Event event1, Event event2) {
		int oi = events1.indexOf(event1);
		int oj = events2.indexOf(event2);
		if (oi != -1 && oj != -1)
			return HBRel[oi][oj];
		else
			return null;

	}

	public String toStringWithDetails() {
		String shortResult = "";
		String detailResult = "";
		for (int i = 0; i < events1.size(); i++) {
			ActorEvent event1 = (ActorEvent) events1.get(i);
			detailResult += "(" + event1.sender + "," + event1.content + ") :";
			boolean nextAdded = false;
			for (int j = 0; j < events2.size(); j++) {
				ActorEvent event2 = (ActorEvent) events2.get(j);
				if (i < j) {
					if (!nextAdded) {
						if (HBRel[i][j] == HappensBefore.Y)
							shortResult += "->" + event2.toString();
						else if (HBRel[i][j] == HappensBefore.D)
							shortResult += "," + event2.toString();
						nextAdded = true;
					}

					detailResult += "(" + event2.sender + "," + event2.content + ")=" + HBRel[i][j];
				} else
					detailResult += "()";
			}
			detailResult += "\n";
		}
		return shortResult + "\n" + detailResult;
	}

	public String toString() {
		String shortResult = "";
		for (int i = 0; i < events1.size(); i++) {
			ActorEvent event1 = (ActorEvent) events1.get(i);
			if (i == 0)
				shortResult += event1.toString();
			if (i < events2.size() - 1) {
				ActorEvent event2 = (ActorEvent) events2.get(i + 1);
				if (HBRel[i][i + 1] == HappensBefore.Y) {
					// if (!events1.get(i).causallyRelatedTo(events2.get(i +
					// 1)))
					shortResult += "->" + event2.toString();
					// else
					// Logger.logInfo("casually related" + events1.get(i)
					// + " and " + events2.get(i + 1));
				}

				else if (HBRel[i][i + 1] == HappensBefore.D)
					shortResult += "," + event2.toString();
			}
		}
		return shortResult + "\n";
	}
}
