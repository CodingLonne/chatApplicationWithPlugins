public class ServerEncryptionPlugin implements ServerPlugin{
    private String encryptionType;

    public ServerEncryptionPlugin(){}

    @Override
    public int getArgCount() {return 1;}

    @Override
    public void setNeededArgs(String[] args){
        this.encryptionType = args[0];
    }

    @Override
    public String modifySendMessage(String message){
        return Crypto.encrypt(message, encryptionType);
    }

    @Override
    public String modifyReceiveMessage(String message) {
        return Crypto.decrypt(message, encryptionType);
    }   
}