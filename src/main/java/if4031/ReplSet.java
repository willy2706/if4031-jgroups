package if4031;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class ReplSet<T extends Serializable> extends ReceiverAdapter {
    private final Class<T> typeClass;
    private final Set<T> state = new HashSet<>();

    private JChannel channel;

    public ReplSet(Class<T> a_typeClass) {
        typeClass = a_typeClass;
    }

    public void start() throws Exception {
        channel = new JChannel(); // use the default config, udp.xml, throws Exception
        channel.setReceiver(this);
        String clusterName = typeClass.getName() + "SetCluster";
        channel.connect(clusterName); // throws Exception
        channel.getState(null, 10000); // throws Exception
    }

    public void stop() {
        channel.close();
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (state) {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(output));
            objectOutputStream.writeObject(state);
            objectOutputStream.flush();
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        synchronized (state) {
            ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(input));
            state.clear();
            state.addAll((Set<T>) objectInputStream.readObject());
        }
    }

    @Override
    public void receive(Message msg) {
        synchronized (state) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(msg.getBuffer());
            BufferedInputStream bufferedInputStream = new BufferedInputStream(byteArrayInputStream);
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new ObjectInputStream(bufferedInputStream);
                int type = objectInputStream.readInt();
                switch (type) {
                    case ADD_COMMAND: {
                        T obj = (T) objectInputStream.readObject();
                        state.add(obj);
                        break;
                    }
                    case REMOVE_COMMAND: {
                        T obj = (T) objectInputStream.readObject();
                        state.remove(obj);
                        break;
                    }
                    default:
                        throw new IOException("Unknown format");
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();

            } finally {
                try {
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    } else {
                        bufferedInputStream.close();
                    }
                } catch (IOException e) {
                    // give up, because we cannot even close a stream, and cannot throw any exception
                }
            }
        }
    }

    /**
     * Mengembalikan true jika obj ditambahkan dan false jika obj telah ada pada set.
     *
     * @param obj objek yang ingin ditambahkan
     * @return true jika ditambahkan
     */
    public boolean add(T obj) throws Exception {
        synchronized (state) {
            if (!contains(obj)) { // interesting, this call a function which also needs to acquire lock, let's see what will happen
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream); // throws IOException
                objectOutputStream.writeInt(ADD_COMMAND); // throws IOException
                objectOutputStream.writeObject(obj); // throws IOException
                objectOutputStream.close();

                Message message = new Message(null, null, byteArrayOutputStream.toByteArray()); // throws Exception
                channel.send(message); // throws Exception

                return true;

            } else {
                return false;
            }
        }
    }

    /**
     * Mengembalikan true jika obj ada pada set.
     *
     * @param obj object yang ingin diketahui keanggotaannya
     * @return true jika obj ada pada set
     */
    public boolean contains(T obj) {
        synchronized (state) {
            return state.contains(obj);
        }
    }

    /**
     * Mengembalikan true jika obj ada pada set dan kemudian obj dihapus dari set.
     * Mengembalikan false jika obj tidak ada pada set.
     *
     * @param obj object yang ingin dihapus
     * @return true jika obj ada
     */
    public boolean remove(T obj) throws Exception {
        synchronized (state) {
            if (contains(obj)) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream); // throws IOException
                objectOutputStream.writeInt(REMOVE_COMMAND); // throws IOException
                objectOutputStream.writeObject(obj); // throws IOException
                objectOutputStream.close();

                Message message = new Message(null, null, byteArrayOutputStream.toByteArray()); // throws Exception
                channel.send(message); // throws Exception

                return true;

            } else {
                return false;
            }
        }
    }

    public static final int ADD_COMMAND = 0, REMOVE_COMMAND = 1;
}
