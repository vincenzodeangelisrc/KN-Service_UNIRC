package it.unirc.KNService.Experiment;
import java.net.URI;


import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;



/**
 * ChatServer Client
 *
 * @author Jiji_Sasidharan
 */
@ClientEndpoint
public class WebsocketClientEndpoint {

	Session userSession = null;
	private MessageHandler messageHandler;

	private int id;



	public WebsocketClientEndpoint(URI endpointURI, int id) {
		try {
			this.id=id;
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			container.connectToServer(this, endpointURI);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Callback hook for Connection open events.
	 *
	 * @param userSession the userSession which is opened.
	 */
	@OnOpen
	public void onOpen(Session userSession) {
		//System.out.println("Client with id="+id+" opening websocket");
		this.userSession = userSession;
	}

	/**
	 * Callback hook for Connection close events.
	 *
	 * @param userSession the userSession which is getting closed.
	 * @param reason the reason for connection close
	 */
	@OnClose
	public void onClose(Session userSession, CloseReason reason) {
		//System.out.println("Client with id="+id+" closing websocket");
		SimulationClientSide.idsClosed.add(String.valueOf(id));
		this.userSession = null;
	}

	/**
	 * Callback hook for Message Events. This method will be invoked when a client send a message.
	 *
	 * @param message The text message
	 */
	@OnMessage
	public void onMessage(String message) {
		
		if (this.messageHandler != null) {
			this.messageHandler.handleMessage(message);
		}
		else {
			System.out.println(message);
		}
		
	}

	/**
	 * register message handler
	 *
	 * @param msgHandler
	 */
	public void addMessageHandler(MessageHandler msgHandler) {
		this.messageHandler = msgHandler;
	}

	/**
	 * Send a message.
	 *
	 * @param message
	 */
	public void sendMessage(String message) {
	
//		if(id!=8000) {
//		SingleClient.isBusy[id-1]=true;
//		}
		this.userSession.getAsyncRemote().sendText(message);
		
//		while(!f.isDone()) {
//			try {
//				Thread.sleep(10);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		if(id!=8000) {
//		SingleClient.isBusy[id-1]=false;
//		}
		
	}
	

	
	

	
	

	/**
	 * Message handler.
	 *
	 * @author Jiji_Sasidharan
	 */
	public static interface MessageHandler {

		public void handleMessage(String message);
	}
}