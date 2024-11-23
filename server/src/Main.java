import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.util.*;

public class Main {
	private static String keyPath = "server.keystore";
	private static IO io = new IO();

    public static void main(String[] args) throws Exception {
		Map<String, String> env = EnvLoader.loadEnv();
		File file = new File(keyPath);
		KeyStore keyStore = KeyStore.getInstance("JKS");

		if (!file.exists()) {
			io.print_flush("Keystore doesn't exist");
			keyStore.load(null, null);

			try (FileOutputStream fos = new FileOutputStream(keyPath)) {
				keyStore.store(fos, env.get("PASSWORD").toCharArray());
			}
        } else {
			io.print_flush("Keystore exists");
			try (FileInputStream keyStoreFile = new FileInputStream(keyPath)) {
				keyStore.load(keyStoreFile, env.get("PASSWORD").toCharArray());
			}
		}

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, env.get("PASSWORD").toCharArray());



        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(8443);
        io.print_flush("Secure server is listening on port " + env.get("PORT"));
		env.clear();
		env = null;

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
