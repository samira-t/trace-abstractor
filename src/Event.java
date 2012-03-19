public abstract class Event {

}

class MTEvent extends Event {
	String tid;
	String line;

	public MTEvent(String tid, String line) {
		this.tid = tid;
		this.line = line;
	}

	public int hashCode() {
		return (this.tid.hashCode() + line.hashCode());
	}

	public boolean equals(Object obj) {
		return (((MTEvent) obj).tid.equals(tid) && ((MTEvent) obj).line
				.equals(line));
	}

}

class ActorEvent extends Event {
	String sender;
	String receiver;
	String content;
	String fullContent;
	// String realSenderSeqNum;
	String seqNum = "";
	String[] vc;

	public ActorEvent(String sender, String receiver, String content,
			String[] vc) {
		this.sender = sender;
		this.receiver = receiver;
		this.fullContent = content;
		int senderId = Integer.parseInt(sender);
		// this.realSenderSeqNum = vc[senderId];
		this.content = content;
		this.content = (content.indexOf("(") >= 0) ? content.substring(0,
				content.indexOf("(")) : content;
		if (content.contains("!(")) {
			String signature = content.split(",")[1];
			signature = signature.indexOf("(") >= 0 ? signature.substring(0,
					signature.indexOf("(")) : signature;
			signature = signature.indexOf("(") >= 0 ? signature.substring(0,
					signature.indexOf(")") - 1) : signature;

			this.content = "!" + signature.replace(")", "");
		}
		this.seqNum = vc[senderId];
		this.vc = vc;
	}

	public boolean causallyRelatedTo(ActorEvent otherEvent) {
		for (int i = 0; i < vc.length; i++) {
			if (otherEvent.vc[i].compareTo(this.vc[i]) > 0)
				return false;
		}
		return true;
	}

	public int hashCode() {
		return (this.sender.hashCode() + receiver.hashCode()
				+ content.hashCode() + seqNum.hashCode());
	}

	public boolean equals(Object obj) {
		ActorEvent other = (ActorEvent) obj;
		return (other.sender.equals(sender) && other.receiver.equals(receiver)
				&& other.content.equals(content) && other.seqNum.equals(seqNum));
	}

	public String toString() {
		String shortContent = (content.equals(fullContent)) ? content : content
				+ "(_)";
		return ("(" + sender + "," + receiver + "," + shortContent + ","
				+ seqNum + ")");
	}

	public ActorEvent clone() {
		return new ActorEvent(this.sender, this.receiver, this.fullContent,
				this.vc);
	}
}
