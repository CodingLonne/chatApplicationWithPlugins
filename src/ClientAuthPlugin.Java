public class ClientAuthPlugin implements ClientPlugin {
    private boolean authenticated;
    private String userName;

    @Override
    public void setNeededArgs(String host, int port, String name, String[] args) {
        userName = name;
        System.out.println("password:");
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

    @Override
    public String modifyReceiveMessage(String line) {
        if (authenticated){
            return line;
        } else{
            if ("OK\tAuthenticated".equals(answer)) {
                authenticated = true;
                return ("Connected. Type messages, /color blue, or /quit.");
            } else {
                return ("Authentication failed or server disconnected.");
            }
        }
    }
}