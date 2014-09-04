package utility;
import java.io.Serializable;
import java.util.HashMap;

import processes.MigratableProcess;
/**
* command class used to send between manager and worker
*/
public class Message implements Serializable {
    public enum msgType {
        COMMAND,
        RESPONSE,
        INDICATION
    }
    public enum msgResult{
        SUCCESS,
        FAILURE
    }
    private msgType messageType;
    private CommandType cmdId;
    private ResponseType resId;
    private int processId;
    private String processName;
    private String[] args;
    private MigratableProcess processObject;
    private int sourceNode;
    private int targetNode;
    private msgResult result;
    private String cause;
    private ProcessInfo.Status procStatus;
    private HashMap<Integer,ProcessInfo.Status> workerInfo;
    private int workerID;
    
    
    public Message (msgType type){
        messageType = type;
        
    }
    
    
    
    /*get methods*/
    public msgType getMessageType(){
        return messageType;
    }
    
    public CommandType getCommandId(){
        return cmdId;
    }
    
    public ResponseType getResponseId(){
        return resId;
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
    
    public msgResult getResult(){
        return result;
    }
    
    public String getCause(){
        return cause;
    }
    
    public ProcessInfo.Status getStatus(){
        return procStatus;
    }
    /*set method*/
    
    public void setMessageType(msgType type){
        messageType = type ;
    }
    
    public void setCommandId(CommandType msgId){
        cmdId = msgId;
    }
    
    public void setResponseId(ResponseType msgId){
        resId = msgId;
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
    
    public void setResult(msgResult success){
        result = success;
    }
    
    public void setCause(String c){
        cause = c;
    }
    
    public void setStatus(ProcessInfo.Status pStatus){
        procStatus = pStatus;
    }



	public int getWorkerID() {
		return workerID;
	}



	public void setWorkerID(int workerID) {
		this.workerID = workerID;
	}
    
}