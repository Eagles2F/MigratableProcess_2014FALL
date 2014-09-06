package manager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import utility.*;
import utility.Message.msgType;
import utility.ProcessInfo.Status;
import manager.ProcessManager;
import utility.*;
public class ManagerServer implements Runnable{
    private ProcessManager manager;
    private int workerId;
    private Socket socket;
    private boolean running;

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
            
            while(running){
                System.out.println("managerServer for worker "+workerId+"running");
                try{
                    workerMessage = (Message) objInput.readObject();
                }catch(ClassNotFoundException e){
                    continue;
                }
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
            System.out.println("process "+workerMsg.getProcessId()+"failed to migrate from: "+workerMsg.getProcessId());
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
            System.out.println("process "+workerMsg.getProcessId()+"failed to migrate to : "+workerMsg.getProcessId());
        }
        else{
            
            manager.processesMap.get(workerMsg.getProcessId()).setWorkerId(workerMsg.getTargetId());
            
        }
    }
    public int sendToWorker(Message cmd) throws IOException{
        
            
            objOutput.writeObject(cmd);
            objOutput.flush();
            return 0;
         
    }
    
    public void stop(){
        running = false;
    }
}