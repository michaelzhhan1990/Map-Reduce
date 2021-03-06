package ha.DFS;

import ha.mapreduce.TaskConf;
import ha.mapreduce.TaskTrackerInterface;

import java.net.InetSocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 * dfs center, control all the remote read and write and file registration
 * @author hanz&amos
 *
 */
public class NameNode implements NameNodeInterface, Runnable {

  // table of slave id, slave ip, key is slave id, value is slave id

  private HashMap<String, DataNodeInterface> stubMap;

  /**
   * whether a certain data node is read/write, or write-only
   */
  private HashMap<DataNodeInterface, Boolean> writables;

  /**
   * which data nodes each file is located on
   */
  private HashMap<String, List<DataNodeInterface>> filelocations;

  // key is datanode id, value is slave status, 1 is alive, 0 is down
  // periodically check if the node is down, if they didn't reply, check their status, if they are
  // down for two times, then get rid of them, write all the files to other place
  private HashMap<DataNodeInterface, Boolean> statusList;

  /**
   * list of files contained in each data node
   */
  private HashMap<DataNodeInterface, List<String>> fileinDataNode;

  /**
   * where the data nodes are
   */
  private Map<DataNodeInterface, InetSocketAddress> dnTrackers;
  
  /**
   * how many times a file is replicated across the DFS
   */
  private int replicaPerFile;
  
  

  public NameNode(int replicaPerFile) throws RemoteException, NotBoundException {

    stubMap = new HashMap<String, DataNodeInterface>();
    writables = new HashMap<DataNodeInterface, Boolean>();
    filelocations = new HashMap<String, List<DataNodeInterface>>();
    statusList = new HashMap<DataNodeInterface, Boolean>();
    fileinDataNode = new HashMap<DataNodeInterface, List<String>>();
    dnTrackers = new HashMap<DataNodeInterface, InetSocketAddress>();
    this.replicaPerFile=replicaPerFile;

  }
  /**
   * keep the record of file-data nodes storing information
   * @param filename
   * @param dataNode
   */
  private void addToFileLocations(String filename, DataNodeInterface dataNode) {
    if (!filelocations.containsKey(filename)) {
      filelocations.put(filename, new ArrayList<DataNodeInterface>());
    }
    if (!fileinDataNode.containsKey(dataNode)) {
      fileinDataNode.put(dataNode, new ArrayList<String>());
    }
    if (!filelocations.get(filename).contains(dataNode)) {
      filelocations.get(filename).add(dataNode);
    }
    if (!fileinDataNode.get(dataNode).contains(filename)) {
      fileinDataNode.get(dataNode).add(filename);
    }
  }
  /**
   * get the stub of a data node given a filename, return the first stub from the list, the first stub is the one has the origin file, other nodes store only replicas 
   * @param filename
   * @return
   */
  private DataNodeInterface getStubFor(String filename) {
    List<DataNodeInterface> stubs = filelocations.get(filename);
    if (stubs == null) {
      System.err.println("[NAME NODE] Location for file " + filename + " is null!");
      return null; // pass the NullPointerException on, but at least we know the culprit
    } else {
      return stubs.get(0);
    }
  }
  /**
   * remote reading
   */
  @Override
  public byte[] read(String filename, long start, int length) throws RemoteException {
    return getStubFor(filename).read(filename, start, length);
  }

  /**
   * Make sure there are at least N data nodes containing replicas of the file, N defined by replica factor
   */
  private void allocateDataNodes(String filename) {
    Iterator<DataNodeInterface> dnit = stubMap.values().iterator();
    while (!filelocations.containsKey(filename) || filelocations.get(filename).size() < replicaPerFile) {
      if (!dnit.hasNext()) {
        System.err.println("[NAME NODE] Not enough data nodes to allocate for file " + filename
                + "!");
        break;
      }

      DataNodeInterface dni = dnit.next();
      if (writables.get(dni)) {
        addToFileLocations(filename, dni);
      }
    }
  }
  /**
   * remote write
   */
  @Override
  public void write(String filename, String stuff) throws RemoteException {
    allocateDataNodes(filename);
    for (DataNodeInterface dataNode : filelocations.get(filename)) {
      dataNode.write(filename, stuff);
    }
  }
 
  @Override
  public void write(String filename, byte[] key, byte[] value) throws RemoteException {
    allocateDataNodes(filename);
    for (DataNodeInterface dataNode : filelocations.get(filename)) {
      dataNode.write(filename, key, value);
    }
  }

  @Override
  public void open(String filename) throws RemoteException {
    allocateDataNodes(filename);
    for (DataNodeInterface dataNode : filelocations.get(filename)) {
      dataNode.open(filename);
    }
  }

  // put file given by file name and datanode name
  @Override
  public void put(String filename, String rmiName) throws RemoteException {
    addToFileLocations(filename, stubMap.get(rmiName));
  }

