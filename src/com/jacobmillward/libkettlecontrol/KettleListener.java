package com.jacobmillward.libkettlecontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedTransferQueue;

class KettleListener implements Runnable {
    public volatile BufferedReader inputReader;
    public LinkedTransferQueue<KettleStatus> messageQueue;
    public volatile boolean closing = false;
    
    public KettleListener(InputStream inputStream) {
        this.inputReader = new BufferedReader(new InputStreamReader(inputStream));
        messageQueue = new LinkedTransferQueue<>();
    }

    @Override
    public void run() {
        //For lifetime of thread
        while(!closing) {
            String statusString;
            try {
                if(inputReader.ready()) {
                    statusString = inputReader.readLine();
                    for(KettleStatus status : handleMessage(statusString)) {
                        messageQueue.offer(status);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            inputReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }
    
    private KettleStatus[] handleMessage(String message) {
        //Check if message is asynchronous update or requested status
        KettleStatus[] result;
        if(message.startsWith("sys status key=")) {
            //Check if there is a status character
            if(message.length() == 18) {
                //Convert the 16th byte into a boolean array containing the first 6 bits
                //16th byte of response contains information on which buttons are active
                byte[] StatusByte = message.substring(15, 15).getBytes(StandardCharsets.US_ASCII);
                boolean statusBoolArray[] = new boolean[6];
                statusBoolArray[0] = ((StatusByte[0] & 0x01) != 0);
                statusBoolArray[1] = ((StatusByte[0] & 0x02) != 0);
                statusBoolArray[2] = ((StatusByte[0] & 0x04) != 0);
                statusBoolArray[3] = ((StatusByte[0] & 0x08) != 0);
                statusBoolArray[4] = ((StatusByte[0] & 0x10) != 0);
                statusBoolArray[5] = ((StatusByte[0] & 0x20) != 0);
                
                int counter = 0;
                for(boolean value : statusBoolArray) {
                    if (value) counter++;
                }
                
                //Fill array with correct status.
                result = new KettleStatus[counter];
                int i = 0;
                if(statusBoolArray[0]) {
                    result[i] = KettleStatus.TURNED_ON; 
                    i++;
                }
                if(statusBoolArray[1]) {
                    result[i] = KettleStatus.SEL_WARM;
                    i++;
                }
                if(statusBoolArray[2]) {
                    result[i] = KettleStatus.SEL_65C;
                    i++;
                }
                if(statusBoolArray[3]) {
                    result[i] = KettleStatus.SEL_80C;
                    i++;
                }
                if(statusBoolArray[4]) {
                    result[i] = KettleStatus.SEL_95C;
                    i++;
                }
                if(statusBoolArray[5]) {
                    result[i] = KettleStatus.SEL_100C;
                    i++;
                }
            }
            //If no status character
            result = new KettleStatus[0];
        }
        //If HELLOAPP response
        else if (message.startsWith(KettleStatus.HELLO.code())) {
            result = new KettleStatus[1];
            result[0] = KettleStatus.HELLO;
        }
        //If asynchronous update
        else {
            String status = message.substring(11); //Code begins here
            result = new KettleStatus[1]; //Prepare variable to hold single result
            
            for(KettleStatus kStatus : KettleStatus.values()) {
                if (kStatus.code().equals(status)) {
                    result[0] = kStatus;
                    break;
                }
            }
        }
        
        return result;
    }
}
