public class ServerLoggingPlugin implements ServerPlugin{
    private final MessageLog log;
    private final Socket socket;

    Public ServerLoggingPlugin(){
    }

    @Override
    public int getArgCount() {return 2;}


    @Override
    public void setNeededArgs(String[] args) {
        if args.count()
        this.log = args[0]
        this.log = args[1]
    }

    @Override
    public void receiveMessageEvent(String message) {
        log.record(socket.getRemoteSocketAddress().toString(), message);
    }



    
}