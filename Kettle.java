// Uses the information here: http://www.awe.com/mark/blog/20140223.html
// to decode responses, and send commands to the iKettle.
// Jacob Millward 28 Dec 2014
package com.jacobmillward.kettlecontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Kettle {
	
	private Socket sock;
	private KettleListener listener;
	private Thread listenerThread;
	private OutputStream outputStream;
	private InputStream inputStream;
	private String hostname;
	private final int port = 2000;
	public String ipPrefix="192.168.1.";
	
	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public Kettle() {
		this("192.168.1.");
	}
	public Kettle(String networkPrefix) {
		this.ipPrefix = networkPrefix;
		sock = scan();
		if(sock != null) {
			this.hostname = sock.getInetAddress().getHostAddress();
			try {
				inputStream = sock.getInputStream();
				outputStream = sock.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			listener = new KettleListener(inputStream);
			listenerThread = new Thread(listener);
			listenerThread.start();
		}
		else System.out.println("No kettle found");
	}
	
	public Kettle(InetAddress address){
		//Store hostname and port
		this.hostname = address.getHostAddress();
		
		try {
			sock = new Socket(hostname, port); //Attempt to create a socket
			outputStream = sock.getOutputStream();
			inputStream = sock.getInputStream();
		}
		catch (IOException e) {
			System.out.println("Could not create socket");
			e.printStackTrace();
		}
		listener = new KettleListener(inputStream);
		listenerThread = new Thread(listener);
		listenerThread.start();
	}
	
	public Socket scan() {
		for (int i = 1; i < 254; i++){
			System.out.println(ipPrefix+i);
			try {
			    Socket mySocket = new Socket();
			    SocketAddress address = new InetSocketAddress(ipPrefix+i, port);
			    mySocket.setSoTimeout(2000);
			    mySocket.connect(address, 500);   
			    
			    BufferedReader in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
			    send(mySocket.getOutputStream(), "HELLOKETTLE\n");
			    try {
			        String response = in.readLine();
			        if (response.startsWith("HELLOAPP")) {
			            	return mySocket;
			            }
			        }
		        catch (SocketTimeoutException e) {
		        }
			}
			catch (IOException e) {
			}
		}
		return null;
	}
	
	public void sendCommand(KettleCommand command) {
		String message = "set sys output 0x";
		
		switch (command) {
		case BTN_OFF:
			message += "0";
			break;
		case BTN_ON:
			message += "4";
			break;
		case BTN_100C:
			message += "80";
			break;
		case BTN_95C:
			message += "2";
			break;
		case BTN_80C:
			message += "4000";
			break;
		case BTN_65C:
			message += "200";
			break;
		case BTN_WARM:
			message += "8";
			break;
		case BTN_WARM_5:
			message += "8005";
			break;
		case BTN_WARM_10:
			message += "8010";
			break;
		case BTN_WARM_20:
			message += "8020";
			break;
		}
		send(message += "\n");
	}
	
	public void askButtonStatus() throws IOException {
		send("get sys status\n");
	}
	
	public KettleStatus[] getStatus() {
		List<KettleStatus> result = new ArrayList<KettleStatus>();
		listener.transferQueue.drainTo(result);
		return (KettleStatus[]) result.toArray(new KettleStatus[result.size()]);
	}
	
	private void send(OutputStream out, String message) {
		try {
			out.write(message.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void send(String message) {
		try {
			outputStream.write(message.getBytes(StandardCharsets.US_ASCII));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void close() throws IOException {
		listener.closing = true;
		try {
			listenerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		outputStream.close();
		inputStream.close(); 
		sock.close();
	}
	
}