  @Override
  public long getFileSize(String filename) throws RemoteException {
    return getStubFor(filename).getFileSize(filename);
  }
  /**
   * data nodes using this fuction for name node stub to register
   */
  @Override
  public void register(String rmiName, InetSocketAddress rmi_location, boolean writable)
          throws RemoteException {
    try {
      stubMap.put(
              rmiName,
              (DataNodeInterface) LocateRegistry.getRegistry(rmi_location.getHostString(),
                      rmi_location.getPort()).lookup(rmiName));
      writables.put(stubMap.get(rmiName), writable);

      statusList.put(stubMap.get(rmiName), true);

      dnTrackers.put(stubMap.get(rmiName), rmi_location);

    } catch (NotBoundException e) {
      e.printStackTrace();
    }

  }
  /**
   * periodically check the data nodes if they are alive
   */
  public void heartBeat() throws RemoteException {
    for (Map.Entry<DataNodeInterface, Boolean> pairs : statusList.entrySet()) {
      DataNodeInterface t = pairs.getKey();
      Boolean tag = pairs.getValue();
      System.out.println("[Name Node] sending heart beat for datanodes");

      try {
        System.out.println(t.sayhello());
      } catch (RemoteException e) {
        // do somehting
        // set the task to do again
        // check if the status is faired last time. two fail means real fails
        System.err.println("[NameNode ] something wrong with this datanode ");
        if (tag == true) // last time is good, give him one more chance
        {
          statusList.put(t, false);
        } else { // add the file to other place and then delete the node,delete it!!
          ArrayList<String> filetoAdd = (ArrayList) fileinDataNode.get(t);
          for (String ite : filetoAdd) {
            allocateDataNodes(ite);


            // delete the node from list_filelocations
            List<DataNodeInterface> l_node = filelocations.get(ite);
            l_node.remove(t);
            filelocations.put(ite, l_node);
          }

        }

        // delete the node from stubMap
        for (Map.Entry<String, DataNodeInterface> stub : stubMap.entrySet()) {
          if (stub.getValue() == t) {
            stubMap.remove(stub.getKey());
            break;
          }

        }

        writables.remove(t);

        statusList.remove(t);
        fileinDataNode.remove(t);

      }

    }
  }

  private void reWrite2Random(String filename) {

  }

  @Override
  public String sayhello() throws RemoteException {
    // TODO Auto-generated method stub
    return null;
  }
  /**
   * using another thread to periodically ask the data noes whether they are alive or not
   */
  public void run() {
    while (true) {
      try {
        Thread.sleep(5000);
        if (statusList.size() != 0) {
          ArrayList<DataNodeInterface> tobeDelete = new ArrayList<DataNodeInterface>();
          for (Map.Entry<DataNodeInterface, Boolean> pairs : statusList.entrySet()) {
            DataNodeInterface t = pairs.getKey();
            Boolean tag = pairs.getValue();

            try {
              System.out.println("[Name Node] sending heart beat for datanode "
                      + dnTrackers.get(t).toString());
              System.out.println("[Name Node] " + t.sayhello());
            } catch (RemoteException e) {
              // do somehting
              // set the task to do again
              // check if the status is faired last time. two fail means real fails
              System.err.println("[NameNode ] something wrong with datanode "
                      + dnTrackers.get(t).toString());
              if (tag == true) // last time is good, give him one more chance
              {
                System.out.println("[Name Node] give it one more chance");
                statusList.put(t, false);
              } else { // add the file to other place and then delete the node,delete it!!
                System.err.println("[Name Node ] datanode " + dnTrackers.get(t).toString()
                        + " is down.");

                if (fileinDataNode.size() != 0) {
                  ArrayList<String> filetoAdd = (ArrayList) fileinDataNode.get(t);

                  if (filetoAdd.size() != 0) {
                    System.out.println("[NAME NODE] reallocate all the file replica in datanode "+dnTrackers.get(t).toString()+" to other datanodes");

                    for (String ite : filetoAdd) {
                      
                      // allocate another datanode for the file
                      
                      allocateDataNodes(ite);

                      // delete the node from list_filelocations
                      List<DataNodeInterface> l_node = filelocations.get(ite);
                      l_node.remove(t);
                      filelocations.put(ite, l_node);
                    }
                  }
                  fileinDataNode.remove(t);
                }

                // delete the node from stubMap
                for (Map.Entry<String, DataNodeInterface> stub : stubMap.entrySet()) {
                  if (stub.getValue() == t) {
                    stubMap.remove(stub.getKey());
                    break;
                  }

                }

                writables.remove(t);

                tobeDelete.add(t);

              }

            }

          }
          if (tobeDelete.size() != 0) {
            for (DataNodeInterface t : tobeDelete) {
              statusList.remove(t);

            }

          }

        }

      } catch (InterruptedException e) {
        System.err.println("[Name Node] Cannot sleep thread!");
        e.printStackTrace();
      }
    }
  }

  

}
