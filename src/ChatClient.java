import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ChatClient {
    private static class Receiver extends Thread {
        private final Connection connection;

        public Receiver(Connection connection) {
            this.connection = connection;
            setDaemon(true);
        }

        public void run() {
            try {
                String message;
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

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 3 || args.length > 4 || !ChatServer.validLabel(args[2])) {
            System.out.println("Usage: java ChatClient host port name [color]");
            System.out.println("Name/color: 1-24 letters, digits, underscores or hyphens.");
            return;
        }
        String color = args.length == 4 ? args[3] : "black";
        if (!ChatServer.validLabel(color)) {
            System.out.println("Invalid color label.");
            return;
        }
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
        System.out.print("Password: ");
        System.out.flush();
        String password = keyboard.readLine();
        if (password == null) {
            return;
        }
        File logFile = File.createTempFile("client-" + args[2] + "-", ".log", new File("."));
        MessageLog log = new MessageLog(logFile.getPath());
        Socket socket = null;
        Connection connection = null;
        Receiver receiver = null;
        try {
            socket = new Socket(args[0], Integer.parseInt(args[1]));
            connection = new Connection(socket, log);
            System.out.println("Log: " + logFile.getAbsolutePath());
            connection.send("AUTH\t" + args[2] + "\t" + password);
            String answer = connection.receive();
            if (!"OK\tAuthenticated".equals(answer)) {
                System.out.println("Authentication failed or server disconnected.");
                return;
            }
            System.out.println("Connected. Type messages, /color blue, or /quit.");
            receiver = new Receiver(connection);
            receiver.start();
            String line;
            while ((line = keyboard.readLine()) != null) {
                if (!receiver.isAlive()) {
                    break;
                }
                if ("/quit".equals(line)) {
                    connection.send("QUIT");
                    break;
                } else if (line.startsWith("/color ")) {
                    String candidate = line.substring(7).trim();
                    if (ChatServer.validLabel(candidate)) {
                        color = candidate;
                        System.out.println("Outgoing color: " + color);
                    } else {
                        System.out.println("Invalid color label.");
                    }
                } else if (line.length() > 0) {
                    connection.send("CHAT\t" + color + "\t" + line);
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
