JAVA CHAT
=========

This is the chat implementation. All four features are always active.
The Java code uses no generics, raw collections, lambdas, streams, records,
var, or try-with-resources. It uses arrays, ordinary classes, synchronized
methods/blocks, and Thread subclasses. There are no external dependencies.

BUILD AND RUN
-------------
Install a JDK with javac and java on PATH. From this directory:

    mkdir bin
    javac -encoding UTF-8 -d bin src/*.java

Start a server in one terminal:

    java -cp bin ChatServer

Start clients in two other terminals, from this same directory:

    java -cp bin ChatClient localhost 5000 Alice red
    java -cp bin ChatClient localhost 5000 Bob blue

Enter the fixed password spl-chat in each client. The password prompt uses
ordinary console input (it is visible while typed). Type a message and press
Enter. Every authenticated client, including the sender, receives it, e.g.:

    [red] Alice: Hello Bob!

Commands:
    /color green     Set the color for all subsequent outgoing messages.
    /quit            Disconnect this client.

The initial color defaults to black if omitted. Colors are textual labels,
as expressly permitted by the assignment, not terminal escape sequences.
Names and colors accept 1-24 ASCII letters, digits, underscores or hyphens.
Messages are nonempty lines; tabs and Unicode text are supported. Empty input
is ignored. Names are display labels and need not be unique.

Optional server arguments:
    java -cp bin ChatServer 6000 my-server.log
For clients on another computer, replace localhost with the server hostname
and use the server's port. Stop the server with Ctrl+C. If it disconnects,
press Enter in an idle client to leave its blocking keyboard input.

In Eclipse, create a Java project and copy these source files into its src
folder. Run ChatServer as a Java application. Create separate ChatClient
run configurations with the arguments shown above. No libraries are needed.

HOW THE REQUIREMENTS ARE MET
----------------------------
1. Multiple clients: ChatServer accepts TCP sockets and starts a Session
   thread for each connection. It broadcasts valid chat messages to all
   authenticated sessions. An array stores sessions without using generics
   or collections. Synchronized operations protect registration/removal and
   broadcasts; a synchronized send prevents overlapping output frames.

2. Color: ChatClient keeps its current color in a String. Every outgoing
   CHAT includes it. The server preserves it, and receivers display it.
   /color changes the value, never disables the feature.

3. Authentication: The first client frame must be AUTH with a name and the
   fixed password. The server validates it before registering that connection
   for broadcasts or accepting chat. Incorrect passwords, malformed AUTH,
   and chat sent before authentication receive an encrypted ERROR followed
   by disconnection. A waiting unauthenticated connection receives no chat.
   OK/ERROR authentication responses are control messages, not chat messages.
   The server derives the sender's name from its authenticated session, not
   from an incoming chat payload.

4. Two encryption algorithms: Connection.send applies ROT13 to the entire
   protocol frame, then reverses the result. Connection.receive reverses the
   received frame, then applies ROT13 to undo the first transformation.
   Both stages apply to authentication, passwords, acknowledgements, errors,
   chat (including names/colors), and QUIT. Only the line delimiter used for
   framing is outside encryption. For example, Hello -> Uryyb -> byyrU.
   These are deliberately simple assignment algorithms, not secure encryption
   for real-world private communication.

5. Logging: Connection.receive decrypts and logs every received frame before
   returning it to application code. This includes rejected messages and
   control messages. AUTH records retain the name but redact the password.
   The server appends to server.log (or the selected file). Each client creates
   its own uniquely named client-NAME-*.log, even when names are reused.
   Log entries contain a timestamp, remote address, and decoded message.
   Logs use UTF-8 and flush each record immediately. The shared server log is
   synchronized. A log write failure ends that connection rather than silently
   continuing without logging. A server log records inbound messages, not a
   second copy of each outbound broadcast; clients log their received copy.

FILES AND PROTOCOL
------------------
Crypto.java       Two transformations and their inverses.
MessageLog.java   Timestamped, synchronized file logging.
Connection.java   UTF-8 socket framing, encryption/decryption and receive log.
ChatServer.java   Listener, authentication, session threads and broadcast.
ChatClient.java   Console commands and background receiving thread.

Below, <TAB> is an actual tab. Each entire frame is encrypted before sending:

Client -> server:
    AUTH<TAB>name<TAB>password
    CHAT<TAB>color<TAB>text
    QUIT

Server -> client:
    OK<TAB>Authenticated
    ERROR<TAB>description
    CHAT<TAB>name<TAB>color<TAB>text

After authentication, malformed commands receive ERROR and the connection
remains usable. EOF or QUIT removes a session. Text is split with a limit so
tabs inside a message remain part of its text. Socket closure unblocks reads.
Console commands themselves are local; their effects appear in later frames.

This intentionally small implementation uses blocking I/O and one thread per
client. A stalled receiver can delay broadcasting; there are no timeouts,
message-size limits, chat history, or production-scale delivery guarantees.

VERIFICATION
------------
The optional integration test needs Python 3 and a JDK; the chat itself needs
only Java. From this directory run:

    python tests/integration_test.py

It builds into a temporary directory and starts a real Java server and client.
It checks encrypted frames, wrong-password rejection, chat-before-auth
rejection, no broadcasts to unauthenticated peers, multi-client delivery,
Unicode/tab text, malformed-message handling, disconnect cleanup, live client
color changes, and server/client log contents. Temporary files are cleaned up.

chat.zip contains this README, all Java source files, and a test.
