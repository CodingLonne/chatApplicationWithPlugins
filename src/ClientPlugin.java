public interface ClientPlugin {
    // Plugins can use this method to tag on to behaviour in the initial set-up of the client-server connection
    default void initialize(Connection connection){return;}

    //Plugins can indicate how many arguments they claim (the color plugin claims 1)
    default int getArgCount() {return 0;}
    default void setNeededArgs(String host, int port, String name, String[] args) {}
    // Plugins can add message arguments
    default String[] getMessageArguments() {return new String[0];}
    // Plugins can modify how messages are sent. (aka encryption or adding a color)
    default String modifySendMessage(String line) {return line;}
    default String modifyReceiveMessage(String message) {return message;}
    // Plugins get notified when messages are sent or received
    default void sendMessageEvent() {}
    default void receiveMessageEvent() {}
    // Whenever a receive message is a command. Plugins may intercept this as their own command
    default boolean executeCommand(String commandString) {return false;}
}
