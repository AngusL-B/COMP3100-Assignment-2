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

    // Client Constructor, creates a new socket and assigns the input and output
    // stream from this socket to two variables
    public myClient() {
        try {
            socket = new Socket("127.0.0.1", 50000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Sends a given message to the server
    public void sendMessage(String messageOut) {
        if (debug)
            System.out.println("SEND: " + messageOut);
        try {
            out.write((messageOut + "\n").getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Reads in what the server has typed to the input buffer
    public String[] receiveMessage() {
        String messageIn = null;
        try {
            messageIn = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (debug)
            System.out.println("RCVD: " + messageIn);
        return messageIn.split(" ");
    }

    // Sends a given message to the server and returns the servers response
    public String[] sendRec(String messageOut) {
        sendMessage(messageOut);
        return receiveMessage();
    }

    public String[] firstJob;

    // Authenticates the client against the server
    // Also stores the first job that is accepted to a variable
    public void authenticate() {
        sendRec("HELO");
        sendRec("AUTH " + System.getProperty("user.name"));
        firstJob = sendRec("REDY");
    }

    // Closes the connection to the server
    public void quit() {
        sendRec("QUIT");
        try {
            in.close();
            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Schedules the given job to the given server
    // This function is mainly to make code easier to read in further sections
    public void scheduleJob(String[] job, String[] server) {
        sendRec("SCHD " + job[2] + " " + server[0] + " " + server[1]);
    }

    // Gets the server with the lowest amount of cores left after assigning current
    // job there, if it can fit
    // Otherwise it will check all capable servers to see if any have a local queue,
    // and assigns job to the first with no queued jobs
    public String[] getServerOnCores(String[] job) {
        String[] server = null;

        int coresNeeded = Integer.parseInt(job[4]);
        int coresAvailable;
        int coresRemaining;
        int bestCores = Integer.MAX_VALUE;

        String[] serverData = sendRec("GETS Capable " + job[4] + " " + job[5] + " " + job[6]);
        int numServers = Integer.parseInt(serverData[1]);

        sendMessage("OK");

        String[][] servers = new String[numServers][];

        for (int i = 0; i < numServers; i++) {
            servers[i] = receiveMessage();
        }

        sendRec("OK");

        // Compares all servers to find the server with the lowest coresRemaining value
        for (int i = 0; i < numServers; i++) {
            coresAvailable = Integer.parseInt(servers[i][4]);
            coresRemaining = coresAvailable - coresNeeded;

            if (coresRemaining >= 0 && coresRemaining < bestCores) {
                server = servers[i];
                bestCores = coresRemaining;
            }
        }

        // If no server can be found, the job is queued to the first server with no jobs
        // in its local queue
        // If there is no such queue, the function returns null
        if (server == null) {
            for (int i = 0; i < numServers; i++) {
                String waitCount = sendRec("CNTJ " + servers[i][0] + " " + servers[i][1] + " 1")[0];

                if (Integer.parseInt(waitCount) == 0) {
                    server = servers[i];
                    return server;
                }
            }
        }

        return server;
    }

    // Gets the first available Server else returns null
    public String[] getServerAvail(String[] job) {
        String[] server = null;

        String[] serverData = sendRec("GETS Avail " + job[4] + " " + job[5] + " " + job[6]);
        int numServers = Integer.parseInt(serverData[1]);

        // If numServers == 0 then no servers will be sent, only the final dot
        if (numServers > 0) {
            sendMessage("OK");

            server = receiveMessage();

            // Clears input buffer
            for (int i = 1; i < numServers; i++) {
                receiveMessage();
            }
        }

        sendRec("OK");

        return server;
    }

    // Recieves jobs from job queue
    // This function will:
    // exit if NONE is returned from server
    // dequeue if CHKQ is returned from server 
    // return value if JOBN or JOBP is returned from server
    public String[] getJob() {
        String[] job = { "" };

        while (!(job[0].equals("JOBN") || job[0].equals("JOBP"))) {
            job = sendRec("REDY");

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

    // Adds a job to the global queue
    public void enqueueJob(String[] job) {
        isQueued++;
        sendRec("ENQJ GQ");
    }

    // Removes all jobs from the global queue
    public void dequeueJobs() {
        for (int i = 0; i < isQueued; i++) {
            sendRec("DEQJ GQ 0");
        }
        isQueued = 0;
    }

    private boolean finished = false;
    private int isQueued = 0;

    // Scheduling algorithm
    public void runAlgorithm() {
        String[] job = firstJob;
        String[] server;

        while (!finished) {
            // Get first Available Server that can complete Job
            server = getServerAvail(job);

            // If not get first Server that has the cores to run the job
            // or get first server that does not have a job queued in its local queue
            if (server == null)
                server = getServerOnCores(job);

            // If there is still no server that fits these conditions, queue the job to the
            // global queue
            // Else schedule the job to the designated Server
            if (server == null)
                enqueueJob(job);
            else
                scheduleJob(job, server);

            // Fetch a new job
            job = getJob();
        }
    }

    // Main line where code is run from
    public static void main(String[] args) {
        myClient client = new myClient();
        client.authenticate();

        client.runAlgorithm();

        client.quit();
    }
}