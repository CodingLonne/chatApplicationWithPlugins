public class ClientColorPlugin implements ClientPlugin {
    private String color;

    public ClientColorPlugin(){
        
    }

    public int getNeededArgs() {
        return 1;
    }

    @Override
    public String modifySendMessage(String message){
        return this.color + "\t" + message;

    }

    @Override 
    public boolean executeCommand(String commandString) {
        String[] parts = commandString.split("\t", 2);
        if (parts[0].equals("/color")) {
            String candidate = parts[1].substring(7).trim();
            if (ChatServer.validLabel(candidate)) {
                color = candidate;
                System.out.println("Outgoing color: " + color);
            } else {
                System.out.println("Invalid color label.");
            }
            return true;
        } else {
            return false;
        }
    }
}