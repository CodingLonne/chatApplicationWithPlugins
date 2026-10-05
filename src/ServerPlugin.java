public interface ServerPlugin {
    //Plugins can set-up after initial call from client on connection containing message
    default void initialize(Connection connection, String message) {}
    default boolean overrideUserConnectEvent() {return false;}
    default boolean userConnectEvent(Connection connection, String message) {return false;} //if true then userconnect event was succesfull

    //Plugins can indicate how many terminal arguments they claim (the color plugin claims 1)
    default int getArgCount() {return 0;}
    default void setNeededArgs(String[] args) {}

    // Plugins can modify how messages are sent. (aka encryption or adding a color)
    default String modifySendMessage(String message) {return message;}
    default String modifyReceiveMessage(String message) {return message;}

    // Plugins can verify their message arguments corresponding to them and give information about their expected contents
    // server receiving
    default int getReceivedMessageArgsCount() {return 0;}
    default String[] expectedVerifiedReceivedMessageArgs() {return new String[0];}
    default String verifyReceivedMessageArgs(String[] message) {return null;}
    //server sending
    default int getSendingMessageArgsCount() {return 0;}
    default String[] getSendingMessageArgs(String[] receivedArgs) {return new String[0];}

    // Plugins get notified whenever a message is sent or received
    default void receiveMessageEvent(Connection connection, String message) {}

    // Whenever a receive message is a command. Plugins may intercept this as their own command
    default void executeCommand(String commandString) {}
}
