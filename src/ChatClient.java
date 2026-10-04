import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ChatClient {
    private static class Receiver extends Thread {
        private final Connection connection;

        public Receiver(Connection connection) {
            this.connection = connection;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                String message; //TODO
                while ((message = connection.receive()) != null) {
                    String[] fields = message.split("\t", 4);
                    if (fields.length == 4 && "CHAT".equals(fields[0])) {
                        System.out.println("[" + fields[2] + "] " + fields[1] + ": " + fields[3]);
                    } else {
                        System.out.println(message.replace('\t', ' '));
                    }
                }
                System.out.println("Server disconnected. Press Enter to leave.");
            } catch (IOException e) {
                System.out.println("Connection closed: " + e.getMessage());
            } finally {
                connection.close();
            }
        }
    }

    public static void start_client(String host, int port, String userName) throws IOException, InterruptedException {
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        File logFile = File.createTempFile("client-" + userName + "-", ".log", new File("."));
        MessageLog log = new MessageLog(logFile.getPath());
        Socket socket = null;
        Connection connection = null;
        Receiver receiver = null;
        try {
            socket = new Socket(host, port);
            connection = new Connection(socket, log);
            System.out.println("Log: " + logFile.getAbsolutePath());
            connection.send("CON\t" + userName);

            for (ClientPlugin plugin : Launcher.clientPlugins) {
                plugin.initialize(connection);
            }

            receiver = new Receiver(connection);
            receiver.start();
            String line;
            while ((line = keyboard.readLine()) != null) {
                if (!receiver.isAlive()) {
                    break;
                }
                // decode beforehand
                for (ClientPlugin plugin : Launcher.clientPlugins) {
                    line = plugin.modifyReceiveMessage(line);
                }
                // check for commands, otherwise send
                if ("/quit".equals(line)) {
                    connection.send("QUIT");
                    break;
                } else if (line.length() > 0 && line.charAt(0) == '/') {
                    String commandString = line;
                    boolean pluginCommand = Launcher.clientPlugins.stream().anyMatch(plugin -> plugin.executeCommand(commandString));
                    if (!pluginCommand) {
                        System.out.println("Unknown command");
                    }
                    break;
                } else if (line.length() > 0) {
                    String message = "CHAT\t" + 
                        Launcher.clientPlugins.stream()
                            .flatMap(plugin -> Arrays.asList(plugin.getMessageArguments()).stream())
                            .collect(Collectors.joining("\t")) + line;
                    
                    for (ClientPlugin plugin : Launcher.clientPlugins) {
                        message = plugin.modifySendMessage(message);
                    }
                    connection.send(message);
                }
            }
        } finally {
            if (connection != null) {
                connection.close();
            } else if (socket != null) {
                socket.close();
            }
            if (receiver != null) {
                receiver.join();
            }
            log.close();
        }
    }
}
