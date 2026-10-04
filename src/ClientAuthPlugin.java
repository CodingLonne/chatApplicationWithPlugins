import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class ClientAuthPlugin implements ClientPlugin{
    private boolean authenticated;
    private String userName;
    private String password;

    @Override
    public void initialize(Connection connection){
        try {
            connection.send("AUTH\t" + userName + "\t" + password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setNeededArgs(String host, int port, String name, String[] args) {
        BufferedReader keyboard;
        try {
            keyboard = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
            userName = name;
            System.out.println("password:");
            System.out.flush();
            String password;
            try {
                password = keyboard.readLine();
                if (password == null) {
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String modifySendMessage(String line) {
        if (authenticated){
            return line;
        } else{
            String[] message = line.split("\t", -1);
            return "AUTH\t" + userName + "\t" + message[message.length -1];
        }
    }

}
