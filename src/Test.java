import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.jacobmillward.libkettlecontrol.Kettle;
import com.jacobmillward.libkettlecontrol.KettleCallback;
import com.jacobmillward.libkettlecontrol.KettleStatus;

public class Test {

    public static Kettle kettle;

    public static void main(String[] args) throws UnknownHostException {
        kettle = new Kettle(new PrintStatusTask());
    }

}

class PrintStatusTask implements KettleCallback, Runnable {

    @Override
    public void onConnectionComplete() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        if (Test.kettle.isConnected()) {
            System.out.println("Connected to "+Test.kettle.getHostname());
            try {
                while(System.in.available() == 0) {
                    ArrayList<KettleStatus> ks;
                    ks = Test.kettle.getStatus();
                    if (!ks.isEmpty()) {
                        for (KettleStatus s : ks) {
                            System.out.print(s.name() + " ");
                        }
                        System.out.print("\n");
                    }
                }
                System.out.println("Stopping");
                Test.kettle.close();
                System.out.println("Stopped");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("Could not connect to kettle.");
        }
    }
}
