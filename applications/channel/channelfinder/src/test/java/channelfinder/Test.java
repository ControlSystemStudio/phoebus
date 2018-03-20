package channelfinder;

import java.util.Collection;

import org.phoebus.channelfinder.Channel;
import org.phoebus.channelfinder.ChannelFinderClient;
import org.phoebus.channelfinder.ChannelFinderClientImpl.CFCBuilder;

public class Test {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ChannelFinderClient client = CFCBuilder.serviceURL("https://localhost:9191/ChannelFinder").create();

        long start = System.currentTimeMillis();
        Collection<Channel> result = client.find("*XF*");
        System.out.println("Finished searching XF "+result.size()+" : " + String.valueOf(System.currentTimeMillis()-start));
        
        start = System.currentTimeMillis();
        result = client.find("*C01*");
        System.out.println("Finished searching to C01 "+result.size()+" : " + String.valueOf(System.currentTimeMillis()-start));
        
        start = System.currentTimeMillis();
        result = client.find("*C02*");
        System.out.println("Finished searching to C02 "+result.size()+"  : " + String.valueOf(System.currentTimeMillis()-start));
        start = System.currentTimeMillis();
    }

}
