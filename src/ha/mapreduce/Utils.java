package ha.mapreduce;

public class Utils {
  // http://stackoverflow.com/a/391978/257583
  public static String padRight(String s, int n) {
    return String.format("%1$-" + n + "s", s);
  }

  public static String padLeft(String s, int n) {
    return String.format("%1$" + n + "s", s);
  }
}
