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
    
    private KettleConnection kettleCon;
    private Thread kettleConThread;
    
    public String getHostname() {
        return kettleCon.getHostname();
    }

    public int getPort() {
        return kettleCon.getPort();
    }

    public boolean isConnected() {
        return kettleCon.isConnected();
    }

    public Kettle(KettleCallback c) {
        kettleCon = new KettleConnection(c);
        kettleConThread = new Thread(kettleCon);
        kettleConThread.start();
    }
    
    public Kettle(InetAddress address, KettleCallback c) {
        kettleCon = new KettleConnection(address, c);
        kettleConThread = new Thread(kettleCon);
        kettleConThread.start();
    }
    
    public void sendCommand(KettleCommand command) {
        kettleCon.commandQueue.offer(command);
    }
    
    public void askButtonStatus() throws IOException {
        kettleCon.send("get sys status\n");
    }
    
    public ArrayList<KettleStatus> getStatus() {
        ArrayList<KettleStatus> result = new ArrayList<>();
        kettleCon.messageQueue.drainTo(result);
        return result;
    }

    public void close() throws IOException {
        kettleCon.close();
        kettleCon.closing = true;
        try {
            kettleConThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    
}
