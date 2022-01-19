package it.unirc.KNService.Experiment;


import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

import org.json.JSONObject;



public class SimulationClientSide {
	public static String URL="ws://localhost:8080/WebSocketServer/KN-ServiceServer"; 

	//Simulation parameters
	static int nUsers=40;
	static int nRings=2;
	static int nTokensPerRing=10;	
	static int duration=999999999; //
	static int numberOfSenders=1;
	static int numberOfHandshakesPerSender=20;
	static int sqrtCells=5;
	static double sigma=0.25;


	
	static long startTime=0;
	static int numberOfCells=sqrtCells*sqrtCells;
	static int nTot=nUsers*nRings;	
	static int totalHandshakes=numberOfSenders*numberOfHandshakesPerSender;
	static long sendingTime[][]= new long[nTot][numberOfHandshakesPerSender];
	static int handshakesToPerform[][]= new int[nTot][numberOfHandshakesPerSender+1];
	static ConcurrentHashMap<String, PositionNotification>[] posNotification=new ConcurrentHashMap [nTot];
	static ConcurrentHashMap<String, EphemeralConfirmation>[] confirmation=new ConcurrentHashMap [nTot];
	static Cell[][] locationUsers= new Cell[nTot][2];
	static boolean started=false;
	static int currentToken=0;
	static int performedHandshake=0;
	static int completedHandshake=0;
	static int successfullHandshake=0;
	static int[] proxyNodes=new int[nRings];
	static int i=1;


	//EncryptionParameters
	static KeyPairGenerator keyGen;
	static KeyPair[] pair=new KeyPair[nTot];
	static PrivateKey[] privateKeys=new PrivateKey[nTot];
	static PublicKey[] publicKeys=new PublicKey[nTot];
	static Cipher ciphersRSA[]=new Cipher[nTot];
	static SecretKeySpec[] AESKeys=new SecretKeySpec[nTot];
	static Cipher ciphersAES[]=new Cipher[nTot];




	static ConcurrentLinkedQueue<String>[] proxyQueues= new ConcurrentLinkedQueue[nRings];	
	public static Queue<String> idsClosed=new LinkedList<String>();



	static ConcurrentLinkedQueue<Message>[] handshakeResponse= new ConcurrentLinkedQueue[nTot];
	static ConcurrentLinkedQueue<Message>[] posNot= new ConcurrentLinkedQueue[nTot];
	static ConcurrentLinkedQueue<Message>[] handshakeRequestToServer= new ConcurrentLinkedQueue[nTot];
	static ConcurrentLinkedQueue<Message>[] ephemeralConfirmationToSend= new ConcurrentLinkedQueue[nTot];
	static ConcurrentHashMap<String, Message> messages= new ConcurrentHashMap<String, Message>();
	static ConcurrentHashMap<String,Handshake> handshakes=new ConcurrentHashMap<String, Handshake>();



	static int contExt=0;

