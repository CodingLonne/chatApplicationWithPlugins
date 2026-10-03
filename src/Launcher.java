import java.io.IOException;
import java.util.List;

public class Launcher {
    public static List<Plugin> plugins;
    public static void main(String[] args) throws IOException, InterruptedException {
        // java Launcher client host port name [color]
        // java Launcher server [port] [log-file]
        plugins = List.of();
        
        if (args[0].equals("client")) {

        } else if (args[1].equals("server")) {
            //server
            if (args.length > 2) {
                System.out.println("Usage: java ChatServer [port] [log-file]");
                return;
            }
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
            MessageLog log = new MessageLog(args.length > 1 ? args[1] : "server.log");
            try {
                new ChatServer(log).serve(port);
            } finally {
                log.close();
            }
        }
    }
}
