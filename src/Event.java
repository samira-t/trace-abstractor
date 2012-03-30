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
		return (((MTEvent) obj).tid.equals(tid) && ((MTEvent) obj).line.equals(line));
	}

}

class ActorEvent extends Event {
	String sender;
	String receiver;
	String content;
	String fullContent;
	// String realSenderSeqNum;
	String seqNum = "";
	String[] msgVc;
	String[] recVc;

	public ActorEvent(String sender, String receiver, String content, String[] msgVc) {
		this.sender = sender;
		this.receiver = receiver;
		this.fullContent = content;
		int senderId = Integer.parseInt(sender);
		// this.realSenderSeqNum = vc[senderId];
		this.content = content;
		this.content = (content.indexOf("(") >= 0) ? content.substring(0, content.indexOf("(")) : content;
		if (content.contains("!(")) {
			String signature = content.split(",")[1];
			signature = signature.indexOf("(") >= 0 ? signature.substring(0, signature.indexOf("(")) : signature;
			signature = signature.indexOf("(") >= 0 ? signature.substring(0, signature.indexOf(")") - 1) : signature;

			this.content = "!" + signature.replace(")", "");
		}
		this.msgVc = msgVc;
		if (msgVc != null)
			this.seqNum = msgVc[senderId];

	}

	public ActorEvent(String sender, String receiver, String content, String[] msgVc, String[] recVc) {
		try {
			this.sender = sender;
			this.receiver = receiver;
			this.fullContent = content;
			int senderId = Integer.parseInt(sender);
			// this.realSenderSeqNum = vc[senderId];
			this.content = content;
			this.content = (content.indexOf("(") >= 0) ? content.substring(0, content.indexOf("(")) : content;
			if (content.contains("!(")) {
				String signature = content.split(",")[1];
				signature = signature.indexOf("(") >= 0 ? signature.substring(0, signature.indexOf("(")) : signature;
				signature = signature.indexOf("(") >= 0 ? signature.substring(0, signature.indexOf(")") - 1) : signature;

				this.content = "!" + signature.replace(")", "");
			}
			this.msgVc = msgVc;
			if (msgVc != null)
				this.seqNum = msgVc[senderId];
			this.recVc = recVc;
		} catch (Exception ex) {
			System.err.print(this);
		}

	}

	public boolean causallyRelatedTo(ActorEvent otherEvent) {
		if (msgVc != null && otherEvent.recVc != null) {
			for (int i = 0; i < msgVc.length; i++) {
				if (Integer.parseInt(otherEvent.recVc[i]) > Integer.parseInt(msgVc[i]))
					return false;
			}
			return true;
		} else
			return false;
	}

	public int hashCode() {
		return (this.sender.hashCode() + receiver.hashCode() + content.hashCode() + seqNum.hashCode());
	}

	public boolean equals(Object obj) {
		ActorEvent other = (ActorEvent) obj;
		try {
			return (other.sender.equals(sender) && other.receiver.equals(receiver) && other.content.equals(content) && other.seqNum.equals(seqNum));
		} catch (Exception ex) {
			System.err.println(this.toString() + obj.toString());
			return false;
		}
	}

	public String toString() {
		String shortContent = (content.equals(fullContent)) ? content : content + "(_)";
		return ("(" + sender + "," + receiver + "," + shortContent + "," + seqNum + ")");
	}

	public ActorEvent clone() {
		try {
			if (this.recVc != null) {
				return new ActorEvent(this.sender, this.receiver, this.fullContent, this.msgVc, this.recVc);
			} else
				return new ActorEvent(this.sender, this.receiver, this.fullContent, this.msgVc);
		} catch (Exception ex) {
			return null;
		}
	}
}
