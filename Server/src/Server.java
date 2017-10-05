import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * ****************************
 * Filename: Server.java 
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
 * This class defines a Server that can
 * <ul>
 * <li>Listen for new connections from clients.</li>
 * <li>Handle connections from multiple clients.</li>
 * <li>Respond to client requests.</li>
 * <li>Broadcast chat message to all clients.</li>
 * </ul>
 */
public class Server {

    /**
     * The code used in the encryption and decryption of messages.
     */
    private static final int CODE = 20;

    /**
     * The Port Number of the server.
     */
    private static final int PORT = 12345;

    /**
     * The IP Address of the server.
     */
    private static String serverIP;

    /**
     * socket: The server socket.
     */
    private static ServerSocket ss;

    /**
     * <p>
     * The thread pool used to hold new threads.
     * </p>
     * A thread pool is able to run and control threads efficiently and safely,
     * especially for the case in which a large amount of clients connect to the
     * server at the same time.
     */
    private static ExecutorService exec;

    /**
     * The start time of the server.
     */
    private static long startTime;

    /**
     * <p>
     * The Map storing each user's name(key) and its corresponding
     * PrintWriter(value).
     * </p>
     * In the multi-threading environment, A CurrentHashMap can perform more
     * safely and efficiently than a HashMap.
     */
    private static ConcurrentHashMap<String, PrintWriter> clients;

    /**
     * The block list in which each user (name) is a Key while each Value is the
     * set of users (name) that have been blocked by this user.
     */
    private static ConcurrentHashMap<String, HashSet<String>> blockList;

    /**
     * <P>
     * The Administrator Password. 
     * </P>
     * For the sake of security, every time the server restarts, it will
     * generate a new random 4-digit password on its screen.
     */
    private static int adminPassword;

    /**
     * The set of names of Administrators.
     */
    private static HashSet<String> admins;
    
    /**
     * A separator.
     */
    private final static String SEPARATOR = "------------------------------------------------------------------";

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
     * Gets the current time in a specific format.
     * 
     * @return the current time in the format of [HH:mm:ss]
     */
    public static String getCurrentTime() {
	// Convert the current time into the format of [HH:mm:ss]
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	String currentTime = sdf.format(System.currentTimeMillis());
	return "[" + currentTime + "] ";
    }
    
    /**
     * Main method: start the server.
     * 
     * @param args
     *            command-line arguments
     * @throws UnknownHostException
     *             may occur if local host name cannot be resolved into an
     *             address.
     */
    public static void main(String[] args) throws UnknownHostException {

	/* Creates a new CahedThreadPool to control threads and handle tasks. */
	exec = Executors.newCachedThreadPool();

	/* Records the start time of the server. */
	startTime = System.currentTimeMillis();

	/*
	 * Finds out the IP Address of the server. Assumes that it will not
	 * change during the server is running.
	 */
	serverIP = InetAddress.getLocalHost().getHostAddress();

	/*
	 * Creates a new ConcurrentHashMap to store user names and their
	 * PrintWriters.
	 */
	clients = new ConcurrentHashMap<String, PrintWriter>();

	/*
	 * Creates a new ConcurrentHashMap to store user names and their block
	 * lists.
	 */
	blockList = new ConcurrentHashMap<String, HashSet<String>>();

	/* Creates a new HashSet to store the names of Administrators. */
	admins = new HashSet<String>();

	/*
	 * For the sake of security, every time the server restarts, it will
	 * generate a new 4-digit Administrator Password randomly and display it
	 * on its screen.
	 */
	adminPassword = (int) (Math.random() * 9000) + 1000;

	try {
	    // Creates a new ServerSocket with a given port (12345).
	    ss = new ServerSocket(PORT);
	    
	    // Shows important information about the server
	    System.out.println(getCurrentTime() + "Server starts to run.");
	    System.out.println(getCurrentTime() + "IP Address: " + serverIP);
	    System.out.println(getCurrentTime() + "Port number: " + PORT);
	    System.out.println(getCurrentTime() + "Administrator Password: " + adminPassword);
	    
	    Socket incoming;
	    while (true) {
		incoming = ss.accept();
		// adds a new task to the thread pool
		exec.execute(new ThreadHandler(incoming));
	    }
	} catch (IOException ioe) {
	    /*
	     * The socket may be closed by shutDown() method so we do not need
	     * to handle the exception caused by the close of socket, but we do
	     * need to handle other I/O exceptions.
	     */
	    if (!ss.isClosed()) {
		System.err.println("I/O Error: " + ioe.getMessage());
		System.exit(1);
	    }
	}
    }

