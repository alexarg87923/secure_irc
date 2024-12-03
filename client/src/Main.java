import java.io.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.util.Map;
import javax.net.ssl.*;
import java.security.cert.Certificate;

public class Main {
	public static void main(String[] args) {
		IO io = new IO();
		Map<String, String> env = EnvLoader.loadEnv();
		String keyPath = "client.keystore";
		String trustPath = "client.truststore";
		File keyFile = new File(keyPath);
		File trustFile = new File(trustPath);

		try {
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
					trustStore.load(trustStoreFile, null);
				}
				io.print_flush("Truststore loaded");
			}

			if (!keyFile.exists()) {
				io.print_flush("Keystore doesn't exist");
				keyStore.load(null, null);

				try (FileOutputStream fos = new FileOutputStream(keyPath)) {
					keyStore.store(fos, env.get("KEY_STORE_PASSWORD").toCharArray());
				}
				io.print_flush("Keystore created");
			} else {
				io.print_flush("Keystore exists");
				try (FileInputStream keyStoreFile = new FileInputStream(keyPath)) {
					keyStore.load(keyStoreFile, env.get("KEY_STORE_PASSWORD").toCharArray());
				}
				io.print_flush("Keystore loaded");
			}

			String alias = "ca_server_public";
			if (trustStore.containsAlias(alias)) {
				io.print_flush("Server public certificate exists");
			} else {
				io.print_flush("Server public certificate doesn't exist");
				String server_ca = alias+".crt";

				CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
				try (FileInputStream fis = new FileInputStream(server_ca)) {
					Certificate cert = certFactory.generateCertificate(fis);
					trustStore.setCertificateEntry(alias, cert);
					io.print_flush("Certificate added with alias: " + alias);

					try (FileOutputStream fos = new FileOutputStream(trustFile)) {
						trustStore.store(fos, env.get("TRUST_STORE_PASSWORD").toCharArray());
						io.print_flush("TrustStore updated successfully with server's public cert.");
					}
				} catch (Exception e) {
					io.print_flush("Failed to add certificate: " + e.getMessage());
					e.printStackTrace();
				}
			}

			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X.509");
			trustManagerFactory.init(trustStore);

			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(keyStore, env.get("KEY_STORE_PASSWORD").toCharArray());

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

			SSLSocketFactory socketFactory = sslContext.getSocketFactory();
			SSLSocket socket = (SSLSocket) socketFactory.createSocket(env.get("IP"), Integer.parseInt(env.get("PORT")));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			writer.write("Test");
			writer.flush();


            String response = reader.readLine();
            io.print_flush("Server" + response);

            socket.close();
        } catch (Exception e) {
            io.print_flush("Error: " + e.getMessage());
        }

		io.close();
		return;
    }
}
