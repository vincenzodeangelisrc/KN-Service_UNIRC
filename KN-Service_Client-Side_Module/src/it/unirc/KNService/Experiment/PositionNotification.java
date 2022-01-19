package it.unirc.KNService.Experiment;

public class PositionNotification {
	Handshake h;
	Message send;
	Message response;
	public PositionNotification(Handshake h, Message send, Message response) {
		super();
		this.h = h;
		this.send = send;
		this.response = response;
	}
	public Handshake getH() {
		return h;
	}
	public void setH(Handshake h) {
		this.h = h;
	}
	public Message getSend() {
		return send;
	}
	public void setSend(Message send) {
		this.send = send;
	}
	public Message getResponse() {
		return response;
	}
	public void setResponse(Message response) {
		this.response = response;
	}
	@Override
	public String toString() {
		return "PositionNotification [h=" + h + ", send=" + send + ", response=" + response + "]";
	}
	
}
