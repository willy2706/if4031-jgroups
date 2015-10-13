import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

/**
 * Created by nim_13512065 on 10/13/15.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        JChannel channel=new JChannel("jgroups/src/main/resources/udp.xml");
        channel.setReceiver(new ReceiverAdapter() {
            public void receive(Message msg) {
                System.out.println("received msg from " + msg.getSrc() + ": " + msg.getObject());
            }
        });
        channel.connect("MyCluster");
        channel.send(new Message(null, null, "hello world"));
        channel.close();
    }
}
