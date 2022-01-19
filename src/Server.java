

import java.io.File;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import  javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
@ServerEndpoint(value = "/KN-ServiceServer")
public class Server {
	static ConcurrentHashMap<String, Session> tokenToSession=new ConcurrentHashMap<String,Session>();
	static ConcurrentHashMap<String, String> idToToken=new ConcurrentHashMap<String,String>();
	static ConcurrentHashMap<String, String> tokenToId=new ConcurrentHashMap<String,String>();

	static int count=0;
	static long currentTime=0;

	static ConcurrentHashMap<String, String> ephLink=new ConcurrentHashMap<String,String>();
	static ConcurrentHashMap<String, String[]> linkedEph=new ConcurrentHashMap<String,String[]>();
	static ConcurrentHashMap<String, String> hashedPosition=new ConcurrentHashMap<String,String>();


	static int contSucc=0;
	@OnOpen
	public void onOpen(Session session) {

		System.out.println("A client want to open the connection");

		//generate a random token for the client
		byte[] array = new byte[100]; 
		new Random().nextBytes(array);
		String token = Base64.encodeBase64String(array);

		JSONObject obj = new JSONObject();

		try {
			obj.put("type", "auth");
			obj.put("tokenUser", token);
		}catch(Exception e) {
			e.printStackTrace();
		}


		tokenToSession.put(token, session);  	
		try {
			Thread.sleep(50);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		session.getAsyncRemote().sendText(obj.toString());

	}
	@OnMessage
	public void onMessage(String message, Session session) {



		try {
			JSONObject obj = new JSONObject(message);
			String type=obj.get("type").toString();


			if(type.equals("auth")) {
				String id=obj.get("globalID").toString();
				String token=obj.get("tokenUser").toString();	

				//obj.get(AuthenticationCredential)  			
				//CHECK CREDENTIALS (Social Network dependent feature: It checks the credential of the users)

				idToToken.put(id, token);
				tokenToId.put(token, id);
			}

			if(type.equals("token")) {

				String externalTokenContent1="";
				String externalTokenContent2="";
				String idMessage1="";
				String idMessage2="";				
				String proxy1="";
				String proxy2="";


				if(obj.has("dataSN")) {
					//data for the social network provider (obtained from primitive P1)					
					JSONObject obj1=new JSONObject(obj.get("dataSN").toString());
					String encryptedOnTheFlyKey=obj1.get("encryptedOnTheFlyKey").toString();
					String encryptedMessage=obj1.get("message").toString();
					byte[] onTheFlyKey=Base64.decodeBase64(encryptedOnTheFlyKey);
					Cipher c= Cipher.getInstance("AES/ECB/PKCS5Padding");
					SecretKeySpec OnTheFlyKey = new SecretKeySpec(onTheFlyKey, "AES");
					c.init(Cipher.DECRYPT_MODE,  OnTheFlyKey);
					String decr=new String(c.doFinal(Base64.decodeBase64(encryptedMessage)), "UTF-8");


					obj1=new JSONObject(decr);
					String typeData=obj1.get("type").toString();
					if(typeData.equals("handshake_request_to_server")) {
						
						String Q=obj1.get("Q").toString();		
						String handShakerequestServer2=ephLink.get(Q);

						if(handShakerequestServer2!=null) {
							ephLink.remove(Q);
							JSONObject obj2=new JSONObject(handShakerequestServer2);
							String eph1=obj1.get("Eph").toString();
							String eph2=obj2.get("Eph").toString();
							String PK1=obj1.get("PKClient").toString();
							String PK2=obj2.get("PKClient").toString();
							proxy1=obj1.get("idProxy").toString();
							proxy2=obj2.get("idProxy").toString();
							byte[] array1=Base64.decodeBase64(eph1);
							byte[] array2=Base64.decodeBase64(eph2);
							byte[]result=new byte[array1.length];
							for(int i=0; i<array1.length;i++) {
								result[i]=(byte) (array1[i]^array2[i]);
							}

							String xor=Base64.encodeBase64String(result);
							MessageDigest sha = MessageDigest.getInstance("SHA-1");
							String hashEph=Base64.encodeBase64String(sha.digest(xor.getBytes()));



							obj1=new JSONObject();
							obj1.put("type", "handshake_response_server");
							obj1.put("hash_xor", hashEph);
							obj1.put("Q", Q);
							obj1.put("Eph", String.valueOf(eph1));

							byte[] random = new byte[256]; 
							new Random().nextBytes(random);
							idMessage1=Base64.encodeBase64String(random);		
							obj1.put("idMessage", idMessage1);


							String response1=obj1.toString();

							Cipher c1= Cipher.getInstance("AES/ECB/PKCS5Padding");
							byte[] random1=new byte[256];
							new Random().nextBytes(random1);
							byte[] key1 = sha.digest(random1);
							key1 = Arrays.copyOf(key1, 16); 
							SecretKeySpec OnTheFlyKey1 = new SecretKeySpec(key1, "AES");
							c1.init(Cipher.ENCRYPT_MODE,  OnTheFlyKey1);
							String response1Encr=Base64.encodeBase64String(c1.doFinal(response1.getBytes("UTF-8")));
							obj1 = new JSONObject();
							obj1.put("message", response1Encr);

							c1= Cipher.getInstance("RSA");
							c1.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decodeBase64(PK1))));
							String encryptedOnTheFlyKey1=Base64.encodeBase64String(c1.doFinal(key1));	
							obj1.put("encryptedOnTheFlyKey", encryptedOnTheFlyKey1);

							String data1=obj1.toString();
							obj1 = new JSONObject();
							obj1.put("flag", "fill");
							obj1.put("data", data1);
							obj1.put("destination","randomStuff");


							externalTokenContent1=obj1.toString();


							obj2=new JSONObject();
							obj2.put("type", "handshake_response_server");
							obj2.put("hash_xor", hashEph);
							obj2.put("Q", Q);
							obj2.put("Eph", String.valueOf(eph2));

							random = new byte[256]; 
							new Random().nextBytes(random);
							idMessage2=Base64.encodeBase64String(random);		
							obj2.put("idMessage", idMessage2);

							String response2=obj2.toString();

							Cipher c2= Cipher.getInstance("AES/ECB/PKCS5Padding");
							byte[] random2=new byte[256];
							new Random().nextBytes(random2);
							byte[] key2 = sha.digest(random2);
							key2 = Arrays.copyOf(key2, 16); 
							SecretKeySpec OnTheFlyKey2 = new SecretKeySpec(key2, "AES");
							c2.init(Cipher.ENCRYPT_MODE,  OnTheFlyKey2);
							String response2Encr=Base64.encodeBase64String(c2.doFinal(response2.getBytes("UTF-8")));
							obj2 = new JSONObject();
							obj2.put("message", response2Encr);

							c2= Cipher.getInstance("RSA");
							c2.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decodeBase64(PK2))));
							String encryptedOnTheFlyKey2=Base64.encodeBase64String(c2.doFinal(key2));	
							obj2.put("encryptedOnTheFlyKey", encryptedOnTheFlyKey2);

							String data2=obj2.toString();
							obj2 = new JSONObject();
							obj2.put("flag", "fill");
							obj2.put("data", data2);
							obj2.put("destination","randomStuff");


							externalTokenContent2=obj2.toString();


							linkedEph.put(eph1, new String[] {eph2,hashEph,Q});
							linkedEph.put(eph2, new String[] {eph1,hashEph,Q});

						}

