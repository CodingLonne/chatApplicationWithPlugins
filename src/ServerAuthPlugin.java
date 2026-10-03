import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServerAuthPlugin implements ServerPlugin {
    private List<String> authenticatedUsers = new ArrayList<>();
    private static final String PASSWORD = "spl-chat";

    public static boolean validLabel(String value) {
        return value.matches("[A-Za-z0-9_-]{1,24}");
    }

    @Override
    public void receiveMessageEvent(Connection connection, String message) {
        String[] auth = message.split("\t", -1);
        if (!authenticatedUsers.contains(auth[1])){
            try {
                if (auth.length != 3 || !"AUTH".equals(auth[0])
                        || !validLabel(auth[1]) || !PASSWORD.equals(auth[2])) {
                    connection.send("ERROR\tAuthentication failed");
                } else {
                    connection.send("OK\tAuthenticated");
                    authenticatedUsers.add(auth[1]);
                }
            } catch (IOException e) {
                System.err.println("Input output exception occured when trying to reply to client");
            }
        }
    }

    @Override
    public String modifySendMessage(String message) {
        String[] auth = message.split("\t", -1);
        if(authenticatedUsers.contains(auth[1])){
            return message;
        } else {
            return "You are not authenticated yet";
        }
    }

}
//[red] Alice: Hi
