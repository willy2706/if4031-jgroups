package if4031;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import java.io.*;
import java.util.Stack;

public class ReplStack<T extends Serializable> extends ReceiverAdapter {
    private final Class<T> typeClass;
    private final Stack<T> stack = new Stack<>();

    private JChannel channel;

    public ReplStack(Class<T> a_typeClass) {
        typeClass = a_typeClass;
    }

    public void start() throws Exception {
        channel = new JChannel(); // use the default config, udp.xml, throws Exception
        channel.setReceiver(this);
        String clusterName = typeClass.getName() + "StackCluster";
        channel.connect(clusterName); // throws Exception
        channel.getState(null, 10000); // throws Exception
    }

    public void stop() {
        channel.close();
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (stack) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(output));
            objectOutputStream.writeObject(stack);
            objectOutputStream.flush();
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        synchronized (stack) {
            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(input));
            stack.clear();
            stack.addAll((Stack<T>) objectInputStream.readObject()); // TODO maybe this will result in reverse order?
        }
    }

    @Override
    public void receive(Message msg) {
        synchronized (stack) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(msg.getBuffer());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(byteArrayInputStream);
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new ObjectInputStream(bufferedInputStream);
                int type = objectInputStream.readInt();
                switch (type) {
                    case PUSH_COMMAND: {
                        T pushObject = (T) objectInputStream.readObject();
                        stack.push(pushObject);
                        break;
                    }
                    case POP_COMMAND: {
                        stack.pop();
                        break;
                    }
                    default:
                        throw new IOException("Unknown format");
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();

            } finally {
                if (objectInputStream != null) {
                    try {
                        objectInputStream.close();
                    } catch (IOException e) {
                        // give up if cannot close
                    }
                } else {
                    try {
                        bufferedInputStream.close();
                    } catch (IOException e) {
                        // give up if cannot close
                    }
                }
            }
        }
    }

    public void push(T obj) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream); // throws IOException
        objectOutputStream.writeInt(PUSH_COMMAND); // throws IOException
        objectOutputStream.writeObject(obj); // throws IOException
        objectOutputStream.close();

        Message message = new Message(null, null, byteArrayOutputStream.toByteArray()); // throws Exception
        channel.send(message); // throws Exception
    }

    public T pop() throws Exception {
        synchronized (stack) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream); // throws IOException
            objectOutputStream.writeInt(POP_COMMAND); // throws IOException
            objectOutputStream.close();

            Message message = new Message(null, null, byteArrayOutputStream.toByteArray()); // throws Exception
            channel.send(message); // throws Exception

            return top();
        }
    }

    public T top() {
        synchronized (stack) {
            return stack.peek();
        }
    }

    public int size() {
        synchronized (stack) {
            return stack.size();
        }
    }

    private static final int PUSH_COMMAND = 0, POP_COMMAND = 1;
}
