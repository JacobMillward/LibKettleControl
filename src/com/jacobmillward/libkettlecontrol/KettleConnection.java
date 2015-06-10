package com.jacobmillward.libkettlecontrol;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;

class KettleConnection implements Runnable {

    public volatile boolean closing = false;
    public LinkedTransferQueue<KettleCommand> commandQueue;
    public LinkedTransferQueue<KettleStatus> messageQueue;
    private Socket sock;
    private KettleListener listener;
    private Thread listenerThread;
    private OutputStream outputStream;
    private InputStream inputStream;
    private String iIPV4 = "192.168.1.";
    private String hostname;
    private final int port = 2000;

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public KettleConnection() {
        commandQueue = new LinkedTransferQueue<>();
        messageQueue = new LinkedTransferQueue<>();
    }

    public KettleConnection(InetAddress address) {
        //Store hostname
        this.hostname = address.getHostAddress();
        commandQueue = new LinkedTransferQueue<>();
        messageQueue = new LinkedTransferQueue<>();
    }

    @Override
    public void run() {
        if (this.hostname != null) {
            try {
                sock = new Socket(hostname, port); //Attempt to create a socket
                outputStream = sock.getOutputStream();
                inputStream = sock.getInputStream();
            }
            catch (IOException e) {
                System.out.println("Could not create socket");
                e.printStackTrace();
            }
        }
        else {
            try {
                this.iIPV4 = InetAddress.getLocalHost().getHostAddress().substring(0, InetAddress.getLocalHost().getHostAddress().lastIndexOf('.')) + ".";
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            sock = scan();
            if (sock != null) {
                this.hostname = sock.getInetAddress().getHostAddress();
                try {
                    inputStream = sock.getInputStream();
                    outputStream = sock.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        listener = new KettleListener(inputStream);
        listenerThread = new Thread(listener);
        listenerThread.start();

        //For lifetime of thread
        while(!closing) {
            //Send any waiting commands
            while (!commandQueue.isEmpty()) {
                KettleCommand command = commandQueue.poll();
                String message = "set sys output"+command.code();
                send(message += "\n");
            }
            //Retrieve any waiting status messages
            while (!messageQueue.isEmpty()) {
                this.messageQueue.offer(listener.messageQueue.poll());
            }
        }

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

    private void send(OutputStream out, String message) {
        try {
            out.write(message.getBytes(StandardCharsets.US_ASCII));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String message) {
        try {
            outputStream.write(message.getBytes(StandardCharsets.US_ASCII));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return (sock != null);
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
