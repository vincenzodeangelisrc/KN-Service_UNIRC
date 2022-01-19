package it.unirc.KNService.Experiment;

public class Message {

	String idMessage;
	String content;
	int sender;
	int dest;
	long timeSending;
	long timeArrival;
	
	
	
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getIdMessage() {
		return idMessage;
	}
	public void setIdMessage(String idMessage) {
		this.idMessage = idMessage;
	}
	public int getSender() {
		return sender;
	}
	public void setSender(int sender) {
		this.sender = sender;
	}
	public int getDest() {
		return dest;
	}
	public void setDest(int dest) {
		this.dest = dest;
	}
	public long getTimeSending() {
		return timeSending;
	}
	public void setTimeSending(long timeSending) {
		this.timeSending = timeSending;
	}
	public long getTimeArrival() {
		return timeArrival;
	}
	public void setTimeArrival(long timeArrival) {
		this.timeArrival = timeArrival;
	}
	@Override
	public String toString() {
		return "Message [idMessage=" + idMessage + ", sender=" + sender + ", dest=" + dest + ", timeSending="
				+ timeSending + ", timeArrival=" + timeArrival + "]";
	}
	public Message(String idMessage, String content, int sender, int dest, long timeSending, long timeArrival) {
		super();
		this.idMessage = idMessage;
		this.sender = sender;
		this.content=content;
		this.dest = dest;
		this.timeSending = timeSending;
		this.timeArrival = timeArrival;
	}

	
	
}