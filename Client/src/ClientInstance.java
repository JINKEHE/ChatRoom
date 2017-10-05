import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/*
 * **************************** 
 * Filename: ClienInstance.java 
 * Student Name: Jinke He
 * University ID: 201219022 
 * Departmental ID: x6jh 
 * ****************************
 */

/*
 * Additional Functionality:
 * 
 * 1. Uses XOR Cipher to encode and decode messages to improve security.
 * 
 * 2. Allows a client to block messages from another client. On the other
 * hand, allows he/she to unblock another client.
 * 
 * 3. Allows a client to send private messages to another client, which are invisible
 * to other clients.
 * 
 * 4. Allows a client to become an Administrator by entering a password.
 * 
 * 5. Allows an Administrator to kick a client out of the chat room.
 * 
 * 6. Allows an Administrator to shut down the server.
 * 
 * 7. Allows a client to clear his/her screen with the request command '-cls'.
 * 
 */

/**
 * <p>
 * This class defines a Client Instance that can
 * <ul>
 * <li>Build connections with a server.</li>
 * <li>Accept user input and handle sending to the server.</li>
 * <li>Handle responses from the server.</li>
 * </ul>
 * <p>
 * There are two threads in a ClientInstance:
 * <ul>
 * <li>{@link IncomingHandler} handles incoming messages</li>
 * <li>{@link OutgoingHandler} handles outgoing messages</li>
 * </ul>
 * They works concurrently.
 */
public class ClientInstance {

    /**
     * The socket used to connect to the server.
     */
    private static Socket socket;

    /**
     * The user name.
     */
    private static String name;

    /**
     * The BufferedReader for incoming streams.
     */
    private static BufferedReader br;

    /**
     * The PrintWriter for outgoing streams.
     */
    private static PrintWriter pw;

    /**
     * The code used in the encryption and decryption of messages.
     */
    private final static int CODE = 20;

    /**
     * A signal indicating that the user name entered the user has been
     * validated by the server.
     */
    private final static String NAME_CHECKED = "[Server] [Valid]";

    /**
     * A signal indicating that the client has been kicked out of the chat room
     * by Administrator.
     */
    private final static String KICKED_OUT = "[Server] [Kicked Out]";

    /**
     * Whether the name has been validated by the server.
     */
    private static volatile boolean nameValidated = false;

    /**
     * Whether the client has finished the chat.
     */
    private static volatile boolean finished = false;

    /**
     * Whether the client is kicked out of the chat room by Administrator.
     */
    private static volatile boolean kickedOut = false;

    /**
     * The Scanner used to read input from user.
     */
    private static Scanner kb;

    /**
     * A separator.
     */
    public final static String SEPARATOR = "------------------------------------------------------------------";

    /**
     * <p>
     * Cuts off the connection with server, close the streams and program exits.
     * </p>
     * {@code close()} will be called in three cases:
     * <ol>
     * <li>Client wants to disconnect.</li>
     * <li>Connection is lost or Server is shut down.</li>
     * <li>Client is kicked out of the chat room by Administrator.</li>
     * </ol>
     * <p>
     * Exceptions are going to be handled here and reasons for 
     * why the Client program is going to exit will be printed out.
     * </p>
     */
    private static synchronized void close() {

	System.out.println(SEPARATOR);

	// Case 1: Client wants to disconnect.
	if (finished)
	    System.out.println("You have left the chat room.");

	// Case 2: Client lost connection suddenly.
	if (!finished && !kickedOut)
	    System.out.println("Connection lost.");

	// Case 3: Client is kicked out of the chat room by Administrator.
	if (!finished && kickedOut)
	    System.out.println("You were kicked out of the chat room.");

	// Closes the socket and streams.
	try {
	    pw.close();
	    br.close();
	    if (socket != null) socket.close();
	} catch (IOException e) {
	    System.err.println(e.getMessage());
	} finally {
	    // Program exits
	    System.exit(0);
	}
    }

    /**
     * <p>
     * Uses XOR Cipher to encode or decode a message.
     * </p>
     * Since x ^ y ^ y = x (^ is XOR operation), XOR can be used as a cipher.
     * <br>
     * <p>
     * When sending or receiving a message from each other, Server and Client
     * will encode or decode it first to make sure that even if some people can
     * get the messages in some illegal ways, the messages are just messy codes
     * to them because of the lack of key to decode them.
     * </p>
     * <p>
     * Since that for XOR Cipher, the process of encryption and decryption are
     * exactly the same, we simply use {@link #encode(String)} for both encoding
     * and decoding.
     * </p>
     * 
     * @param str <br>
     *         In encryption, str is the original message <br>
     *         In decryption, str is the encrypted message
     * @return In encryption, return the encrypted message <br>
     * 	       In decryption, return the original message
     * 
     */
    private static String encode(String str) {

	// Returns null if the input is null
	if (str == null) return null;

	// Converts the given String to a char array
	char[] charArray = str.toCharArray();

	// Performs XOR operations on each element in the array
	for (int i = 0; i < charArray.length; i++) {
	    charArray[i] = (char) (charArray[i] ^ CODE);
	}

	// Returns a new String
	return new String(charArray);
    }

