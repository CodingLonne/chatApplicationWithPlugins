public class ServerColorPlugin implements ServerPlugin{
    
    private static boolean validLabel(String value) {
        return value.matches("[A-Za-z0-9_-]{1,24}");
    }

    private String color;

    public ServerColorPlugin(){
    }

    public int getNeededArgsCount() {
        return 1;
    }

    @Override 
    public void setNeededArgs(String[] args){
        this.color = args[0];
    }

    @Override 
    public String modifySendMessage(String message){
        return this.color + "\t" + message;
    }

    @Override 
    public int getReceivedMessageArgsCount() {return 1;}

    @Override 
    public String verifyReceivedMessageArgs(String[] message) {
        if(validLabel(message[0])) {
            return "Invalid color";
        } else return null;
    }
}