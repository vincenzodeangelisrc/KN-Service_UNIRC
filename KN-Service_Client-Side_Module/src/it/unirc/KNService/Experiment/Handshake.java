package it.unirc.KNService.Experiment;

public class Handshake {

	Message request;
	Message response;
	
	Message requestServerInitiator;
	Message requestServer2;
	
	Message responseServerInitiator;
	Message responseServer2;
	
	int seqNumber;
	String xor;
	
	String ephInitiator;
	String ephDest;
	
	
	
	
	public String getEphInitiator() {
		return ephInitiator;
	}
	public void setEphInitiator(String ephInitiator) {
		this.ephInitiator = ephInitiator;
	}
	public String getEphDest() {
		return ephDest;
	}
	public void setEphDest(String ephDest) {
		this.ephDest = ephDest;
	}
	public String getXor() {
		return xor;
	}
	public void setXor(String xor) {
		this.xor = xor;
	}
	public Handshake(Message request, Message response, Message requestServerInitiator, Message requestServer2,
			Message responseServerInitiator, Message responseServer2, int sq) {
		super();
		this.request = request;
		this.response = response;
		this.requestServerInitiator = requestServerInitiator;
		this.requestServer2 = requestServer2;
		this.responseServerInitiator = responseServerInitiator;
		this.responseServer2 = responseServer2;
		this.seqNumber=sq;
	}
	public Message getRequest() {
		return request;
	}
	public void setRequest(Message request) {
		this.request = request;
	}
	public Message getResponse() {
		return response;
	}
	public void setResponse(Message response) {
		this.response = response;
	}
	public Message getRequestServerInitiator() {
		return requestServerInitiator;
	}
	public void setRequestServerInitiator(Message requestServerInitiator) {
		this.requestServerInitiator = requestServerInitiator;
	}
	public Message getRequestServer2() {
		return requestServer2;
	}
	public void setRequestServer2(Message requestServer2) {
		this.requestServer2 = requestServer2;
	}
	public Message getResponseServerInitiator() {
		return responseServerInitiator;
	}
	public void setResponseServerInitiator(Message responseServerInitiator) {
		this.responseServerInitiator = responseServerInitiator;
	}
	public Message getResponseServer2() {
		return responseServer2;
	}
	public void setResponseServer2(Message responseServer2) {
		this.responseServer2 = responseServer2;
	}
	
	
	public int getSeqNumber() {
		return seqNumber;
	}
	public void setSeqNumber(int seqNumber) {
		this.seqNumber = seqNumber;
	}
	@Override
	public String toString() {
		return "HandShake [request=" + request + ", response=" + response + ", requestServerInitiator="
				+ requestServerInitiator + ", requestServer2=" + requestServer2 + ", responseServerInitiator="
				+ responseServerInitiator + ", responseServer2=" + responseServer2 + ", seqNumber=" + seqNumber + "]";
	}
	
	
	
	
	
}
