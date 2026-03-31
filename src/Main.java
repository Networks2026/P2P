import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Map<Integer, PeerConfigData> peerConfig = PeerConfigData.readInData("TestPeerInfo.cfg");

        for (Entry<Integer, PeerConfigData> entry : peerConfig.entrySet()) {
            Thread thread = new Thread() {
                public void run() {
                    try {
                        TimeUnit.MILLISECONDS.sleep(1000);
                        ProcessBuilder processBuilder = new ProcessBuilder(
                                "java", "peerProcess", String.valueOf(entry.getValue().peerId()));
                        processBuilder.redirectErrorStream(true);
                        Process process = processBuilder.start();

                        BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = r.readLine()) != null) {
                            System.out.println(line);
                        }
                        r.close();
                    } catch (IOException e) {
                        System.err.println("ERROR: " + e.getMessage());
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            thread.start();
        }
    }
}
