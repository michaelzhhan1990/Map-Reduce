package ha.mapreduce;

public class Job {
  private Integer JobID;

  private String JobName;

  private String JobIP; // where the master is, for running and checking

  private String JobPort;

  public Integer getJobID() {
    return JobID;
  }

  public void setJobID(Integer jobID) {
    JobID = jobID;
  }

  public String getJobName() {
    return JobName;
  }

  public void setJobName(String jobName) {
    JobName = jobName;
  }

  public String getJobIP() {
    return JobIP;
  }

  public void setJobIP(String jobIP) {
    JobIP = jobIP;
  }

  public String getJobPort() {
    return JobPort;
  }

  public void setJobPort(String jobPort) {
    JobPort = jobPort;
  }

}