	public static void main(String[] args) throws Exception {

		//initialize the rings
		initialize();

		try {
			while(i<nTot+1) {	

				final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(URL),i);

				clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
					String tokenUser="";   
					int id=i;  
					int nextId=id%nUsers+1+((id-1)/nUsers)*nUsers;



					public synchronized void handleMessage(String message) {

						try {

							JSONObject obj = new JSONObject(message);
							String type=obj.get("type").toString();
							if(type.equals("auth")) {
								tokenUser=obj.get("tokenUser").toString();
								obj = new JSONObject();
								obj.put("type", "auth");
								obj.put("tokenUser", tokenUser);
								obj.put("globalID", String.valueOf(id));
								String messageNew = obj.toString();
								clientEndPoint.sendMessage(messageNew);
							}

							if(type.equals("token")) {

								String tokenContent=obj.get("tokenContent").toString();

								if(obj.has("externalToken1")) {

									int proxy1= Integer.parseInt(obj.get("proxy1").toString());
									String extTokenContent=obj.get("externalToken1").toString();
									ciphersAES[id-1].init(Cipher.ENCRYPT_MODE,  AESKeys[proxy1-1]);
									extTokenContent=Base64.encodeBase64String(ciphersAES[id-1].doFinal(extTokenContent.getBytes("UTF-8")));
									proxyQueues[(proxy1-1)/nUsers].add(extTokenContent);

									//only for benchmark
									String idMessage1=obj.get("idMessage1").toString();
									Message m1= new Message(idMessage1,null,-1,0,System.currentTimeMillis(),0);
									messages.put(idMessage1, m1);

								}

								if(obj.has("externalToken2")) {
									int proxy2= Integer.parseInt(obj.get("proxy2").toString());
									String extTokenContent=obj.get("externalToken2").toString();
									ciphersAES[id-1].init(Cipher.ENCRYPT_MODE,  AESKeys[proxy2-1]);
									extTokenContent=Base64.encodeBase64String(ciphersAES[id-1].doFinal(extTokenContent.getBytes("UTF-8")));
									proxyQueues[(proxy2-1)/nUsers].add(extTokenContent);

									//only for benchmark
									String idMessage2=obj.get("idMessage2").toString();
									Message m1= new Message(idMessage2,null,-1,0,System.currentTimeMillis(),0);
									messages.put(idMessage2, m1);
								}



								if(tokenContent.equals("FirstToken")) { 	

									currentToken++;

									JSONObject obj1 = new JSONObject();
									obj1.put("flag", "empty");
									obj1.put("data", "randomStuff");
									obj1.put("destination", "randomStuff");
									tokenContent=obj1.toString();
									ciphersAES[id-1].init(Cipher.ENCRYPT_MODE,  AESKeys[id-1]);
									tokenContent=Base64.encodeBase64String(ciphersAES[id-1].doFinal(tokenContent.getBytes("UTF-8")));
								}

								ciphersAES[id-1].init(Cipher.DECRYPT_MODE, AESKeys[id-1]);
								tokenContent=new String(ciphersAES[id-1].doFinal(Base64.decodeBase64(tokenContent)), "UTF-8");



								obj = new JSONObject(tokenContent);
								String flag=obj.get("flag").toString();
								String data=obj.get("data").toString();
								String destination=obj.get("destination").toString();


								//only for benchmarck
								String idMessage="";
								if(obj.has("idMessage")) {
									idMessage=obj.get("idMessage").toString();									
								}

								int proxy=proxyNodes[(id-1)/nUsers];

								boolean toSN=false;
								if(id==proxy) { 
									//this node is proxy node
									if(flag.equals("fill")) {
										//the received token is fill
										try {

											obj=new JSONObject(data);
											String encryptedOnTheFlyKey=obj.get("encryptedOnTheFlyKey").toString();
											String encryptedMessage=obj.get("message").toString();
											ciphersRSA[id-1].init(Cipher.DECRYPT_MODE, privateKeys[id-1]);
											byte[] onTheFlyKey=ciphersRSA[id-1].doFinal(Base64.decodeBase64(encryptedOnTheFlyKey));
											Cipher c= Cipher.getInstance("AES/ECB/PKCS5Padding");
											SecretKeySpec OnTheFlyKey = new SecretKeySpec(onTheFlyKey, "AES");
											c.init(Cipher.DECRYPT_MODE,  OnTheFlyKey);
											String decr=new String(c.doFinal(Base64.decodeBase64(encryptedMessage)), "UTF-8");
											if(decr.equals("toEmpty")) {

												if(!proxyQueues[(id-1)/nUsers].isEmpty()) {
													ciphersAES[id-1].init(Cipher.DECRYPT_MODE, AESKeys[id-1]);
													tokenContent=new String(ciphersAES[id-1].doFinal(Base64.decodeBase64(proxyQueues[(id-1)/nUsers].poll())), "UTF-8");

												}
												else {
													obj = new JSONObject();
													obj.put("flag", "empty");
													if(Math.random()<sigma) {
														obj.put("flag", "reserved");
													}

													obj.put("data", "randomStuff");
													obj.put("destination","randomStuff");
													tokenContent=obj.toString();
												}


											}

											else {

												throw new Exception();
											}


										}
										catch(Exception e) {

											try {
												ciphersRSA[id-1].init(Cipher.DECRYPT_MODE, privateKeys[id-1]);

												String destDecr=new String(ciphersRSA[id-1].doFinal(Base64.decodeBase64(destination)), "UTF-8");

												int dest=Integer.parseInt(destDecr);

												if(dest!=-1) {


													obj = new JSONObject();
													obj.put("flag", "fill");
													obj.put("data", data);
													obj.put("destination","randomStuff");

													//toRemoveOnlyForDebug
													//obj.put("idMessage", "Token coming from proxy:"+proxy+" with destination proxy:"+dest+". The original message was:"+messages.get(idMessage));


													String extTokenContent=obj.toString();


													int proxyDest=proxyNodes[(dest-1)/nUsers];

													ciphersAES[id-1].init(Cipher.ENCRYPT_MODE,  AESKeys[proxyDest-1]);
													extTokenContent=Base64.encodeBase64String(ciphersAES[id-1].doFinal(extTokenContent.getBytes("UTF-8")));
													proxyQueues[(proxyDest-1)/nUsers].add(extTokenContent);


													//
													obj = new JSONObject();
													obj.put("type", "token");
													obj.put("tokenUser", tokenUser);
													obj.put("nextUser", String.valueOf(proxyDest));
													obj.put("tokenContent", extTokenContent);
													//String messageNew = obj.toString();
													

												}
												else {		
													toSN=true;
												}

												obj = new JSONObject();
												obj.put("flag", "empty");
												if(Math.random()<sigma) {
													obj.put("flag", "reserved");
												}
												obj.put("data", "randomStuff");
												obj.put("destination","randomStuff");
												tokenContent=obj.toString();
												if(!proxyQueues[(id-1)/nUsers].isEmpty()) {
													ciphersAES[id-1].init(Cipher.DECRYPT_MODE, AESKeys[id-1]);
													tokenContent=new String(ciphersAES[id-1].doFinal(Base64.decodeBase64(proxyQueues[(id-1)/nUsers].poll())), "UTF-8");
												}

											}catch(Exception e1) {


												System.out.println("Error Proxy"+id);
												System.out.println("Id Message"+idMessage);

												e1.printStackTrace();
												return;
											}

										}
									}
									else {
										//the token is empty
										obj = new JSONObject();
										obj.put("flag", "empty");
										if(Math.random()<sigma) {
											obj.put("flag", "reserved");
										}
										obj.put("data", "randomStuff");
										obj.put("destination","randomStuff");
										tokenContent=obj.toString();


										if(!proxyQueues[(id-1)/nUsers].isEmpty()) {
											ciphersAES[id-1].init(Cipher.DECRYPT_MODE, AESKeys[id-1]);
											tokenContent=new String(ciphersAES[id-1].doFinal(Base64.decodeBase64(proxyQueues[(id-1)/nUsers].poll())), "UTF-8");
										}

									}


								}

								else { //the node is not proxy node

									if(flag.equals("fill")||flag.equals("reserved")) {
										try {

											obj=new JSONObject(data);		
											String encryptedOnTheFlyKey=obj.get("encryptedOnTheFlyKey").toString();
											String encryptedMessage=obj.get("message").toString();


											ciphersRSA[id-1].init(Cipher.DECRYPT_MODE, privateKeys[id-1]);
											byte[] onTheFlyKey=ciphersRSA[id-1].doFinal(Base64.decodeBase64(encryptedOnTheFlyKey));
											Cipher c= Cipher.getInstance("AES/ECB/PKCS5Padding");
											SecretKeySpec OnTheFlyKey = new SecretKeySpec(onTheFlyKey, "AES");
											c.init(Cipher.DECRYPT_MODE,  OnTheFlyKey);


											String decr=new String(c.doFinal(Base64.decodeBase64(encryptedMessage)), "UTF-8");
											obj = new JSONObject(decr);


											Message m=messages.get(obj.get("idMessage").toString());	

											m.setTimeArrival(System.currentTimeMillis());


											String typeMex=obj.get("type").toString();
											if(typeMex.equals("handshake_request")) {

										
												JSONObject obj1=new JSONObject();
												obj1.put("type", "handshake_response");
												obj1.put("Q", obj.get("Q").toString());
												obj1.put("sender", String.valueOf(id));
												obj1.put("destination", obj.get("sender").toString());

												byte[] random = new byte[256]; 
												new Random().nextBytes(random);
												idMessage=Base64.encodeBase64String(random);		
												obj1.put("idMessage", idMessage);

												Message m1=new Message(idMessage, obj1.toString(),id, Integer.parseInt(obj1.get("destination").toString()), 0, 0);	


												handshakeResponse[id-1].add(m1);		

											}

											if(typeMex.equals("handshake_response")) {
												

												JSONObject obj1=new JSONObject();
												obj1.put("type", "handshake_request_to_server");
												obj1.put("Q", obj.get("Q").toString());	
												obj1.put("PKClient", Base64.encodeBase64String(publicKeys[id-1].getEncoded()));			
												Handshake h=handshakes.get( obj.get("Q").toString());
												obj1.put("Eph", h.getEphInitiator());
												obj1.put("idProxy", proxy);	



												byte[] random = new byte[256]; 
												new Random().nextBytes(random);
												idMessage=Base64.encodeBase64String(random);	
												obj1.put("idMessage", idMessage);
												Message m1=new Message(idMessage, obj1.toString(),id, -1, 0, 0);	


												handshakeRequestToServer[id-1].add(m1);	

											}


											if(typeMex.equals("handshake_response_server")) {
												
												m.setContent(decr);
												m.setDest(id);

												String Q=obj.get("Q").toString();
												String xor=obj.get("hash_xor").toString();
												Handshake h=handshakes.get(Q);
												if(h.getRequest().getSender()==id) {
													h.setResponseServerInitiator(m);
													h.setXor(xor);
												}
												else {
													h.setResponseServer2(m);
												}




												String eph=obj.get("Eph").toString();


												JSONObject obj1=new JSONObject();
												obj1.put("type", "position_notification_request");
												obj1.put("PKClient", Base64.encodeBase64String(publicKeys[id-1].getEncoded()));
												obj1.put("Eph", eph);
												MessageDigest sha = MessageDigest.getInstance("SHA-1");
												obj1.put("C1", Base64.encodeBase64String(sha.digest(locationUsers[id-1][0].toString().getBytes())));
												obj1.put("C2", Base64.encodeBase64String(sha.digest(locationUsers[id-1][1].toString().getBytes())));
												obj1.put("idProxy", proxy);	



												byte[] random = new byte[256]; 
												new Random().nextBytes(random);
												idMessage=Base64.encodeBase64String(random);	
												obj1.put("idMessage", idMessage);
												Message m1=new Message(idMessage, obj1.toString(),id, -1, 0, 0);	


												posNot[id-1].add(m1);																		
												PositionNotification p= new PositionNotification(h,m1,null);
												posNotification[id-1].put(Q, p);


											}

											if(typeMex.equals("position_notification_response")) {
												
												m.setContent(decr);
												m.setDest(id);


												String Q=obj.get("Q").toString();
												Handshake h=handshakes.get(Q);

												posNotification[id-1].get(Q).setResponse(m);
												String xor=obj.get("hash_xor").toString();
												if(h.getRequest().getSender()==id && !h.getXor().equals(xor)) {													
													System.out.println("Error in the reception of the XOR. Received="+xor+" Corrected="+h.getXor());
												}

												String eph=obj.get("Eph").toString();
												JSONObject obj1=new JSONObject();
												obj1.put("type", "ephemeral_confirmation");
												obj1.put("Eph", eph);
												obj1.put("Q", Q);
												String[] centroids=obj.get("C").toString().split(";");
												MessageDigest sha = MessageDigest.getInstance("SHA-1");
												String C="";
												for(String cn: centroids) {
													if(C.equals("")) {
														C=Base64.encodeBase64String(sha.digest(cn.getBytes()));
													}
													else {C=C+";"+Base64.encodeBase64String(sha.digest(cn.getBytes()));}
												}																							
												obj1.put("C", C);


												byte[] random = new byte[256]; 
												new Random().nextBytes(random);
												idMessage=Base64.encodeBase64String(random);	
												obj1.put("idMessage", idMessage);
												int dest=0;
												if(id==h.getRequest().getSender()) {
													dest=h.getRequest().getDest();
												}
												else {
													dest=h.getRequest().getSender();
												}


												Message m1=new Message(idMessage, obj1.toString(),id,dest, 0, 0);	
												ephemeralConfirmationToSend[id-1].add(m1);		
												EphemeralConfirmation e= new EphemeralConfirmation(m,h);
												confirmation[id-1].put(Q, e);


											}

											if(typeMex.equals("ephemeral_confirmation")) {
												

												String Q=obj.get("Q").toString();
												Handshake h=handshakes.get(Q);

												String ephOther=obj.get("Eph").toString();	
												String MyEph="";
												if(h.getRequest().getSender()==id) {
													MyEph=h.getEphInitiator();
												}
												else {
													MyEph=h.getEphDest();
												}


												byte[] array1=Base64.decodeBase64(MyEph);
												byte[] array2=Base64.decodeBase64(ephOther);
												byte[]result=new byte[array1.length];
												for(int i=0; i<array1.length;i++) {
													result[i]=(byte) (array1[i]^array2[i]);
												}

												String xor=Base64.encodeBase64String(result);
												MessageDigest sha = MessageDigest.getInstance("SHA-1");
												String xorH=Base64.encodeBase64String(sha.digest(xor.getBytes()));





												if(!h.getXor().equals(xorH)) {
													System.out.println("Error in the reception of the XOR");
												}



												if(h.getRequest().getSender()==id) {
													completedHandshake++;	
													if(handshakesToPerform[id-1][0]>0) {
														sendingTime[id-1][numberOfHandshakesPerSender-handshakesToPerform[id-1][0]]=System.currentTimeMillis()+20000;
														
													}
													
													
												}

											}


											c= Cipher.getInstance("AES/ECB/PKCS5Padding");
											MessageDigest sha = MessageDigest.getInstance("SHA-1");
											byte[] random=new byte[256];
											new Random().nextBytes(random);
											byte[] key = sha.digest(random);
											key = Arrays.copyOf(key, 16); 
											OnTheFlyKey = new SecretKeySpec(key, "AES");
											c.init(Cipher.ENCRYPT_MODE,  OnTheFlyKey);
											String mexToSend=Base64.encodeBase64String(c.doFinal("toEmpty".getBytes("UTF-8")));
											obj = new JSONObject();
											obj.put("message", mexToSend);
											ciphersRSA[id-1].init(Cipher.ENCRYPT_MODE, publicKeys[proxy-1]);
											encryptedOnTheFlyKey=Base64.encodeBase64String(ciphersRSA[id-1].doFinal(key));
											obj.put("encryptedOnTheFlyKey", encryptedOnTheFlyKey);


											data=obj.toString();
											obj = new JSONObject();
											obj.put("flag", "fill");
											obj.put("data", data);
											obj.put("destination","randomStuff");

											
											tokenContent=obj.toString();

										}
										catch(Exception e) { 
											//		Token to forward
										}
									}
									else { //the received token is empty
										boolean send=false;
										long currentTime=System.currentTimeMillis();
										if(handshakesToPerform[id-1][0]>0 && currentTime>=sendingTime[id-1][numberOfHandshakesPerSender-handshakesToPerform[id-1][0]] && started) {
											send=true;
										}


										if(send || !handshakeResponse[id-1].isEmpty() || !handshakeRequestToServer[id-1].isEmpty() || !posNot[id-1].isEmpty() || !ephemeralConfirmationToSend[id-1].isEmpty()) {

											String mexToSendClear=""; int idDest=0; Message m=null;
											if(!ephemeralConfirmationToSend[id-1].isEmpty()) {
												Message m1=ephemeralConfirmationToSend[id-1].poll();
												m1.setTimeSending(System.currentTimeMillis());
												idDest=m1.getDest();
												m=m1;
												mexToSendClear=m.getContent();																																												
											}
											else {

												if(!posNot[id-1].isEmpty()) {
													Message m1=posNot[id-1].poll();
													m1.setTimeSending(System.currentTimeMillis());
													idDest=-1;
													m=m1;
													mexToSendClear=m.getContent();																																												
												}
												else {	


													if(!handshakeRequestToServer[id-1].isEmpty()) {
														Message m1=handshakeRequestToServer[id-1].poll();
														m1.setTimeSending(System.currentTimeMillis());
														idDest=-1;
														m=m1;
														mexToSendClear=m.getContent();
														JSONObject obj1=new JSONObject(mexToSendClear);																									
														Handshake h=handshakes.get(obj1.get("Q").toString());
														if(h.getRequest().getSender()==id) {
															h.setRequestServerInitiator(m);
														}
														else {
															h.setRequestServer2(m);
														}
													}

													else {
														if(!handshakeResponse[id-1].isEmpty()) {
															Message m1=handshakeResponse[id-1].poll();
															m1.setTimeSending(System.currentTimeMillis());
															idDest=m1.getDest();
															m=m1;
															mexToSendClear=m.getContent();
															JSONObject obj1=new JSONObject(mexToSendClear);																									
															Handshake h=handshakes.get(obj1.get("Q").toString());
															h.setResponse(m);




															obj=new JSONObject();
															obj.put("type", "handshake_request_to_server");
															obj.put("Q", obj1.get("Q").toString());
															obj.put("PKClient", Base64.encodeBase64String(publicKeys[id-1].getEncoded()));

															byte[] random = new byte[256]; 
															new Random().nextBytes(random);
															h.setEphDest(Base64.encodeBase64String(random));
															obj.put("Eph", h.getEphDest());
															obj.put("idProxy", proxy);		


															random = new byte[256]; 
															new Random().nextBytes(random);
															idMessage=Base64.encodeBase64String(random);														
															obj.put("idMessage", idMessage);
															Message m2=new Message(idMessage, obj.toString(),id, -1, 0,0);	
															handshakeRequestToServer[id-1].add(m2);	
														}



														else {
															if(send) {

																byte[] array = new byte[256]; 
																new Random().nextBytes(array);

																obj=new JSONObject();
																obj.put("type", "handshake_request");
																obj.put("Q", Base64.encodeBase64String(array));
																obj.put("sender", String.valueOf(id));

																idDest=handshakesToPerform[id-1][numberOfHandshakesPerSender-handshakesToPerform[id-1][0]+1];

																obj.put("destination", String.valueOf(idDest));
																byte[] random = new byte[256]; 
																new Random().nextBytes(random);
																idMessage=Base64.encodeBase64String(random);		
																obj.put("idMessage", idMessage);

																mexToSendClear=obj.toString();

																m=new Message(idMessage, mexToSendClear, id, idDest, System.currentTimeMillis(), 0);																									
																Handshake h= new Handshake(m,null,null,null,null,null, (numberOfHandshakesPerSender-handshakesToPerform[id-1][0]));

																random = new byte[256]; 
																new Random().nextBytes(random);
																h.setEphInitiator(Base64.encodeBase64String(random));



																handshakes.put(obj.get("Q").toString(), h);
																handshakesToPerform[id-1][0]--;																																																						
																performedHandshake++;
															}

														}

													}
												}

											}

											Cipher c= Cipher.getInstance("AES/ECB/PKCS5Padding");
											MessageDigest sha = MessageDigest.getInstance("SHA-1");
											byte[] random=new byte[256];
											new Random().nextBytes(random);
											byte[] key = sha.digest(random);
											key = Arrays.copyOf(key, 16); 
											SecretKeySpec OnTheFlyKey = new SecretKeySpec(key, "AES");
											c.init(Cipher.ENCRYPT_MODE,  OnTheFlyKey);
											String mexToSendEncr=Base64.encodeBase64String(c.doFinal(mexToSendClear.getBytes("UTF-8")));



											obj = new JSONObject();
											obj.put("message", mexToSendEncr);

											String encryptedOnTheFlyKey="";

											if(idDest!=-1) {
												ciphersRSA[id-1].init(Cipher.ENCRYPT_MODE, publicKeys[idDest-1]);
												encryptedOnTheFlyKey=Base64.encodeBase64String(ciphersRSA[id-1].doFinal(key));}
											else {
												encryptedOnTheFlyKey=Base64.encodeBase64String(key);												
											}



											obj.put("encryptedOnTheFlyKey", encryptedOnTheFlyKey);										
											data=obj.toString();

											obj = new JSONObject();
											obj.put("flag", "fill");

											obj.put("data", data);
											ciphersRSA[id-1].init(Cipher.ENCRYPT_MODE, publicKeys[proxy-1]);

											if(idDest!=-1) {
												String proxyDest=String.valueOf(proxyNodes[(idDest-1)/nUsers]);		

												String destEncr=Base64.encodeBase64String(ciphersRSA[id-1].doFinal(proxyDest.getBytes("UTF-8")));

												obj.put("destination",destEncr);

											}
											else {

												//only for benchmark
												obj.put("idMessage", m.getIdMessage());
												obj.put("destination",Base64.encodeBase64String(ciphersRSA[id-1].doFinal(String.valueOf("-1").getBytes("UTF-8"))));
											}

											tokenContent=obj.toString();
											messages.put(m.getIdMessage(), m);
										}


									}

								}



								ciphersAES[id-1].init(Cipher.ENCRYPT_MODE,  AESKeys[nextId-1]);
								tokenContent=Base64.encodeBase64String(ciphersAES[id-1].doFinal(tokenContent.getBytes("UTF-8")));


								obj = new JSONObject();
								obj.put("type", "token");
								obj.put("nextUser", String.valueOf(nextId));
								obj.put("tokenContent", tokenContent);
								if(toSN) {												
									obj.put("dataSN", data);

									//only for benchmark
									messages.get(idMessage).setTimeArrival(System.currentTimeMillis());

								}

								String messageNew = obj.toString();								
								clientEndPoint.sendMessage(messageNew);


							}

						}
						catch(Exception e) {
							System.out.println("Error Sender:"+id);
							e.printStackTrace();
						}


					}


				});

