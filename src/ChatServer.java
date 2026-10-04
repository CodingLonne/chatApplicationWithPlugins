import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChatServer {
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

        @Override
        public void run() {
            try {
                String first = connection.receive();
                if (first == null) {
                    return;
                }
                String[] auth = first.split("\t", -1);
                String name = auth[1]; // FLAG: let's try setting this to 0 (was 1)
                // Make acknowledgement and registration atomic with broadcasts.
                synchronized (ChatServer.this) {
                    connection.send("Connected.");
                    add(this);
                }

                String second = connection.receive();
                for (ServerPlugin plugin : Launcher.serverPlugins) {
                    plugin.initialize(connection, second);
                }
                
                String message;
                while ((message = connection.receive()) != null) {
                    for (ServerPlugin plugin : Launcher.serverPlugins) {
                        message = plugin.modifyReceiveMessage(message);
                    }
                    if ("QUIT".equals(message)) {
                        break;
                    }
                    String[] fields = message.split("\t", 3);
                    //receiving: CHAT pluginargs* text
                    //sending: CHAT name pluginargs* text
                    if (fields.length != 2 + Launcher.serverPlugins.stream().mapToInt(ServerPlugin::getReceivedMessageArgsCount).sum()) {
                        List<String> expectedArgs = List.of("CHAT");
                        expectedArgs.addAll(Launcher.serverPlugins.stream()
                            .flatMap(plugin -> Arrays.asList(plugin.expectedVerifiedReceivedMessageArgs()).stream())
                            .toList());
                        connection.send("ERROR\tExpected " + expectedArgs.stream().collect(Collectors.joining(", ")) + " and nonempty text");
                        continue;
                    }
                    // inspect received arguments and return error to client if applicable
                    String[] pluginReceivedArgs = Arrays.copyOfRange(fields, 1, fields.length-1);
                    String potentialErrorMessage = pluginMessageArgsVerifying(pluginReceivedArgs);
                    if (potentialErrorMessage != null) {
                        connection.send("ERROR\t" + potentialErrorMessage);
                        continue;
                    }
                    // process received arguments
                    message = "CHAT\t" + name + String.join("\t", pluginMessageArgsSending(pluginReceivedArgs)) + '\t' + fields[fields.length-1];
                    for (ServerPlugin plugin : Launcher.serverPlugins) {
                        message = plugin.modifySendMessage(message);
                    }
                    broadcast(message);
                }
            } catch (IOException e) {
                System.err.println("Connection ended: " + e.getMessage());
            } finally {
                remove(this);
                connection.close();
            }
        }
    }

    private String[] pluginMessageArgsSending(String[] pluginArgsReceived) {
        int inputIndex = 0;
        int outputIndex = 0;
        int sendingArgumentsCount = Launcher.serverPlugins.stream().mapToInt(ServerPlugin::getSendingMessageArgsCount).sum();
        String[] sendingArguments = new String[sendingArgumentsCount];
        for (ServerPlugin plugin : Launcher.serverPlugins) {
            String[] pluginReceivedArgs = Arrays.copyOfRange(pluginArgsReceived, inputIndex, inputIndex+plugin.getReceivedMessageArgsCount());
            inputIndex += plugin.getReceivedMessageArgsCount();
            for (String arg : plugin.getSendingMessageArgs(pluginReceivedArgs)) {
                sendingArguments[outputIndex] = arg;
                outputIndex += 1;
            }
        }
        return sendingArguments;
    }

    private String pluginMessageArgsVerifying(String[] pluginArgsReceived) {
        int index = 0;
        String errorMsg = null; // if all plugins have valid arguments this stays null, otherwise error message will be stored here
        for (ServerPlugin plugin : Launcher.serverPlugins) {
            String[] pluginReceivedArgs = Arrays.copyOfRange(pluginArgsReceived, index, index+plugin.getReceivedMessageArgsCount());
            index += plugin.getReceivedMessageArgsCount();
            errorMsg = plugin.verifyReceivedMessageArgs(pluginReceivedArgs);
            if (errorMsg != null) break;
        } 
        return errorMsg;
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
