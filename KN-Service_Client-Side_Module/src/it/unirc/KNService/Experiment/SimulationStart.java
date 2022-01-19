package it.unirc.KNService.Experiment;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONObject;

public class SimulationStart {

	static int nTokensPerRing=SimulationClientSide.nTokensPerRing;
	static int complete=0;
	public static void main(String[] args) throws Exception {
		try {
			
			int[]idList= new int[nTokensPerRing*SimulationClientSide.nRings];
			int current=0;
			for(int j=0; j<nTokensPerRing; j++) {	
				for(int i=0; i<SimulationClientSide.nRings;i++) {	
					int idGen=0;

					boolean ok=false;
					while(!ok) {
						ok=true;
						idGen=ThreadLocalRandom.current().nextInt(1, SimulationClientSide.nUsers+1)+i*SimulationClientSide.nUsers;
						for(int k=0; k<current; k++) {
							if(idList[k]==idGen) {
								ok=false;
								break;
							}
						}

					}

					idList[current]=idGen;
					current++;
				}
			}

			for(int i=0; i<idList.length;i++) {	
				int id=idList[i];
				final WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(SimulationClientSide.URL),8000);

		
				clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {

					public void handleMessage(String message) {


						try {
							if(!message.equals("")) {
								JSONObject obj = new JSONObject(message);
								String type=obj.get("type").toString();

								if(type.equals("auth")) {

									obj = new JSONObject();
									obj.put("type", "token");
									obj.put("nextUser", String.valueOf(id));												
									obj.put("tokenContent", "FirstToken");
									String messageNew = obj.toString();									
									clientEndPoint.sendMessage(messageNew);
									complete++;
									
								}

								if(type.equals("token")) {
									System.out.println("Error");
								}
							}
						}catch(Exception e) {
							e.printStackTrace();
						}

					}

				});
			}
		
		while(true) {
	
		}




	} catch (URISyntaxException ex) {
		System.err.println("URISyntaxException exception: " + ex.getMessage());
	}
}
}