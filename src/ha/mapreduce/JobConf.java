package ha.mapreduce;

import java.io.BufferedReader;
/**
 * the class that handle parsing the system and client configuration files
 */
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class JobConf extends Job implements Serializable {
  /**
   * Master node that decides how to distribute tasks
   */
  private InetSocketAddress master;

  private InetSocketAddress namenode;

  private int keySize, valueSize;

  /**
   * Where the RMI server is located
   */
  private InetSocketAddress rmiServer;

  /**
   * Slave nodes that complete tasks sent by master, and that update master on task status.
   */
  private List<InetSocketAddress> slaves;

  private List<InetSocketAddress> datanodes;

  /**
   * Number of mappers running on each slave at one time.
   */
  private Integer mappersPerSlave;
  
  private Integer replicaPerFile;

  public Integer getReplicaPerFile() {
    return replicaPerFile;
  }

  public void setReplicaPerFile(Integer replicaPerFile) {
    this.replicaPerFile = replicaPerFile;
  }

  private String inputFile;

  /**
   * Number of reducers running on each slave at one time
   */
  private Integer reducersPerSlave;

  /**
   * Which class the mapper is part of
   */
  @SuppressWarnings("rawtypes")
  private Class mapperClass;

  /**
   * Which class the reducer is part of
   */
  @SuppressWarnings("rawtypes")
  private Class reducerClass;

  /**
   * Initialize with a local configuration file on disk
   */
  public JobConf(String conf) throws IOException {
    master = null;
    namenode = null;
    rmiServer = null;
    slaves = new ArrayList<InetSocketAddress>();
    datanodes = new ArrayList<InetSocketAddress>();
    mappersPerSlave = 1;
    reducersPerSlave = 1;
    mapperClass = null;
    reducerClass = null;
    keySize = 0;
    valueSize = 0;
    replicaPerFile=0;

    parseConf(conf);
  }

  public InetSocketAddress getNamenode() {
    return namenode;
  }

  public void setNamenode(InetSocketAddress namenode) {
    this.namenode = namenode;
  }

  public List<InetSocketAddress> getDatanodes() {
    return datanodes;
  }

  public void setDatanodes(List<InetSocketAddress> datanodes) {
    this.datanodes = datanodes;
  }

  public String getInputFile() {
    return inputFile;
  }

  public void setInputFile(String inputFile) {
    this.inputFile = inputFile;
  }

  public Integer getMappersPerSlave() {
    return mappersPerSlave;
  }

  public int getKeySize() {
    return keySize;
  }

  public int getValueSize() {
    return valueSize;
  }

  public int getRecordSize() {
    return keySize + valueSize;
  }

  public void setMappersPerSlave(Integer mappersPerSlave) {
    this.mappersPerSlave = mappersPerSlave;
  }

  public InetSocketAddress getMaster() {
    return master;
  }

  public InetSocketAddress getRmiServer() {
    return rmiServer;
  }

  public List<InetSocketAddress> getSlaves() {
    return slaves;
  }

  public Integer getReducersPerSlave() {
    return reducersPerSlave;
  }

  public Registry getRegistry() {
    try {
      return (Registry) LocateRegistry.getRegistry(getRmiServer().getHostString(), getRmiServer()
              .getPort());
    } catch (RemoteException e) {
      System.err.println("Can't get RMI registry!");
      e.printStackTrace();
      System.exit(0);
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public Class<Mapper> getMapperClass() {
    return mapperClass;
  }

  @SuppressWarnings("unchecked")
  public Class<Reducer> getReducerClass() {
    return reducerClass;
  }

  /**
   * Get the key of a string key-value pair
   */
  private static String getKey(String pair, String delimiter) {
    return pair.split(delimiter)[0];
  }

  /**
   * Get the key of a string key-value pair in the configuration file
   */
  private static String getKey(String pair) {
    return getKey(pair, "=");
  }

  /**
   * Get the value of a string key-value pair
   */
  private static String getValue(String pair, String delimiter) {
    return pair.split(delimiter)[1];
  }

  /**
   * Get the value of a string key-value pair in the configuration file
   */
  private String getValue(String pair) {
    return getValue(pair, "=");
  }

  /**
   * Get an InetSocketAddress from a string of the format ADDRESS:PORT
   */

  public static InetSocketAddress getInetSocketAddress(String value) {
    try {
      return new InetSocketAddress(getKey(value, ":"), Integer.parseInt(getValue(value, ":")));
    } catch (NumberFormatException e) {
      System.err.println("Cannot parse integer \"" + getValue(value, ":") + "\"");
      return null;
    }
  }

  /**
   * Find the class with the given name
   */
  @SuppressWarnings("rawtypes")
  private Class getClass(String value) {
    try {
      return Class.forName(value);
    } catch (ClassNotFoundException e) {
      System.err.println("Class \"" + value + "\" not found, setting to null for now.");
      return null;
    }
  }

  /**
   * Parse the configuration file from disk.
   */
  public void parseConf(String conf) throws IOException {

    BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(conf)));
    String line;
    while ((line = br.readLine()) != null) {
      String value = getValue(line);
      switch (getKey(line)) {
        case "MASTER":
          master = getInetSocketAddress(value);
          break;
        case "RMI":
          rmiServer = getInetSocketAddress(value);
          break;
        case "PARTICIPANT":
          slaves.add(getInetSocketAddress(value));
          break;
        case "MAPS_PER_HOST":
          mappersPerSlave = Integer.parseInt(value);
          break;
        case "REDUCES_PER_HOST":
          reducersPerSlave = Integer.parseInt(value);
          break;
        case "MAPPER":
          mapperClass = getClass(value);
          break;
        case "REDUCER":
          reducerClass = getClass(value);
          break;
        case "INPUT_FILE":
          inputFile = value;
          break;
        case "DATANODE":
          datanodes.add(getInetSocketAddress(value));
          break;
        case "KEY_SIZE":
          keySize = Integer.parseInt(value);
          break;
        case "VALUE_SIZE":
          valueSize = Integer.parseInt(value);
          break;
        case "REPLICA_NUM":
          replicaPerFile=Integer.parseInt(value);
          break;
        default:
          System.err.println("Parameter \"" + getKey(line) + "\" unrecognized.");
          break;
      }
    }
    br.close();
  }

  /**
   * Show configuration information
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Master node located at " + master + "\n");
    sb.append("RMI registry at " + rmiServer + "\n");
    sb.append(slaves.size() + " slave nodes located at:\n");
    for (InetSocketAddress address : slaves) {
      sb.append("\t" + address + "\n");
    }
    sb.append(mappersPerSlave + " mappers per slave running " + mapperClass + "\n");
    sb.append(reducersPerSlave + " reducers per slave running " + reducerClass + "\n");
    sb.append("\n");
    return sb.toString();
  }
}