				i++;
				Thread.sleep(100);
			}

			System.out.println("All clients are authenticated");

		


			while (true){
				startTime();
				System.out.println("Currently performed:"+performedHandshake);
				System.out.println("Currently completed:"+completedHandshake);
				Thread.sleep(10000);
				printResult(); 
			}





		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	public static void initialize() throws Exception {
		//Keys generation
		keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(1024);
		for(int i=0; i<nTot;i++) {
			pair[i] = keyGen.generateKeyPair();
			privateKeys[i] = pair[i].getPrivate();
			publicKeys[i] = pair[i].getPublic();
			ciphersRSA[i]=Cipher.getInstance("RSA");
			ciphersAES[i] = Cipher.getInstance("AES/ECB/PKCS5Padding");


			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			byte[] random=new byte[256];
			new Random().nextBytes(random);
			byte[] key = sha.digest(random);
			key = Arrays.copyOf(key, 16); 
			AESKeys[i] = new SecretKeySpec(key, "AES");


		}




		//Proxy nodes selection 
		System.out.println("Proxy nodes:");
		for(int i=0; i<nRings; i++) {
			proxyNodes[i]=ThreadLocalRandom.current().nextInt(1, nUsers+1)+i*nUsers;
			System.out.println("Proxy["+i+"]="+proxyNodes[i]);
			proxyQueues[i]=new ConcurrentLinkedQueue<String>();
		}
		
	
		for(int i=0; i<nTot;i++) {

			handshakeResponse[i]=new ConcurrentLinkedQueue<Message>();
			handshakeRequestToServer[i]=new ConcurrentLinkedQueue<Message>();
			posNot[i]=new ConcurrentLinkedQueue<Message>();
			ephemeralConfirmationToSend[i]=new ConcurrentLinkedQueue<Message>();
			posNotification[i]=new ConcurrentHashMap<String, PositionNotification>();
			confirmation[i]=new ConcurrentHashMap<String, EphemeralConfirmation>();
			byte[] random=new byte[100];
			new Random().nextBytes(random);

		}


		//Sender selection
		for(int i=0; i<numberOfSenders; i++) {
			boolean idOk=false;
			int id=0;
			while(!idOk) {
				id=ThreadLocalRandom.current().nextInt(1, nTot+1);
				if(sendingTime[id-1][0]==0) {
					idOk=true;
				}
				for(int j=0; j<proxyNodes.length;j++) {
					if(proxyNodes[j]==id) {
						idOk=false;
					}
				}


			}

			long[] sendingTimeForUser= new long[numberOfHandshakesPerSender];
			for(int j=0; j<numberOfHandshakesPerSender;j++) {
			
				sendingTimeForUser[j]=(duration/numberOfHandshakesPerSender)*j;


			}
			Arrays.sort(sendingTimeForUser);	
			for(int j=0; j<numberOfHandshakesPerSender;j++) {
				sendingTime[id-1][j]=sendingTimeForUser[j]+1;
			}

			handshakesToPerform[id-1][0]=numberOfHandshakesPerSender;
			
			
			
			
		}


		//Set location for users
		Cell[] cells= new Cell[numberOfCells*2];
		for(int i=0; i<numberOfCells;i++) {
			cells[i]=new Cell(i,ThreadLocalRandom.current().nextInt(99999999), false);
			cells[i+numberOfCells]=new Cell(i,ThreadLocalRandom.current().nextInt(99999999), true);
		}

		for(int i=0; i<nTot;i++) {
			Cell c=cells[ThreadLocalRandom.current().nextInt(0,numberOfCells)];
			locationUsers[i][0]=c;

			LinkedList<Cell> possibleShifted= new LinkedList<Cell>();
			Cell s1=cells[c.getNumber()+numberOfCells];
			possibleShifted.add(s1);

			int row=s1.getNumber()/sqrtCells;
			int column=s1.getNumber()%sqrtCells;

			if(row-1>=0) {
				int pos=(row-1)*sqrtCells+column;
				possibleShifted.add(cells[pos+numberOfCells]);
			}

			if(row-1>=0 && column+1<sqrtCells) {
				int pos=(row-1)*sqrtCells+(column+1);
				possibleShifted.add(cells[pos+numberOfCells]);
			}

			if(column+1<sqrtCells) {
				int pos=row*sqrtCells+(column+1);
				possibleShifted.add(cells[pos+numberOfCells]);
			}
			locationUsers[i][1]=possibleShifted.get(ThreadLocalRandom.current().nextInt(0,possibleShifted.size()));

			//Only for experiment...to obtain only successfull test (worst case in terms of performance)
			locationUsers[i][0]=cells[0];
			locationUsers[i][1]=cells[0];
		}

		for(int i=0; i<nTot; i++) {
			if(handshakesToPerform[i][0]==numberOfHandshakesPerSender) {
				for(int j=0; j<numberOfHandshakesPerSender;j++) {
					int idDest=ThreadLocalRandom.current().nextInt(1, nTot+1);
					while((idDest-1)/nUsers==i/nUsers || proxyNodes[(idDest-1)/nUsers]==idDest) {
						idDest=ThreadLocalRandom.current().nextInt(1, nTot+1);
					}
					handshakesToPerform[i][j+1]=idDest;
				}
				int k=ThreadLocalRandom.current().nextInt(1, numberOfHandshakesPerSender+1);

				List<Integer> adj= new LinkedList<Integer>();
				for(int j=0; j<nTot; j++) {
					if(i/nUsers!=j/nUsers && (j+1)!=proxyNodes[j/nUsers]&&(locationUsers[i][0].equals(locationUsers[j][0])||locationUsers[i][0].equals(locationUsers[j][1])||locationUsers[i][1].equals(locationUsers[j][0])||locationUsers[i][1].equals(locationUsers[j][1]))) {
						adj.add(j+1);
					}
				}
				if(adj.size()>0) {
					handshakesToPerform[i][k]=adj.get(ThreadLocalRandom.current().nextInt(0, adj.size()));
				}

				for(int j=0; j<numberOfHandshakesPerSender;j++) {

					int otherUser=handshakesToPerform[i][j+1];
					if((locationUsers[i][0].equals(locationUsers[otherUser-1][0])||locationUsers[i][0].equals(locationUsers[otherUser-1][1])||locationUsers[i][1].equals(locationUsers[otherUser-1][0])||locationUsers[i][1].equals(locationUsers[otherUser-1][1]))){
						successfullHandshake++;
					}
				}


			}




		}
		System.out.println("Successfull Handshakes:"+successfullHandshake);

	}

	public static double[] meanSD (Double[] values) {
		double res[]= new double[2];

		double sum = 0.0, standardDeviation = 0.0;
		int length = values.length;

		for(double num : values) {
			sum += num;
		}

		res[0]=sum/length;

		for(double num: values) {
			standardDeviation += Math.pow(num - res[0], 2);
		}

		res[1]=Math.sqrt(standardDeviation/length);
		return res;
	}
	
	public static void printResult() throws Exception {
		if(performedHandshake==totalHandshakes && completedHandshake==successfullHandshake) {
			System.out.println("Completed. Wait for finalization");
			Thread.sleep(10000);

			boolean ok=false;
			LinkedList<Handshake> hand= null;
			while(!ok) {
				ok=true;
				hand=new LinkedList<Handshake> (handshakes.values());

				for (Handshake h: hand) {
					if(h.getResponseServerInitiator()==null || h.getResponseServer2()==null || h.getResponseServerInitiator().getTimeArrival()==0 || h.getResponseServer2().getTimeArrival()==0) {
						ok=false;
						break;
					}

				}
				if(!ok) {
					System.out.println(hand);
					Thread.sleep(10000);}
			}

			Double[] TimesHandShake= new Double[hand.size()];	
			Double[][] waitingTimeUsers= new Double[nTot][numberOfHandshakesPerSender];	
			int i=0;

			for (Handshake h: hand) {
				long max=h.getResponseServerInitiator().getTimeArrival();
				if(max<h.getResponseServer2().getTimeArrival()) {
					max=h.getResponseServer2().getTimeArrival();
				}
				TimesHandShake[i]=(double)(max-h.getRequest().getTimeSending());

				int idInitiator=h.getRequest().getSender();
				int sqNumber=h.getSeqNumber();

				waitingTimeUsers[idInitiator-1][sqNumber]=(double)(max-sendingTime[idInitiator-1][sqNumber]);

				i++;

			}



			
			double[] resHandShake=meanSD(TimesHandShake);
			System.out.println("Mean HS:"+resHandShake[0]);
			System.out.println("StandardDeviation HS:"+resHandShake[1]);

			int cont=0;
			Double[]wait= new Double[numberOfSenders*numberOfHandshakesPerSender];
			for(int j=0; j<nTot; j++) {
				for(int k=0; k<numberOfHandshakesPerSender; k++) {
					if(waitingTimeUsers[j][k]!=null) {
						wait[cont]=waitingTimeUsers[j][k];
						cont++;
					}
				}



			}


			double[] resWaitingTime=meanSD(wait);
			System.out.println("MeanWaitingTime:"+resWaitingTime[0]);
			System.out.println("SDWaitingTime:"+resWaitingTime[1]);



			//CompletedTestSuccessfull


			ArrayList<Double> timesTest= new ArrayList<Double>();	
			ArrayList<Double> WaitingtimesTest= new ArrayList<Double>();	



			for(int u=0; u<nTot;u++) {
				LinkedList<EphemeralConfirmation> list=new LinkedList<EphemeralConfirmation> (confirmation[u].values());
				for(EphemeralConfirmation e: list) {		

					if(u==e.getH().getRequest().getSender()-1) {										
						timesTest.add((double)(e.getConfirm().getTimeArrival()-e.getH().getRequest().getTimeSending()));	
						WaitingtimesTest.add((double)(e.getConfirm().getTimeArrival()-sendingTime[u][e.getH().getSeqNumber()]));	

					}		
				}
			}
			System.out.println("");

			double[] TestTime=meanSD(timesTest.toArray(new Double[timesTest.size()]));
			double[] WaitingTestTime=meanSD(WaitingtimesTest.toArray(new Double[WaitingtimesTest.size()]));
			System.out.println("Mean testTime:"+TestTime[0]);
			System.out.println("SD   testTime:"+TestTime[1]);
			System.out.println("Mean WaitingTestTime:"+WaitingTestTime[0]);
			System.out.println("SD   WaitingTestTime:"+WaitingTestTime[1]);





		}
	}










	public static void startTime() {
		if(currentToken>0 && !started) {
			//Starting time after 60 seconds
			startTime=System.currentTimeMillis()+60000;
			for(int i=0; i<nTot;i++) {
				for(int j=0; j<numberOfHandshakesPerSender;j++) {
					if(sendingTime[i][j]!=0) {
						sendingTime[i][j]=sendingTime[i][j]+startTime;
					}
				}

			}
			System.out.println("Start Simulation");
			started=true;
		}
	}

}