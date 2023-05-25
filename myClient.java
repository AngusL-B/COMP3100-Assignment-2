import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class myClient {

    private Socket socket;
    private BufferedReader in;
    private DataOutputStream out;
    private static final boolean debug = false;

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
        if(debug)System.out.println("SEND: " + messageOut);
        try {
            out.write((messageOut + "\n").getBytes());
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
        if(debug)System.out.println("RCVD: " + messageIn);
        return messageIn;
    }

    public String sendRecieve(String messageOut) {
        sendMessage(messageOut);
        return recieveMessage();
    }

    public String[] firstJob;

    public void authenticate() {
        sendRecieve("HELO");
        sendRecieve("AUTH " + System.getProperty("user.name"));
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
        int coresNeeded = Integer.parseInt(job[4]);
        int lowestCoreCount = 9999;
        String[] server = null;

        String[] serversData = sendRecieve("GETS Avail " + job[4] + " " + job[5] + " " + job[6]).split(" ");

        sendMessage("OK");

        if (Integer.parseInt(serversData[1]) > 0) {
            // for (int i = 0; i < Integer.parseInt(serversData[1]); i++) {
            //     String[] currentServer = recieveMessage().split(" ");
            //     int coresLeft = Integer.parseInt(currentServer[4]) - coresNeeded;
            //     if (coresLeft >= 0 && coresLeft <= lowestCoreCount) {
            //         lowestCoreCount = coresLeft;
            //         server = currentServer;
            //     }
            // }
            server = recieveMessage().split(" ");
            for (int i = 1; i < Integer.parseInt(serversData[1]); i++) {
                recieveMessage();
            }
            sendRecieve("OK");

            if (Integer.parseInt(server[4]) == coresNeeded) {
                return server;
            }
        } else {
            recieveMessage();
        }

        serversData = sendRecieve("GETS Capable " + job[4] + " " + job[5] + " " + job[6]).split(" ");

        sendMessage("OK");

        String[][] servers = new String[Integer.parseInt(serversData[1])][];

        for (int i = 0; i < servers.length; i++) {
            servers[i] = recieveMessage().split(" ");
        }

        sendRecieve("OK");

        for (int i = 1; i <= servers.length; i++) {
            if (Integer.parseInt(servers[servers.length - i][4]) >= coresNeeded) {
                return servers[servers.length - i];
            }
        }

        return server;
    }

    public String[] getJob() {
        String[] job = sendRecieve("REDY").split(" ");

        if (job[0].equals("NONE")) {
            finished = true;
            return job;    
        }

        if (job[0].equals("CHKQ")) {
            dequeueJobs();
        }

        while (!(job[0].equals("JOBN") || job[0].equals("JOBP"))) {
            job = sendRecieve("REDY").split(" ");
            
            if (job[0].equals("NONE")) {
                finished = true;
                return job;
            }

            if (job[0].equals("CHKQ")) {
                dequeueJobs();
            }
        }

        return job;
    }

    public void enqueueJob(String[] job) {
        isQueued++;
        sendRecieve("ENQJ GQ");
    }

    public void dequeueJobs() {
        for (int i = 0; i < isQueued; i++) {
            sendRecieve("DEQJ GQ 0");
        }
        isQueued = 0;
    }

    private boolean finished = false;
    private int isQueued = 0;

    public void runAlgorithm() {
        String[] job = firstJob;
        String[] server;


        while (!finished) {
            server = getServer(job);
            if (server == null) {
                enqueueJob(job);
            } else {
                scheduleJob(job, server);
            }
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