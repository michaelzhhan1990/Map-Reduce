package ha.testing.IO;

import ha.DFS.NameNodeInterface;
import ha.mapreduce.JobConf;

import java.io.IOException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Write {
  public static void main(String []args) throws NotBoundException, UnknownHostException, IOException{
    if (args.length != 1) {
      System.out.println("USAGE: java ha.testing.IO.Read <conf file> ");
      System.exit(0);
    }

    JobConf conf = new JobConf(args[0]);
    System.out.println("[CLIENT] Setting up new job as such:");
    System.out.println(conf);
 
    
    Registry registry = LocateRegistry.getRegistry(conf.getRmiServer().getHostString(), conf
            .getRmiServer().getPort());
    
    // every datanode(slave) has a namenode stub 
    NameNodeInterface stub=(NameNodeInterface)registry.lookup("NameNode");


    stub.open("hello.txt");
    stub.write("hello.txt", conf.toString());

  }
  


}
