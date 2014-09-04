package processes;

import java.io.Serializable;

import utility.ProcessInfo;

public abstract class MigratableProcess implements Runnable,Serializable {
    
   int processId;
   ProcessInfo.Status status;
   boolean complete;

	public void setProcessID(int processId){
	    
	}

	public Integer getProcessID(){
	    return processId;
	}

	public void suspend(){
	    
	}

	public void exit(){
	    
	}
	
	public void setStatus(ProcessInfo.Status status){
	    
	}
	public ProcessInfo.Status getStatus(){
	    return status;
	}
	
	public void setComplete(){
	    
	}
	public Boolean isComplete(){
	    return complete;
	}
}
