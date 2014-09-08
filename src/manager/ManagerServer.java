package manager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import utility.*;
import utility.Message.msgType;
import utility.ProcessInfo.Status;
import manager.ProcessManager;
import utility.*;
public class ManagerServer implements Runnable{
    private ProcessManager manager;
    private int workerId;
    private Socket socket;
    private volatile boolean running;

    private ObjectInputStream objInput;
    private ObjectOutputStream objOutput;

    public ManagerServer(ProcessManager procManager, int id, Socket s) throws IOException {

        manager = procManager;
        workerId = id;
        running = true;

        System.out.println("adding a new process server for worker "+id);
        socket = manager.workersMap.get(id);
        manager.workerStatusMap.put(id,0);
        objInput = new ObjectInputStream(socket.getInputStream());
        objOutput = new ObjectOutputStream(socket.getOutputStream());
       
    }
    
    public void run(){
        try{
            Message assignCmd = new Message(msgType.COMMAND);
            assignCmd.setCommandId(CommandType.ASSIGNID);
            assignCmd.setWorkerID(workerId);
            sendToWorker(assignCmd);
            
            Message workerMessage;
            System.out.println("managerServer for worker "+workerId+"running");
            while(running){
                
                try{
                    workerMessage = (Message) objInput.readObject();
                }catch(ClassNotFoundException e){
                    continue;
                }
                if(workerMessage.getResponseId() != ResponseType.PULLINFORES)
                    System.out.println("worker message received: id "+workerMessage.getResponseId());
                
                switch(workerMessage.getResponseId()){
                    case STARTRES:
                        handleStartRes(workerMessage);
                    
                        break;
                    case MIGARATESOURCERES:
                        handleMigrateSourceRes(workerMessage);
                        break;
                    case MIGRATETARGETRES:
                        hanleMigrateTargetRes(workerMessage);
                        break;
                    case KILLRES:
                        handleKillRes(workerMessage);
                        break;
                    case PULLINFORES:
                        handlePullInfo(workerMessage);
                        break;
                    default:
                        System.out.println("unrecagnized message");
                }
            }
            objInput.close();
            objOutput.close();
        }catch(IOException e){
            
        }
    }
    
    private void handleStartRes(Message workerMsg){
        if(workerMsg.getResult() == Message.msgResult.FAILURE){
            System.out.println("process "+workerMsg.getProcessName()+"failed to start: "+workerMsg.getCause());
        }
        else{
            manager.processesMap.get(workerMsg.getProcessId()).setStatus(Status.RUNNING);
        }
    }
    
    private void handleKillRes(Message workerMsg){
        if(workerMsg.getResult() == Message.msgResult.FAILURE){
            System.out.println("process "+workerMsg.getProcessId()+"failed to kill: "+workerMsg.getCause());
        }
        else{
            
            manager.processesMap.get(workerMsg.getProcessId()).setStatus(ProcessInfo.Status.FINISHED);
        }
    }
    
    private void handleMigrateSourceRes(Message workerMsg){
        if(workerMsg.getResult() == Message.msgResult.FAILURE){
            System.out.println("process "+workerMsg.getProcessId()+"failed to migrate from "+workerMsg.getProcessId()+":"+workerMsg.getCause());
        }
        else{
            
            workerMsg.setMessageType(Message.msgType.COMMAND);
            workerMsg.setCommandId(CommandType.MIGRATETARGET);
            
            try{
                manager.processServerMap.get(workerMsg.getTargetId()).sendToWorker(workerMsg);
            }catch(IOException e){
            
                System.out.println("failed to send message to target worker "+workerMsg.getTargetId()+"remove the worker");
                manager.removeNode(workerMsg.getTargetId());
            }
            
        }
    }
    
    private void hanleMigrateTargetRes(Message workerMsg){
        if(workerMsg.getResult() == Message.msgResult.FAILURE){
            System.out.println("process "+workerMsg.getProcessId()+"failed to migrate to "+workerMsg.getProcessId()+":"+workerMsg.getCause());
        }
        else{
            int workerId = workerMsg.getTargetId();
            int procId = workerMsg.getProcessId();
            System.out.println("proc id:"+procId);
            ProcessInfo procInfo = manager.processesMap.get(procId);
            if(procInfo == null)
                System.out.println("null procInfo");
            
            procInfo.setWorkerId(workerId);
            procInfo.setWorkerId(workerMsg.getTargetId());
            procInfo.setStatus(Status.RUNNING);
            
        }
    }
    
    private void handlePullInfo(Message workerMsg){
        Integer processId;
        HashMap<Integer,ProcessInfo.Status> workerStatus = workerMsg.getWorkerInfo();
        if(!workerStatus.isEmpty()){
            Set<Integer> id = workerStatus.keySet();
            Iterator<Integer> idIterator = id.iterator();
            while(idIterator.hasNext()){
                processId = idIterator.next();
                //System.out.println("process id "+processId);
                ProcessInfo procInfo = manager.processesMap.get(processId);
                if(procInfo == null)
                    System.out.println("procInfo null");
                Status s = workerStatus.get(processId);
                if(s == null)
                    System.out.println("workerStatus null");
                
                procInfo.setStatus(s);
                
                
                //manager.processesMap.get(processId).setStatus(workerStatus.get(processId));
            }
            
        }
        /*reset the guard state for this worker, it's alive*/
        manager.workerStatusMap.put(workerId, 0);
    }
    public int sendToWorker(Message cmd) throws IOException{
            if(cmd.getCommandId() == CommandType.START){
                System.out.println("arg length: "+cmd.getArgs().length);
                for(int i=0;i<cmd.getArgs().length;i++){
                    System.out.println("args:"+cmd.getArgs()[i]);
                }
            }
            objOutput.writeObject(cmd);
            objOutput.flush();
            return 0;
         
    }
    
    public void stop(){
        running = false;
    }
}