package ha.mapreduce;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class Task {
  protected InputStreamReader isr;
  protected OutputCollector collector;
  protected int recordStart;
  protected int recordCount;
  protected int keySize;
  protected int valueSize;
  
  public void setup(TaskConf tc) throws FileNotFoundException {
    isr = new InputStreamReader(new FileInputStream(tc.getFilename()));
    this.recordStart = tc.getRecordStart();
    this.recordCount = tc.getRecordCount();
    this.keySize = tc.getKeySize();
    this.valueSize = tc.getValueSize();
    this.collector = new OutputCollector(tc.getFilename() + "_" + recordStart + ".map", keySize, valueSize);
  }
  
  public abstract void process() throws IOException;
}
