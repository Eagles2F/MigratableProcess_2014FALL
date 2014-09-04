package processes;

import java.io.Serializable;

import utility.ProcessInfo;

public interface MigratableProcess extends Runnable,Serializable {
    
   

	public void setProcessID(int processId);

	public Integer getProcessID();

	public void suspend();

	public void exit();
	
	public void setStatus(ProcessInfo.Status status);
	public ProcessInfo.Status getStatus();
	
	public void setComplete();
	public Boolean isComplete();
}