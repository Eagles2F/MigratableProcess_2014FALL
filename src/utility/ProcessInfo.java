package utility;
public class ProcessInfo{
    String name;
    String status;
    int procId;
    int workerId;
    public enum Status{
        STARTING,
        RUNNING,
        SUSPEND,
        TERMINATING,
        TRASFERING,
        FAILED,
        FINISHED,
        KILLED
    }
    public ProcessInfo(){
        status = Status.STARTING.toString();
        
        
    }
    public String getName(){
        return name;
    }
    public String getStatus(){
        return status;
    }
    public int getProcId(){
        return procId;
    }
    public int getWorkerId(){
        return workerId;
    }
    
    public void setName(String procName){
        name = procName;
    }
    public void setId(int id){
        procId = id;
    }
    public void setWorkerId(int id){
        workerId = id;
    }
    
    public void setStatus(Status s){
        status = s.toString();
            
        
    }
}