package it.unirc.KNService.Experiment;

public class EphemeralConfirmation {
private Message confirm;
private Handshake h;
public EphemeralConfirmation(Message initiatorConfirm, Handshake h) {
	super();
	this.confirm = initiatorConfirm;
	this.h=h;
}
public Message getConfirm() {
	return confirm;
}
public void setConfirm(Message initiatorConfirm) {
	this.confirm = initiatorConfirm;
}

public Handshake getH() {
	return h;
}
public void setH(Handshake h) {
	this.h = h;
}


	
}
