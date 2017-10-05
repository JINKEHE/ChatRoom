import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * ****************************
 * Filename: ClientMain.java 
 * Student Name: Jinke He
 * University ID: 201219022 
 * Departmental ID: x6jh 
 * ****************************
 */

/**
 * This class contains a main method that will instantiate a new
 * {@link ClientInstance}.
 */
public class ClientMain {

    /**
     * <code>port</code> is the port number of the server which user wishes to
     * connect to.
     */
    private static int port;

    /**
     * <code>kb</code> is a {@link Scanner} used to read input from user.
     */
    private static Scanner kb;

    /**
     * <p>
     * Instantiates a new {@linkplain ClientInstance Client Instance}.
     * </p>
     * <p>
     * Firstly, prompts the client for a IP Address and validates it with a
     * Regular Expression {@code validIP}.
     * </p>
     * <p>
     * Secondly, prompts the client for a Port number and validates it with a
     * valid range [1024,65535].
     * </p>
     * <p>
     * If {@code targetIP} and {@code port} are valid, the program will try to
     * build a connection with the target server.
     * </p>
     * <p>
     * If any validation fails, the program will exit with an error message.
     * </p>
     * 
     * @param args
     *            the supplied command-line arguments as an array of String
     *            objects
     */
    public static void main(String[] args) {

	System.out.println(ClientInstance.SEPARATOR + "\nWelcome to Jinke He's chat room.\n");

	// Instantiates the Scanner
	kb = new Scanner(System.in);

	// Prompts user for a IP Address
	System.out.print("Please enter the IP address you wish to connect to: ");
	String targetIP = kb.nextLine();

	/*
	 * Validates the input IP Address
	 */
	if (!validateIP(targetIP)) {
	    System.out.println("Invalid IP Address, program exits.");
	    return;
	}

	// Prompts user for a port number
	System.out.print("Please enter the port number: ");
	String targetPort = kb.nextLine();

	/*
	 * Validates the input port number
	 */
	if (!validatePort(targetPort)) {
	    System.out.println("Invalid port number, program exits.");
	    return;
	}

	// Instantiates a new ClientInstance
	new ClientInstance(targetIP, port);
    }

    /**
     * Validates a IP Address with a Regular Expression of IP Address.
     * 
     * @param targetIP
     *            the target IP Address entered by the client
     * @return <tt>true</tt>, if and only if, the given IP Address matches the
     *         regular expression or it is 'localhost'
     */
    public static boolean validateIP(String targetIP) {

	// "localhost" is a valid IP Address
	if (targetIP.equals("localhost")) {
	    return true;
	}

	// Returns false if the length of the given IP Address is incorrect
	if (targetIP.length() < 7 || targetIP.length() > 15 || "".equals(targetIP)) {
	    return false;
	}

	// The regular expression of a valid IP Address
	String validIP = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
	Pattern pat = Pattern.compile(validIP);
	Matcher mat = pat.matcher(targetIP);

	// true if and only if the given IP Address matches the regular expression
	return mat.find();
    }

    /**
     * Validates a port number with a acceptable range [1024,65535]
     * 
     * @param targetPort
     *            the target port number entered by the client, which is a String
     * @return <tt>true</tt>, if and only if, {@code targetPort} can be parsed
     *         into a number which is between 1024 and 65535
     */
    public static boolean validatePort(String targetPort) {
	try {
	    port = Integer.parseInt(targetPort);
	    // Conventionally, port numbers smaller than 1024 are reserved for system services.
	    if (port < 1024 || port > 65535) 
		return false;
	} catch (NumberFormatException e) {
	    // Unable to parse the given String into a number
	    return false;
	}
	return true;
    }
}
