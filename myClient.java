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

    public void sendMessage(String messageOut) {
        try {
            out.writeUTF(messageOut + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String recieveMessage() {
        String messageIn = "";
        try {
            messageIn = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(messageIn);
        return messageIn;
    }

    public String sendRecieve(String messageOut) {
        sendMessage(messageOut);
        return recieveMessage();
    }

    public String[] firstJob;

    public void authenticate() {
        sendRecieve("HELO");
        sendRecieve("AUTH" + System.getProperty("user.name"));
        firstJob = sendRecieve("REDY").split(" ");
    }

    public void quit() {
        sendRecieve("QUIT");
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scheduleJob(String[] job, String[] server) {
        sendRecieve("SCHD " + job[2] + " " + server[0] + " " + server[1]);
    }

    public String[] getServer(String[] job) {
        
        return new String[0];
    }

    public String[] getJob() {
        String[] job = sendRecieve("REDY").split(" ");
        if (job[0].equals("NONE")) {
            finished = true;
            return job;
        }

        while (!job[0].equals("JOBN")) {
            job = sendRecieve("REDY").split(" ");
            if (job[0].equals("NONE")) {
                finished = true;
                return job;
            }
        }
        return job;
    }

    private boolean finished = false;

    public void runAlgorithm() {
        String[] job = firstJob;
        String[] server;


        while (!finished) {
            server = getServer(job);
            scheduleJob(job, server);
            job = getJob();
        }
    }

    public static void main(String[] args) {
        myClient client = new myClient();
        client.authenticate();

        client.runAlgorithm();

        client.quit();
    }
}