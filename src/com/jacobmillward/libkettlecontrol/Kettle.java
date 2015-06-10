// Uses the information here: http://www.awe.com/mark/blog/20140223.html
// to decode responses, and send commands to the iKettle.
// Jacob Millward 28 Dec 2014
package com.jacobmillward.libkettlecontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
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
    public String iIPV4="192.168.1.";
    
    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public boolean isConnected() {
        return (sock!=null);
    }

    public Kettle() throws UnknownHostException {
        this.iIPV4 = InetAddress.getLocalHost().getHostAddress().substring(0, InetAddress.getLocalHost().getHostAddress().lastIndexOf('.')) + ".";
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
    
    private Socket scan() {
        for (int i = 1; i < 254; i++){
            System.out.println(iIPV4 + i + " ");
            try {
                    Socket mySocket = new Socket();
                    SocketAddress address = new InetSocketAddress(iIPV4 + i, port);
                    mySocket.setSoTimeout(2000);
                    mySocket.connect(address, 500);

                    BufferedReader in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
                    send(mySocket.getOutputStream(), "HELLOKETTLE\n");
                    try {
                        String response = in.readLine();
                        if (response.startsWith(KettleStatus.HELLO.code())) {
                            return mySocket;
                        }
                    } catch (SocketTimeoutException e) {
                    }
            }
            catch (IOException e) {
            }
        }
        return null;
    }
    
    public void sendCommand(KettleCommand command) {
        String message = "set sys output"+command.code();
        send(message += "\n");
    }
    
    public void askButtonStatus() throws IOException {
        send("get sys status\n");
    }
    
    public KettleStatus[] getStatus() {
        List<KettleStatus> result = new ArrayList<KettleStatus>();
        listener.messageQueue.drainTo(result);
        return result.toArray(new KettleStatus[result.size()]);
    }
    
    private void send(OutputStream out, String message) {
        try {
            out.write(message.getBytes(StandardCharsets.US_ASCII));
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
