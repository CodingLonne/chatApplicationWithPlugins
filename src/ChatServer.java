import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    private static final String PASSWORD = "spl-chat";
    private Session[] clients = new Session[8];
    private int count;
    private final MessageLog log;

    public ChatServer(MessageLog log) {
        this.log = log;
    }

    private synchronized void add(Session client) {
        if (count == clients.length) {
            Session[] larger = new Session[clients.length * 2];
            System.arraycopy(clients, 0, larger, 0, count);
            clients = larger;
        }
        clients[count++] = client;
    }

    private synchronized void remove(Session client) {
        for (int i = 0; i < count; i++) {
            if (clients[i] == client) {
                clients[i] = clients[--count];
                clients[count] = null;
                return;
            }
        }
    }

    private synchronized Session[] snapshot() {
        Session[] result = new Session[count];
        System.arraycopy(clients, 0, result, 0, count);
        return result;
    }

    // Serialize broadcasts so every recipient sees the same message order.
    private synchronized void broadcast(String message) {
        Session[] recipients = snapshot();
        for (int i = 0; i < recipients.length; i++) {
            try {
                recipients[i].connection.send(message);
            } catch (IOException e) {
                remove(recipients[i]);
                recipients[i].connection.close();
            }
        }
    }

    public static boolean validLabel(String value) {
        return value.matches("[A-Za-z0-9_-]{1,24}");
    }

    private class Session extends Thread {
        private final Connection connection;

        public Session(Socket socket) throws IOException {
            connection = new Connection(socket, log);
        }

        public void run() {
            try {
                String first = connection.receive();
                if (first == null) {
                    return;
                }
                String[] auth = first.split("\t", -1);
                if (auth.length != 3 || !"AUTH".equals(auth[0])
                        || !validLabel(auth[1]) || !PASSWORD.equals(auth[2])) {
                    connection.send("ERROR\tAuthentication failed");
                    return;
                }
                String name = auth[1];
                // Make acknowledgement and registration atomic with broadcasts.
                synchronized (ChatServer.this) {
                    connection.send("OK\tAuthenticated");
                    add(this);
                }
                String message;
                while ((message = connection.receive()) != null) {
                    if ("QUIT".equals(message)) {
                        break;
                    }
                    String[] fields = message.split("\t", 3);
                    if (fields.length != 3 || !"CHAT".equals(fields[0])
                            || !validLabel(fields[1]) || fields[2].length() == 0) {
                        connection.send("ERROR\tExpected CHAT, color and nonempty text");
                        continue;
                    }
                    broadcast("CHAT\t" + name + "\t" + fields[1] + "\t" + fields[2]);
                }
            } catch (IOException e) {
                System.err.println("Connection ended: " + e.getMessage());
            } finally {
                remove(this);
                connection.close();
            }
        }
    }

    public void serve(int port) throws IOException {
        ServerSocket listener = new ServerSocket(port);
        try {
            System.out.println("Chat server listening on port " + listener.getLocalPort());
            while (true) {
                Socket socket = listener.accept();
                try {
                    new Session(socket).start();
                } catch (IOException e) {
                    socket.close();
                    System.err.println("Cannot initialize client: " + e.getMessage());
                }
            }
        } finally {
            listener.close();
        }
    }
}
