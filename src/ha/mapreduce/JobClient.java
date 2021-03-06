package ha.mapreduce;

import ha.DFS.DataNode;
import ha.DFS.NameNodeInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * used by user(client) to submit and monitoring jobs
 * @author hanz&amos
 *
 */
public class JobClient {

  private JobConf jconf;

  private int JobID;

  public JobClient(JobConf conf) {
    this.jconf = conf;
  }

  /**send the jobconf to master
   * @throws IOException
   * @throws InterruptedException
   * @throws ClassNotFoundException
   * 
   */
  private void sendConf() throws IOException, InterruptedException, ClassNotFoundException {
    String masterAddress = jconf.getMaster().getHostName();
    Integer masterPort = jconf.getMaster().getPort();
    System.out.println("[CLIENT] Connecting to master at " + masterAddress + ":" + masterPort
            + "...");
    Socket s = new Socket(masterAddress, masterPort);
    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
    ObjectInputStream newJobsStream = new ObjectInputStream(s.getInputStream());

    System.out.println("[CLIENT] Submitting job config...");
    oos.writeObject(jconf);

    System.out.println("[CLIENT] Waiting for job id...");
    JobID = newJobsStream.readInt();
    System.out.print("JobID is:");
    System.out.println(JobID);

    Thread.sleep(500);
    newJobsStream.close();
    oos.close();
    s.close();
  }

  /**
   * Send job over to master node and listen for
   */
  private void submitJob(JobConf conf) {

    /*
     * get output configuration, compute input split
     */
    try {
      sendConf();
    } catch (Exception e) {
      System.out.println("error in submitting job");
      e.printStackTrace();
    }

  }
  /**
   * keep ask for information about submitted job
   * @param jt
   * @throws RemoteException
   */
  private void getUpdates(JobTrackerInterface jt) throws RemoteException {
    while (true) {
      // poll for job status
      System.out.println(jt.getStatuses());
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        System.err.println("[CLIENT] Cannot sleep thread!");
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) throws RemoteException, NotBoundException {
    if (args.length != 2) {
      System.out
              .println("USAGE: java ha.mapreduce.JobClient <conf file> <client address>:<client port>");
      System.exit(0);
    }

    JobConf conf = null;
    try {
      conf = new JobConf(args[0]);
    } catch (IOException e1) {
      System.err.println("Cannot read configuration file!");
      e1.printStackTrace();
      System.exit(1);
    }
    System.out.println("[CLIENT] Setting up new job as such:");
    System.out.println(conf);
    JobClient client = new JobClient(conf);

    System.out.println("[CLIENT] Submitting input file to DFS");
    InetSocketAddress thisMachine = JobConf.getInetSocketAddress(args[1]);
    Registry registry2 = LocateRegistry.createRegistry(thisMachine.getPort());
    String dataNodeName = thisMachine.toString() + " data node";
    new DataNode(dataNodeName, registry2, thisMachine);
    NameNodeInterface nameNode = (NameNodeInterface) conf.getRegistry().lookup("NameNode");
    nameNode.register(dataNodeName, thisMachine, false);
    nameNode.put(conf.getInputFile(), dataNodeName);

    try {
      client.submitJob(conf);
    } catch (Exception e) {
      System.err.println("Could not send job conf over to master.");
      e.printStackTrace();
    }
    System.out.println("Sent job conf to master. Now listening for updates.");

    int port = conf.getRmiServer().getPort();
    try {
      
           
      Registry registry = LocateRegistry.getRegistry(conf.getRmiServer().getHostString(),conf.getRmiServer().getPort());
      JobTrackerInterface stub = (JobTrackerInterface) registry.lookup("JobTracker");
      System.out.println("about to update");
      client.getUpdates(stub);

    } catch (Exception e) {
      System.err.println("Client exception: " + e.toString());
      e.printStackTrace();
    }
  }

}
