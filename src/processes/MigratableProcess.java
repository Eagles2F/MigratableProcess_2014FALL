package processes;

import java.io.Serializable;

import utility.ProcessInfo;

public abstract class MigratableProcess implements Runnable,Serializable {
    
   int processId;
   ProcessInfo.Status procStatus;
   boolean complete;

	public void setProcessID(int processId){
	    this.processId = processId;
	}

	public Integer getProcessID(){
	    return processId;
	}

	public void suspend(){
	    
	}
	
	public void setStatus(ProcessInfo.Status status){
		this.procStatus = status;
	    
	}
	public ProcessInfo.Status getStatus(){
	    return procStatus;
	}
	
	public void setComplete(boolean flag){
	    complete = flag;
	}
	public Boolean isComplete(){
	    return complete;
	}
}