    /**
     * <p>
     * Creates a new instance of ClientInstance.
     * </p>
     * A ClientInstance has two threads:
     * <ul>
     * <li>{@link IncomingHandler} handles receiving messages.</li>
     * <li>{@link OutgoingHandler} handles sending messages</li>
     * </ul>
     * 
     * @param serverIP
     *            the IP address of the server
     * @param port
     *            the port number of the server
     */
    public ClientInstance(String serverIP, int port) {

	/* Builds a connection with server and initialises I/O streams. */
	try {
	    // build a connection
	    socket = new Socket(serverIP, port);
	    System.out.println(SEPARATOR);
	    kb = new Scanner(System.in);
	    // set up I/O
	    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
	} catch (IOException IOE) {
	    // Fails to build a connection
	    System.err.println("I/O problem found: ");
	    System.err.println(IOE.getMessage());
	    return;
	}

	/*
	 * Creates two threads to receive messages and send messages
	 * concurrently.
	 */
	Thread incoming = new Thread(new IncomingHandler());
	Thread outgoing = new Thread(new OutgoingHandler());
	outgoing.start();
	incoming.start();

    }

    /**
     * <p>
     * IncomingHandler handles receiving and printing out incoming messages.
     * </p>
     * Before a client enters the chat room, he/she needs to select a user name
     * and the server will validate it.
     */
    private static class IncomingHandler implements Runnable {

	@Override
	public void run() {

	    try {
		/*
		 * In Stage 1, the program is actually waiting for a signal from
		 * the server. After the server validates the name chosen by the
		 * client, it will send a signal "[Server] [Valid]" to the
		 * client and then IncomingHandler will move to next stage.
		 * 
		 * In the first stage, the client can only receive messages from
		 * the server.
		 */
		while (!nameValidated) {
		    String feedback = encode(br.readLine());
		    if (feedback.equals(NAME_CHECKED)) {
			// User name is validated by the server
			System.out.println();
			nameValidated = true;
		    } else if (feedback.startsWith("[Server] ")) {
			// Only shows messages from the server
			System.out.println(feedback);
		    }
		}

		/*
		 * In Stage 2, prints out all the messages received until the
		 * connection lost(read null) or it receives a signal indicating
		 * that the client has been kicked out of the chat room.
		 */
		while (!finished) {
		    String messageIn = encode(br.readLine());
		    if (messageIn == null) {
			// Connection lost
			break;
		    } else if (messageIn.equals(KICKED_OUT)) {
			// The client is kicked out of the chat room
			kickedOut = true;
			break;
		    } else {
			// Otherwise, prints out the messages
			System.out.println(messageIn);
		    }
		}
	    } catch (IOException e) {
		// Errors and exceptions would be handled in close()
	    } finally {
		close();
	    }
	}

    }

    /**
     * <p>
     * OutgoingHandler handles sending messages to the server.
     * </P>
     * <p>
     * Before the client can chat with others, he/she needs to 
     * enter a valid user name, which is not used by others or empty.
     * </p>
     */
    private static class OutgoingHandler implements Runnable {

	@Override
	public void run() {
	    try {
		/*
		 * Stage 1, asks the client to enter a valid name.
		 */
		while (!nameValidated) {
		    name = kb.nextLine().trim();
		    pw.println(encode(name));
		    pw.flush();
		    // Waits for IncomingHandler to check a signal.
		    Thread.sleep(1);
		}

		/*
		 * Stage 2, sends messages to the server until the client enters
		 * '-exit' to disconnect or the connection lost.
		 */
		String message = null;
		while (!finished) {
		    message = kb.nextLine().trim();
		    if (message.equals("-exit") || message == null) {
			// The client wants to finish the chat
			finished = true;
		    } else {
			// Otherwise sends message to the server
			pw.println(encode(message));
			pw.flush();
		    }
		}
	    } catch (InterruptedException e) {
		System.err.println("Problem found: " + e.getMessage());
	    } finally {
		close();
	    }
	}
    }
}