    /**
     * <p>
     * A ThreadHandler receives messages or commands from a client and make
     * responses.
     * </p>
     * <p>
     * Before a client can enter the chat room, he/she will be required to enter
     * a user name. After the ThreadHandler validates the name, a feedback will
     * be given to the client and he/she will be allowed to chat with others.
     * </p>
     */
    private static class ThreadHandler implements Runnable {

	/**
	 * The client socket.
	 */
	private Socket client;

	/**
	 * Reads text from InputStream.
	 */
	private BufferedReader in;

	/**
	 * Prints text to OutputStream.
	 */
	private PrintWriter out;

	/**
	 * The name of the client.
	 */
	private String userName;

	/**
	 * The time at which the client enters the chat room.
	 */
	private long clientStartTime;

	/**
	 * Has the name of client been validated? <br>
	 * In this chat room, only the clients whose names have been validated
	 * are considered formal users.
	 */
	private boolean nameValidated;

	/**
	 * Has the client decided to finish the chat?
	 */
	private boolean finished = false;

	/**
	 * Creates a new instance of ThreadHandler.
	 * 
	 * @param s
	 *            the client socket
	 */
	private ThreadHandler(Socket s) {
	    this.client = s;
	}

	/**
	 * Asks the client to enter a valid name. <br>
	 * Broadcasts chat messages to all clients. <br>
	 * Responds to client requests.<br>
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

	    // The input from client
	    String clientInput = null;

	    try {
		// Sets up I/O
		in = new BufferedReader(new InputStreamReader(client.getInputStream()));
		out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));

		feedback("Connection has been built successfully.");

		// Asks the client to enter a user name
		feedback("Please enter a user name:");

		// Stage 1: validate the user name sent by the client and give a
		// feedback
		while (!nameValidated) {

		    // Reads the input from the client
		    userName = encode(in.readLine()).trim();

		    // Uses lock in case that two clients enter the same name at the same time 
		    synchronized (this) {
			
			if (userName.length() > 0 && !clients.containsKey(userName)) {
			    /*
			     * If the name is not empty and has not been used by
			     * others, it is valid
			     */
			    nameValidated = true;

			    /*
			     * Puts the client's PrintWriter into the HashMap,
			     * using his/her name as the key
			     */
			    clients.putIfAbsent(userName, out);

			    /*
			     * Creates an empty block list(HashSet) for the new
			     * client
			     */
			    blockList.putIfAbsent(userName, new HashSet<String>());

			    // Records the time at which the client enters the
			    // chat room
			    clientStartTime = System.currentTimeMillis();

			    /*
			     * Sends a special signal to the client program to
			     * let it move on. The client will not see this
			     * signal.
			     */
			    feedback("[Valid]");

			    // Informs the client that his/her name is validated
			    feedback("Your user name is " + userName + ".");

			    // Sends welcome messages to the client
			    feedback("Enter '-h' to see the list of request commands.");
			    feedback("Enter '-exit' to disconnect.\n" + SEPARATOR);
			    feedback("You can now chat with others.");