						else {
							ephLink.put(Q,decr);
						}


					}

					if(typeData.equals("position_notification_request")) {


						String eph1=obj1.get("Eph").toString();			
						String eph2=linkedEph.get(eph1)[0];
						String positionNotification2=hashedPosition.get(eph2);



						if(positionNotification2!=null) {

							hashedPosition.remove(eph2);
							JSONObject obj2=new JSONObject(positionNotification2);



							String C11=obj1.get("C1").toString();
							String C12=obj1.get("C2").toString();

							String C21=obj2.get("C1").toString();
							String C22=obj2.get("C2").toString();


							List<String> commonC= new LinkedList<String>();
							if(C11.equals(C21)|| C11.equals(C22)) {
								commonC.add(C11);
							}
							if(C12.equals(C21)|| C12.equals(C22)) {
								commonC.add(C12);
							}


							if(!commonC.isEmpty()) {
								contSucc++;

								String PK1=obj1.get("PKClient").toString();
								String PK2=obj2.get("PKClient").toString();
								proxy1=obj1.get("idProxy").toString();
								proxy2=obj2.get("idProxy").toString();



								String hashEph=linkedEph.get(eph1)[1];
								String hashEph2=linkedEph.get(eph2)[1];
								if(!hashEph.equals(hashEph2)) {
									System.out.println("Error");
								}


								obj1=new JSONObject();
								obj1.put("type", "position_notification_response");
								obj1.put("hash_xor", hashEph);
								String C="";
								for(String cn: commonC) {
									if(C.equals("")) {
										C=cn;
									}
									else {C=C+";"+cn;}
								}


								obj1.put("C", C);
								obj1.put("Eph", eph1);
								obj1.put("Q", linkedEph.get(eph1)[2]);

								byte[] random = new byte[256]; 
								new Random().nextBytes(random);
								idMessage1=Base64.encodeBase64String(random);		
								obj1.put("idMessage", idMessage1);


								String response1=obj1.toString();

								Cipher c1= Cipher.getInstance("AES/ECB/PKCS5Padding");
								byte[] random1=new byte[256];
								new Random().nextBytes(random1);
								MessageDigest sha = MessageDigest.getInstance("SHA-1");
								byte[] key1 = sha.digest(random1);
								key1 = Arrays.copyOf(key1, 16); 
								SecretKeySpec OnTheFlyKey1 = new SecretKeySpec(key1, "AES");
								c1.init(Cipher.ENCRYPT_MODE,  OnTheFlyKey1);
								String response1Encr=Base64.encodeBase64String(c1.doFinal(response1.getBytes("UTF-8")));
								obj1 = new JSONObject();
								obj1.put("message", response1Encr);

								c1= Cipher.getInstance("RSA");
								c1.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decodeBase64(PK1))));
								String encryptedOnTheFlyKey1=Base64.encodeBase64String(c1.doFinal(key1));	
								obj1.put("encryptedOnTheFlyKey", encryptedOnTheFlyKey1);

								String data1=obj1.toString();
								obj1 = new JSONObject();
								obj1.put("flag", "fill");
								obj1.put("data", data1);
								obj1.put("destination","randomStuff");


								externalTokenContent1=obj1.toString();


								obj2=new JSONObject();
								obj2.put("type", "position_notification_response");
								obj2.put("hash_xor", hashEph);
								obj2.put("C", C11+"||"+C12+"||"+C21+"||"+C22);
								obj2.put("Eph", eph2);
								obj2.put("Q", linkedEph.get(eph2)[2]);

								random = new byte[256]; 
								new Random().nextBytes(random);
								idMessage2=Base64.encodeBase64String(random);		
								obj2.put("idMessage", idMessage2);

								String response2=obj2.toString();


								Cipher c2= Cipher.getInstance("AES/ECB/PKCS5Padding");
								byte[] random2=new byte[256];
								new Random().nextBytes(random2);
								byte[] key2 = sha.digest(random2);
								key2 = Arrays.copyOf(key2, 16); 
								SecretKeySpec OnTheFlyKey2 = new SecretKeySpec(key2, "AES");
								c2.init(Cipher.ENCRYPT_MODE,  OnTheFlyKey2);
								String response2Encr=Base64.encodeBase64String(c2.doFinal(response2.getBytes("UTF-8")));
								obj2 = new JSONObject();
								obj2.put("message", response2Encr);

								c2= Cipher.getInstance("RSA");
								c2.init(Cipher.ENCRYPT_MODE, KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.decodeBase64(PK2))));
								String encryptedOnTheFlyKey2=Base64.encodeBase64String(c2.doFinal(key2));	
								obj2.put("encryptedOnTheFlyKey", encryptedOnTheFlyKey2);

								String data2=obj2.toString();
								obj2 = new JSONObject();
								obj2.put("flag", "fill");
								obj2.put("data", data2);
								obj2.put("destination","randomStuff");


								externalTokenContent2=obj2.toString();

							}

							else {
								externalTokenContent1="";
								externalTokenContent2="";
							}

						}

						else {
							hashedPosition.put(eph1,decr);
						}

					}

				}


				String nextId=obj.get("nextUser").toString();
				String tokenContent=obj.get("tokenContent").toString();
				String tokenNext=idToToken.get(nextId);
				Session s=tokenToSession.get(tokenNext);



				obj = new JSONObject();
				obj.put("type", "token");
				obj.put("tokenContent", tokenContent);

				if(!externalTokenContent1.equals("")) {
					obj.put("externalToken1", externalTokenContent1);
					obj.put("proxy1", proxy1);

					//only for benchmark
					obj.put("idMessage1", idMessage1);

				}

				if(!externalTokenContent2.equals("")) {
					obj.put("externalToken2", externalTokenContent2);
					obj.put("proxy2", proxy2);

					//only for benchmark
					obj.put("idMessage2", idMessage2);
				}



				boolean send=true;
				while(send) {

					try {

						Future<Void> f=s.getAsyncRemote().sendText(obj.toString());
						while(!f.isDone()) {Thread.sleep(10);}
						send=false;

					}
					catch(Exception e) {
						System.out.println(nextId);					
						Thread.sleep(100);
					}
				}

				count++;



				if(count%100000==0) {

					long time=System.currentTimeMillis();
					System.out.println(time-currentTime);
					System.out.println("Successfull Handshake="+contSucc);
					currentTime=time;


					boolean found=false;
					for(String key: tokenToSession.keySet() ) {
						Session sess=tokenToSession.get(key);
						if(!sess.isOpen()) {
							found=true;
						}

					}
					if(!found) {
						System.out.println("All the session are open");
					}
					else {
						System.out.println("Error:There is a session not open");
					}

					System.out.println("");
				}





			}

			//return "";



		}catch(Exception e) {

			System.out.println("Error");
			try {

				PrintWriter pw = new PrintWriter(new File("FILE_TO_PRINT_ERROR"));
				e.printStackTrace(pw);
				pw.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}


			e.printStackTrace();

		}	

	
	}
	@OnClose
	public void onClose(Session session) {

	}
	@OnError
	public void onError(Throwable exception, Session session) {
		
	}
}