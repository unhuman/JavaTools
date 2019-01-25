import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple listener on a port, logs what comes in
 *
 * Captain Obvious says, "This is not production ready"
 *
 */
public class SimplePortListener {
    private static final int DEFAULT_PORT = 8800;
    private static final long SLEEP_TIME_MS = 5;

    public static void main(String[] args) throws Exception {
        int port = (args.length == 1) ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Listener started - port: " + port);

        while (true)
        {
            try (Socket client = serverSocket.accept()) {
                // In theory, this could handle simultaneous requests
                // by running the processing below in a separate thread.

                System.out.println("** REQUEST START **");

                // Read in and echo out what comes in
                char[] buffer = new char[1024];
                BufferedReader requestReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                while (awaitReadyReceive(requestReader)) {
                    try {
                        int sizeRead = requestReader.read(buffer, 0, buffer.length);
                        if (sizeRead == 0) {
                            break;
                        }
                        String output = new String(buffer, 0, sizeRead);
                        System.out.print(output);
                    } catch (IOException ioe) {
                        System.err.println("Connection from client closed unexpectedly: " +
                                "(" + ioe.getClass().toString() + ") " + ioe.getMessage());
                        break;
                    }
                }
            } finally {
                System.out.println();
                System.out.println("** REQUEST COMPLETED **");
                System.out.println();
            }
        }
    }

    /**
     * Gives a little bit of an extra chance for data to come in over the wire
     * @param bufferedReader
     * @return true if there's data ready, false otherwise
     * @throws IOException
     * @throws InterruptedException
     */
    private static boolean awaitReadyReceive(BufferedReader bufferedReader) throws IOException, InterruptedException {
        if (bufferedReader.ready()) {
            return true;
        }

        Thread.sleep(SLEEP_TIME_MS);

        return bufferedReader.ready();
    }
}