			    // Sends notifications to all other clients
			    broadcast(userName + " has entered the chat room.", "Server");

			} else {

			    if (userName.length() == 0) {
				// If the name is empty, it is not valid
				feedback("User name should be empty.");
			    }

			    if (clients.containsKey(userName)) {
				// If the name already exists, it is not valid
				feedback("'" + userName + "' already exists. ");
			    }

			    // Asks the client to try another name
			    feedback("Try again: ");
			}
		    }

		}

		// Stage 2: broadcast messages/respond to requests from the client
		while (!finished) {

		    // Reads input from the client
		    clientInput = encode(in.readLine());

		    if (clientInput == null) {
			// null means the client disconnected abruptly
			finished = true;

		    } else if (clientInput.length() == 0) {
			// The client is not allowed to send an empty message
			feedback("You are not allowed to send an empty message.");

		    } else if (clientInput.startsWith("-")) {
			// Any message starting with "-" will be treated as a
			// request command
			processCommand(clientInput);

		    } else {
			// Else, broadcasts the message to all other clients
			broadcast(clientInput, userName);
		    }
		}
	    } catch (IOException e) {
		// Prints out error messages if exception occurs
		if (nameValidated){
		    System.err.println(getCurrentTime() + userName + ": " + e.getMessage());
		}
	    } finally {

		/*
		 * For clients who failed to select a valid user name, we assume
		 * they have never entered the chat room, so print out no
		 * messages when they disconnect.
		 */
		if (nameValidated == true) {

		    // Removes the client from client lists
		    clients.remove(userName, out);

		    // Removes his/her block list
		    blockList.remove(userName);

		    // If he/she is an administrator, removes he/she from the Administrator list
		    admins.remove(userName);

		    // Sends notifications to other clients
		    broadcast(userName + " has left the chat room.", "Server");
		}

		// Closes the socket and streams
		try {
		    in.close();
		    if (out != null) {
			out.flush();
			out.close();
		    }
		    if (client != null) {
			client.close();
		    }
		} catch (IOException e) {
		    System.err.println("closing: " + e.getMessage());
		}
	    }
	}

	/**
	 * Processes the request commands and call the corresponding methods to
	 * deal with them.
	 * 
	 * @param userInput
	 *            the whole command, starting with "- "
	 */
	public void processCommand(String userInput) {

	    // Complex commands (require parameters)
	    if (userInput.startsWith("-block ")) {
		// The client wants to block someone
		block(userInput);

	    } else if (userInput.startsWith("-unblock ")) {
		// The client wants to unblock someone
		unBlock(userInput);

	    } else if (userInput.startsWith("-private ")) {
		// The client wants to send a private message to someone
		privateMsg(userInput);

	    } else if (userInput.startsWith("-admin ")) {
		// The client wants to become an Administrator
		verifyAdmin(userInput);

	    } else if (userInput.startsWith("-kick ")) {
		// The client wants to kick someone out of the chat room
		kick(userInput);

	    } else {
		
		// Simple commands (need no parameters)
		switch (userInput) {

		// The client asks for the list of request commands
		case "-h": {
		    showHelp();
		    break;
		}

		// The client asks how long he/she has been in the chat room for
		case "-ct": {
		    getStayingTime();
		    break;
		}

		// The client asks how long the server has been running for
		case "-st": {
		    getRunningTime();
		    break;
		}

		// The client asks the server's IP Address
		case "-sip": {
		    getServerIP();
		    break;
		}

		// The client asks how many clients are connected to the chat room
		case "-num": {
		    getClientsNumber();
		    break;
		}

		// The client wants to clear his/her screen
		case "-cls": {
		    clearScreen();
		    break;
		}

		// The client sends a disconnect request.
		case "-exit": {
		    finished = true;
		    break;
		}

		// The client wants to shut down the server
		case "-shutdown": {
		    shutDown();
		    break;
		}
		
		// does not match any command
		default: {
		    feedback(userInput + " is not recognized as a command.");
		    feedback("You may enter '-h' for help.");
		}
		}
	    }
	    return;
	}

	/**
	 * Shows all the request commands the client can send to the server.
	 * <br>
	 * If the client is an Administrator, he/she will have two extra commands.
	 */
	public void showHelp() {
	    // The formats of all the request commands are on the left hand side
	    out.println(encode(SEPARATOR + "\nThe list of request commands:"));
	    out.println(encode("-h                  Display the list of request commands"));
	    out.println(encode("-sip                Display the server's IP Address"));
	    out.println(encode("-num                Display the number of people in the chat room"));
	    out.println(encode("-st                 Display how long the server has been running"));
	    out.println(encode("-ct                 Display how long you have been here"));
	    out.println(encode("-block name         Block all the messages from another user"));
	    out.println(encode("-unblock name       Unblock a user"));
	    out.println(encode("-private name: msg  Send a private message to another user"));
	    out.println(encode("-cls                Clear screen"));
	    out.println(encode("-exit               Disconnect and exit"));
	    // If the client is an administrator, he/she has two extra commands
	    if (admins.contains(userName)) {
		out.println(encode("-kick name          Kick a user out of the chat room"));
		out.println(encode("-shutdown           shut down the server."));
	    } else {
		// If not, he can become an administrator with the command "-admin password"
		out.println(encode("-admin password     Enter the password to become an Administrator")); 
	    }
	    out.println(encode(SEPARATOR));
	    out.flush();
	}

	/**
	 * Gets the past time between a start time and now.
	 * 
	 * @param startTime
	 *            a start time
	 * @return the past time in a specific format
	 */
	public String getPastTime(long startTime) {
	    // Converts milliseconds to seconds
	    long passTimeSeconds = (System.currentTimeMillis() - startTime) / 1000;

	    // Calculates the past days, hours, minutes and seconds
	    long days = passTimeSeconds / (24 * 3600);
	    long hours = passTimeSeconds % (24 * 3600) / 3600;
	    long minutes = (passTimeSeconds % (24 * 3600) % 3600) / 60;
	    long seconds = (passTimeSeconds % (24 * 3600) % 3600) % 60;

	    // Returns the past time in the format of x day(s) x hour(s) x
	    // minute(s) x second(s)
	    return days + " day(s) " + hours + " hour(s) " + minutes + " minute(s) " + seconds + " second(s)";
	}

	/**
	 * (The Server) gives a feedback to the client who sends a request to it.
	 * 
	 * @param content
	 *            the message the server wants to send to the client
	 */
	public void feedback(String content) {
	    /*
	     * Appends "[Server] " before the content to let the client know
	     * this a message from the server.
	     */
	    out.println(encode("[Server] " + content));
	    out.flush();
	}

	/**
	 * <p>
	 * Broadcasts a message to all other clients.
	 * </p>
	 * There are two kinds of broadcasts, one is the server's broadcast and
	 * another is the client's broadcast. <br>
	 * 
	 * @param content
	 *            the content to be broadcast
	 * @param sender
	 *            "Server" or a client
	 */
	public void broadcast(String content, String sender) {
	    String message = "";

	    // If the sender is the server
	    if (sender.equals("Server")) {
		message = "[Server] " + content;
		for (PrintWriter writer : clients.values()) {
		    if (!writer.equals(out)) {
			// Sends messages to all other clients
			writer.println(encode(message));
			writer.flush();
		    }
		}
		// Prints out events on server's screen
		System.out.println(getCurrentTime() + content);
	    } else {
		// If the sender is a client		
		for (Map.Entry<String, PrintWriter> entry : clients.entrySet()){
		    String receiver = entry.getKey();
		    // Check whether the sender is blocked by the receiver
		    if (!blockList.get(receiver).contains(sender)){
			if (!receiver.equals(sender)){
			    // Message sent to others
			    message = getCurrentTime() + sender + ": " + content;
			} else {
			    // Feedback given to the sender
			    message = getCurrentTime() + sender + "(You): " + content;
			}
			PrintWriter writer = entry.getValue();
			writer.println(encode(message));
			writer.flush();
		    }
		    // If the sender is blocked by this receiver, do not send to him/her
		}
	    }
	}

	/**
	 * <p>
	 * Shows the number of people who are currently connected to the chat
	 * room.
	 * </p>
	 * Notice that clients who have connected to the server but have not
	 * entered valid names are not taken into count because we assume that
	 * they have not entered the chat room.
	 */
	public void getClientsNumber() {
	    feedback("The number of people in the chat room is " + clients.size());
	}

	/**
	 * Shows the server's IP Address.
	 */
	public void getServerIP() {
	    feedback("The server's IP Address is " + serverIP);
	}

	/**
	 * Shows how long the server has been running for.
	 */
	public void getRunningTime() {
	    feedback(("The server has been running for " + getPastTime(startTime)));
	}

	/**
	 * Shows how long he/she has been in the chat room for.
	 */
	public void getStayingTime() {
	    feedback("You have been staying here for " + getPastTime(clientStartTime));
	}

	/**
	 * <p>
	 * Blocks all the messages from another client with his/her name.
	 * </p>
	 * The client can also {@linkplain #unBlock(String) unblock} another
	 * client.
	 * 
	 * @param userInput
	 *            the whole command, '-block name'
	 */
	public void block(String userInput) {

	    // Gets the name which the client wants to block
	    String blockName = userInput.substring(7);

	    if (blockName.equals(userName)) {
		// Cannot not block oneself
		feedback("You cannot block yourself.");
		return;
	    }

	    if (clients.containsKey(blockName)) {		
		// Adds the name to the current client's block list		 
		blockList.get(userName).add(blockName);
		feedback("You will no longer receive the messages from " + blockName + ".");
	    } else {
		// If the target client does not exist
		feedback("Failed. Cannot find a user named " + blockName + ".");
	    }
	}

	/**
	 * Unblocks another client (receive messages from him/her again).
	 * 
	 * @param userInput
	 *            the whole command, '-unblock name'
	 */
	public void unBlock(String userInput) {

	    // Gets the name which the client wants to unblock
	    String unBlockName = userInput.substring(9);

	    if (clients.containsKey(unBlockName)) {
		// Removes the name from the current client's block list
		blockList.get(userName).remove(unBlockName);
		feedback("You will now receive messages from " + unBlockName + ".");
	    } else {
		// If the target client does not exist
		feedback("Failed. Cannot find a user named " + unBlockName + ".");
	    }
	}

	/**
	 * <p>
	 * Sends a private message to another client.
	 * </p>
	 * <p>
	 * The message is only visible to the receiver and the sender.
	 * </p>
	 * 
	 * @param userInput
	 *            the whole command, "-private name: message"
	 */
	public void privateMsg(String userInput) {
	    try {
		// Gets the name of the target receiver
		String receiver = userInput.substring(9,userInput.indexOf(":"));
		// Gets the message to be sent
		String message = userInput.substring(userInput.indexOf(":")+1).trim();
		if (!blockList.containsKey(receiver)) {
		    // The target client does not exist
		    feedback("Failed. Cannot find a user named " + receiver + ".");
		} else if (receiver.equals(userName)) {
		    // Cannot send a private message to oneself
		    feedback("You are not allowed to send a private message to yourself.");
		} else {
		    if (blockList.get(receiver).contains(userName)) {
			// If the client has been blocked by the target receiver
			feedback("Failed. You are blocked by " + receiver + ".");
		    } else {
			// Creates a private message with a fixed format
			String finalMsg = getCurrentTime() + userName + ": " + message + " [Private Message]";
			// Sends it to the target receiver
			clients.get(receiver).println(encode(finalMsg));
			clients.get(receiver).flush();
			// Gives a feedback to the client(sender)
			feedback("You've sent a private message to " + receiver + ".");
		    }
		}
	    } catch (Exception e) {
		// If the command is not in the correct format
		feedback("Failed. Invalid format.");
		feedback("Valid Format: '-private name: message'.");
	    }
	}

	/**
	 * Clears the client's screen by simply printing out 50 empty lines.
	 */
	public void clearScreen() {
	    for (int i = 0; i < 50; i++) out.println();
	    out.flush();
	}

	/**
	 * Verifies the password entered by the client.
	 * 
	 * @param userInput
	 *            the whole command, '-admin password'
	 */
	public void verifyAdmin(String userInput) {
	    if (userInput.equals("-admin " + adminPassword)) {
		// Password matches
		admins.add(userName);
		feedback("You are now an Administrator.");
		feedback("Enter '-h' to see your extra commands.");
		// Sends notifications to other clients
		broadcast(userName + " has become an Administrator.", "Server");
	    } else {
		// The given password is incorrect
		feedback("Wrong password.");
	    }
	}

	/**
	 * <p>
	 * Kicks a user out of the chat room, which is one of the privileges of
	 * Administrator.
	 * </p>
	 * <p>
	 * To use it, the client needs to first {@linkplain #verifyAdmin(String)
	 * become an Administrator}.
	 * </p>
	 * 
	 * @param userInput
	 *            the whole command, '-kick name'
	 */
	public void kick(String userInput) {
	    // The name to be kicked is after "-kick "
	    String kickedUser = userInput.substring(6);
	    // Checks whether the client is an Administrator
	    if (admins.contains(userName)) {
		// Only the Administrator have the right to kick others
		PrintWriter target = clients.get(kickedUser);
		if (target == null) {
		    // Target client does not exist
		    feedback("Failed. Cannot find a user named " + kickedUser + ".");
		} else {
		    // Cannot kick another administrator
		    if (admins.contains(kickedUser)) {
			feedback("Failed. Cannot kick out another Administrator.");
		    } else {
			/*
			 * Sends a recognisable signal to the target client then
			 * his/her client program will exit.
			 */
			target.println(encode("[Server] [Kicked Out]"));
			target.flush();
			broadcast(kickedUser + " is kicked out of the chat room by " + userName, "Server");
		    }
		}
	    } else {
		// The client is not an Administrator
		feedback("Failed. You are not an Administrator.");
	    }
	}

	/**
	 * Shuts down the server, which is a privilege of the Administrator.<br>
	 * <p>
	 * To use it, the client needs to first {@linkplain #verifyAdmin(String)
	 * become an Administrator}.
	 * </p>
	 */
	public synchronized void shutDown() {

	    // Checks whether the client is an Administrator
	    if (admins.contains(userName)) {
		try {
		    // Sends notifications to all other clients
		    broadcast("Server is shut down by " + userName, "Server");
		    // Sends a feedback to the client who made the request
		    feedback("You have shut down the server.");
		    ss.close();
		} catch (IOException e) {
		    System.err.println("There is a problem shutting down the server:" + e.getMessage());
		} finally {
		    System.exit(0);
		}
	    } else {
		// If not, abort the action
		feedback("Failed. You are not an Administrator.");
	    }
	}

    }

}
