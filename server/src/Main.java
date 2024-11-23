import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {
		Map<String, String> env = EnvLoader.loadEnv();

        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream keyStoreFile = new FileInputStream("server.keystore")) {
            keyStore.load(keyStoreFile, env.get("PASSWORD").toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, env.get("PASSWORD").toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(8443);

        System.out.println("Secure server is running on port 8443...");

        while (true) {
            SSLSocket socket = (SSLSocket) serverSocket.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private static void handleClient(SSLSocket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
            String input;
            while ((input = in.readLine()) != null) {
                System.out.println("Received: " + input);
                out.write("Echo: " + input + "\n");
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
