import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

/** One flushed, timestamped record for every received protocol message. */
public class MessageLog {
    private final BufferedWriter writer;

    public MessageLog(String path) throws IOException {
        writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(path, true), "UTF-8"));
    }

    public synchronized void record(String peer, String message) throws IOException {
        // Record authentication events without storing the password.
        if (message.startsWith("AUTH\t")) {
            String[] fields = message.split("\t", -1);
            message = "AUTH\t" + (fields.length > 1 ? fields[1] : "?")
                    + "\t[password redacted]";
        }
        message = message.replace("\r", "\\r").replace("\n", "\\n");
        writer.write(new Date().toString() + " | " + peer + " | " + message);
        writer.newLine();
        writer.flush();
    }

    public synchronized void close() throws IOException {
        writer.close();
    }
}
