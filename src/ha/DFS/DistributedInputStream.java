package ha.DFS;

import java.io.IOException;
import java.io.InputStream;
/**
 * the IO used by dfs
 * @author hanz& amos
 *
 */
public class DistributedInputStream extends InputStream {
  long position;

  String filename;

  NameNodeInterface nameNode;

  public DistributedInputStream(String filename, NameNodeInterface nameNode) {
    this.filename = filename;
    this.nameNode = nameNode;
    this.position = 0;
  }

  @Override
  public int read() throws IOException {
    return nameNode.read(filename, position++, 1)[0];
  }

  @Override
  public int read(byte[] arg0) throws IOException {
    System.arraycopy(nameNode.read(filename, position, arg0.length), 0, arg0, 0, arg0.length);
    position += arg0.length;
    return arg0.length;
  }
  
  public int read(byte[] key, byte[] value) throws IOException {
    return read(key) + read(value);
  }

  @Override
  public long skip(long n) {
    position += n;
    return n;
  }
}