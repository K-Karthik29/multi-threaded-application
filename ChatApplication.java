import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    // Server-side code
    static class ChatServer {
        private static final int PORT = 12345;
        private static final Set<PrintWriter> clientWriters = ConcurrentHashMap.newKeySet();

        public static void main(String[] args) {
            System.out.println("Chat server started...");
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    new ClientHandler(serverSocket.accept()).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static class ClientHandler extends Thread {
            private final Socket socket;
            private PrintWriter out;
            private BufferedReader in;
            private String clientName;

            public ClientHandler(Socket socket) {
                this.socket = socket;
            }

            public void run() {
                try {
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    out.println("Enter your name:");
                    clientName = in.readLine();
                    synchronized (clientWriters) {
                        clientWriters.add(out);
                    }

                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.equalsIgnoreCase("exit")) {
                            break;
                        }
                        System.out.println(clientName + ": " + message);
                        synchronized (clientWriters) {
                            for (PrintWriter writer : clientWriters) {
                                writer.println(clientName + ": " + message);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (clientName != null) {
                            System.out.println(clientName + " has left the chat.");
                        }
                        if (out != null) {
                            synchronized (clientWriters) {
                                clientWriters.remove(out);
                            }
                        }
                        if (socket != null) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Client-side code
    static class ChatClient {
        private static final String SERVER_ADDRESS = "localhost";
        private static final int SERVER_PORT = 12345;

        public static void main(String[] args) {
            try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 Scanner scanner = new Scanner(System.in)) {

                System.out.println("Connected to chat server.");
                System.out.print("Enter your name: ");
                String name = scanner.nextLine();
                out.println(name);

                Thread receiveThread = new Thread(new ReceiveMessages(in));
                receiveThread.start();

                String message;
                while (true) {
                    message = scanner.nextLine();
                    out.println(message);
                    if (message.equalsIgnoreCase("exit")) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static class ReceiveMessages implements Runnable {
            private final BufferedReader in;

            public ReceiveMessages(BufferedReader in) {
                this.in = in;
            }

            @Override
            public void run() {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
