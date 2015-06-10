import java.io.IOException;
import java.util.ArrayList;

import com.jacobmillward.libkettlecontrol.Kettle;
import com.jacobmillward.libkettlecontrol.KettleStatus;

public class Test {

    public static void main(String[] args) throws IOException{
        Kettle kettle = new Kettle();
        if(kettle.getHostname() != null) {
            System.out.println("Connected to "+kettle.getHostname());
            ArrayList<KettleStatus> ks;
            while(System.in.available() == 0) {
                ks = kettle.getStatus();
                if (ks != null) {
                    for (KettleStatus s : ks) {
                        System.out.print(s.name() + " ");
                    }
                    System.out.print("\n");
                }
            }
        }
        kettle.close();
    }

}
