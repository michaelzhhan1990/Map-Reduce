package ha.mapreduce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Reducer extends Task {
  public abstract void reduce(String key, Collection<String> values, OutputCollector collector);

  @Override
  public void process() throws IOException {
    byte[] key = new byte[taskConf.getKeySize()], value = new byte[taskConf.getValueSize()];
    String previousKey = "";
    List<String> values = new ArrayList<String>();
    isr.skip(taskConf.getStart());
    for (int i = 0; i < taskConf.getEndingRecord(); i++) {
      isr.read(key, value);
      String currentKey = new String(key), currentValue = new String(value);

      if (currentKey.equals(previousKey)) { // add to list of values to reduce
        System.out.println("[REDUCER " + taskConf.getTaskID()
                + "] Adding value \"" + currentValue + "\"");

        values.add(currentValue);
      } else { // reduce what we have so far
        System.out.println("[REDUCER " + taskConf.getTaskID() + "] Encountered key \"" + currentKey
                + "\" different from \"" + previousKey + "\"");

        if (!previousKey.isEmpty()) {
          System.out.println("[REDUCER " + taskConf.getTaskID() + "] Calling reduce function on ("
                  + previousKey + ", " + values + ")");

          reduce(previousKey, values, collector);
        }

        previousKey = currentKey;
        values = new ArrayList<String>();
        System.out.println("[REDUCER " + taskConf.getTaskID()
                + "] Adding value \"" + currentValue + "\"");

        values.add(currentValue);
      }
    }

    System.out.println("[REDUCER " + taskConf.getTaskID() + "] Done with all keys");

    // done with everything but last key
    if (!previousKey.isEmpty()) {
      System.out.println("[REDUCER " + taskConf.getTaskID() + "] Calling reduce function on ("
              + new String(key) + ", " + values + ")");

      reduce(previousKey, values, collector);
    }
    
    collector.write2Disk();
  }
}
