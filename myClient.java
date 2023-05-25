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

    public String[] recieveMessage() {
        String messageIn = null;
        try {
            messageIn = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(debug)System.out.println("RCVD: " + messageIn);
        return messageIn.split(" ");
    }

    public String[] sendRecieve(String messageOut) {
        sendMessage(messageOut);
        return recieveMessage();
    }

    public String[] firstJob;

    public void authenticate() {
        sendRecieve("HELO");
        sendRecieve("AUTH " + System.getProperty("user.name"));
        firstJob = sendRecieve("REDY");
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
        String[] server = null;

        int coresNeeded = Integer.parseInt(job[4]);
        int coresAvailable;
        int coresRemaining;
        int bestCores = 99999;

        String[] serverData = sendRecieve("GETS Capable " + job[4] + " " + job[5] + " " + job[6]);
        int numServers = Integer.parseInt(serverData[1]);

        sendMessage("OK");

        String[] currentServer;

        String[][] servers = new String[numServers][];

        for (int i = 0; i < numServers; i++) {
            servers[i] = recieveMessage();
        }

        sendRecieve("OK");

        for (int i = 0; i < numServers; i++) {
            currentServer = servers[i];
            coresAvailable = Integer.parseInt(currentServer[4]);
            coresRemaining = coresAvailable - coresNeeded;

            if (coresRemaining >= 0 && bestCores > coresRemaining) {
                server = currentServer;
                bestCores = coresRemaining;
            }
        }


        if (server == null) {
            System.out.println("ID " + job[2]);
            System.out.println("Cores " + job[4]);
            System.out.println("submitTime " + job[1]);

            for (int i = 0; i < numServers; i++) {
                String waitCount = sendRecieve("CNTJ " + servers[i][0] + " " + servers[i][1] + " 1")[0];

                if (Integer.parseInt(waitCount) == 0) {
                    server = servers[i];
                    System.out.println(server[0] + " " + server[1]);
                    return server;
                }
            }
        }

        return server;
    }

    public String[] getJob() {
        String[] job = sendRecieve("REDY");

        if (job[0].equals("NONE")) {
            finished = true;
            return job;    
        }

        if (job[0].equals("CHKQ")) {
            dequeueJobs();
        }

        while (!(job[0].equals("JOBN") || job[0].equals("JOBP"))) {
            job = sendRecieve("REDY");
            
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
                System.out.println("q");
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