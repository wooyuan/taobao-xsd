import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * Sound File Downloader
 * For downloading success and failure sound files for barcode scanning
 */
public class SoundFileDownloader {

    static {
        // Setup trust all certificates
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Free sound file URLs
        String successSoundUrl = "https://samplelib.com/lib/preview/mp3/sample-15s.mp3";
        String failureSoundUrl = "https://samplelib.com/lib/preview/mp3/sample-3s.mp3";
        
        // Save paths
        String successFilePath = "src/main/resources/static/audio/success.mp3";
        String failureFilePath = "src/main/resources/static/audio/failure.mp3";
        
        try {
            System.out.println("Starting to download sound files...");
            
            // Download success sound
            downloadFile(successSoundUrl, successFilePath);
            System.out.println("Success sound file downloaded: " + successFilePath);
            
            // Download failure sound
            downloadFile(failureSoundUrl, failureFilePath);
            System.out.println("Failure sound file downloaded: " + failureFilePath);
            
            System.out.println("All sound files downloaded successfully!");
        } catch (IOException e) {
            System.err.println("Failed to download sound files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Download file from URL
     * @param urlString File URL
     * @param savePath Save path
     * @throws IOException IO Exception
     */
    private static void downloadFile(String urlString, String savePath) throws IOException {
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(savePath)) {
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }
}
