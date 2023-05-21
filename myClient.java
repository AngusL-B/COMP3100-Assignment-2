import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class myClient {

    private Socket socket;
    private BufferedReader in;
    private DataOutputStream out;

    public myClient() {
        try {
            socket = new Socket("127.0.0.1", 50000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

    }
}