import javax.net.ssl.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyFactorySpi;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.security.PrivateKey;

public class Main {
	private static IO io = new IO();

    public static void main(String[] args) throws Exception {
		Map<String, String> env = EnvLoader.loadEnv();
		String keyPath = "server.keystore";
		String trustPath = "server.truststore";
		File keyFile = new File(keyPath);
		File trustFile = new File(trustPath);

		KeyStore keyStore = KeyStore.getInstance("JKS");
		KeyStore trustStore = KeyStore.getInstance("JKS");

		if (!trustFile.exists()) {
			io.print_flush("Truststore doesn't exist");
			trustStore.load(null, null);

			try (FileOutputStream fos = new FileOutputStream(trustFile)) {
				trustStore.store(fos, env.get("TRUST_STORE_PASSWORD").toCharArray());
			}
			io.print_flush("Truststore created");
        } else {
			io.print_flush("Truststore exists");
			try (FileInputStream trustStoreFile = new FileInputStream(trustFile)) {
				trustStore.load(trustStoreFile, env.get("TRUST_STORE_PASSWORD").toCharArray());
			}
			io.print_flush("Truststore loaded");
		}

		if (!keyFile.exists()) {
			io.print_flush("Keystore doesn't exist");
			keyStore.load(null, null);

			try (FileOutputStream fos = new FileOutputStream(keyFile)) {
				keyStore.store(fos, env.get("KEY_STORE_PASSWORD").toCharArray());
			}
			io.print_flush("Keystore created");
			String serverKeyPath = "server.key";
			File serverKeyFile = new File(serverKeyPath);
			File rootCaKeyFile = new File("rootCA.crt");
			File serverCAKeyFile = new File("server.crt");

			if (!serverKeyFile.exists() || !rootCaKeyFile.exists()) {
				io.print_flush("Server Key or rootCA doesn't exist");
			} else {
				io.print_flush("Server Key and root CA does exist");

				CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
				Certificate rootCA = null;
				Certificate serverCA = null;

				try (FileInputStream fis = new FileInputStream(rootCaKeyFile)) {
					rootCA = certFactory.generateCertificate(fis);
				} catch (Exception e) {
					io.print_flush("Failed to get certificate: " + e.getMessage());
				}

				try (FileInputStream fis = new FileInputStream(serverCAKeyFile)) {
					serverCA = certFactory.generateCertificate(fis);
				} catch (Exception e) {
					io.print_flush("Failed to get certificate: " + e.getMessage());
				}
				
				String key = new String(Files.readAllBytes(Paths.get(serverKeyPath)));
				key = key.replace("-----BEGIN PRIVATE KEY-----", "")
						.replace("-----END PRIVATE KEY-----", "")
						.replaceAll("\\s", "");
				

				byte[] keyBytes = Base64.getDecoder().decode(key);
				PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
				KeyFactory keyFactory = KeyFactory.getInstance("RSA");
				PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

				keyStore.setKeyEntry("server_private_key", privateKey, env.get("KEY_STORE_PASSWORD").toCharArray(), new Certificate[]{rootCA, serverCA});
				
				try (FileOutputStream keyStoreFile = new FileOutputStream(keyFile)) {
					keyStore.store(keyStoreFile, env.get("KEY_STORE_PASSWORD").toCharArray());
				}
			}
        } else {
			io.print_flush("Keystore exists");
			try (FileInputStream keyStoreFile = new FileInputStream(keyFile)) {
				keyStore.load(keyStoreFile, env.get("KEY_STORE_PASSWORD").toCharArray());
			}
			io.print_flush("Keystore loaded");
		}

		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X.509");
		trustManagerFactory.init(trustStore);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(keyStore, env.get("KEY_STORE_PASSWORD").toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

        SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(Integer.parseInt(env.get("PORT")));
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
                io.print_flush("Received: " + input);
                out.write("Echo: " + input + "\n");
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
