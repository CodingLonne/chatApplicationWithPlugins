import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/** The only network I/O path: every outgoing frame is encrypted. */
public class Connection {
    private final Socket socket;
    private final BufferedReader input;
    private final BufferedWriter output;
    private final MessageLog log;

    public Connection(Socket socket, MessageLog log) throws IOException {
        this.socket = socket;
        this.log = log;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
    }

    public String receive() throws IOException {
        String wire = input.readLine();
        if (wire == null) {
            return null;
        }
        String message = wire;
        log.record(socket.getRemoteSocketAddress().toString(), message); //TODO: remove if we want this as a plugin
        return message;
    }

    public synchronized void send(String message) throws IOException {
        if (message.indexOf('\n') >= 0 || message.indexOf('\r') >= 0) {
            throw new IOException("A protocol frame must be a single line.");
        }
        output.write(message);
        output.newLine();
        output.flush();
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Could not close socket: " + e.getMessage());
        }
    }
}
