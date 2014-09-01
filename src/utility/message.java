package utility;
import java.io.Serializable;
import processes.MigratableProcess;
/**
* command class used to send between manager and worker
*/
public class message implements Serializable {
    private int messageType;
    private int messageId;
    private int processId;
    private String processName;
    private String[] args;
    private MigratableProcess processObject;
    private int sourceNode;
    private int targetNode;
    private int result;
    private String cause;
    
    public message (int type,int id){
        messageType = type;
        messageId = id;
    }
    
    
    
    /*get methods*/
    public int getMessageType(){
        return messageType;
    }
    
    public int getMessageId(){
        return messageId;
    }
    
    public int getProcessId(){
        return processId;
    }
    
    public String getProcessName(){
        return processName;
    }
    
    public String[] getArgs(){
        return args;
    }
    
    public MigratableProcess getProcessObject(){
        return processObject;
    }
    
    public int getSourceId(){
        return sourceNode;
    }
    
    public int getTargetId(){
        return targetNode;
    }
    
    public int getResult(){
        return result;
    }
    
    public String getCause(){
        return cause;
    }
    
    /*set method*/
    
    public void setMessageType(int type){
        messageType = type ;
    }
    
    public void setMessageId(int msgId){
        messageId = msgId;
    }
    
    public void setProcessId(int procId){
        processId = procId;
    }
    
    public void setProcessName(String name){
        processName = name;
    }
    
    public void setArgs(String[] arguments){
        args = arguments;
    }
    
    public void setProcessObject(MigratableProcess mp){
        processObject = mp;
    }
    
    public void setSourceId(int sourceId){
        sourceNode = sourceId;
    }
    
    public void setTargetId(int targetId){
        targetNode = targetId;
    }
    
    public void setResult(int r){
        result = r;
    }
    
    public void setCause(String c){
        cause = c;
    }
}