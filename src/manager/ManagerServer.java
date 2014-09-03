package manager;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import utility.*;

import manager.ProcessManager;
import utility.*;
public class ManagerServer implements Runnable{
    private ProcessManager manager;
    private int workerId;
    private Socket socket;
    private boolean running;

    private ObjectInputStream objInput;
    private ObjectOutputStream objOut;

    public ManagerServer(ProcessManager procManager, int id, Socket s) throws IOException {

        manager = procManager;
        workerId = id;
        running = true;

        System.out.println("adding a new process server for worker "+id);
        socket = manager.workersMap.get(id);
        objInput = new ObjectInputStream(socket.getInputStream());
        objOut = new ObjectOutputStream(socket.getOutputStream());
       
    }
    
    public void run(){
        try{
            Message workerMessage;
            while(running){
                try{
                    workerMessage = (Message) objInput.readObject();
                }catch(ClassNotFoundException e){
                    continue;
                }
                System.out.println("worker message received: id "+workerMessage.getCommandId());
                
                switch(workerMessage.getResponseId()){
                    case STARTRES:
                        handleStartRes(workerMessage);
                    
                        break;
                    case MIGARATESOURCERES:
                        //handleMigrateSourceRes(workerMessage);
                        break;
                    case MIGRATETARGETRES:
                        //hanleMigrateTargetRes(workerMessage);
                        break;
                    case KILLRES:
                        //handleKillRes(workerMessage);
                        break;
                    default:
                        System.out.println("unrecagnized message");
                }
            }
        }catch(IOException e){
            
        }
    }
    
    private void handleStartRes(Message workerMsg){
        if(workerMsg.getResult() == -1){
            System.out.println("process "+workerMsg.getProcessName()+"failed to start");
        }
        else{
            ProcessInfo procInfo = new ProcessInfo();
            procInfo.setId(workerMsg.getProcessId());
            procInfo.setName(workerMsg.getProcessName());
            procInfo.setStatus(workerMsg.getStatus());
            manager.processesMap.put(workerMsg.getProcessId(), procInfo);
        }
    }
    public void sendToWorker(Message cmd){
        try {
            if (objOut == null) {
                objOut = new ObjectOutputStream(socket.getOutputStream());
            }
            
            objOut.writeObject(cmd);
            objOut.flush();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Command sent failed");
        }
    }
    public void stop(){
        running = false;
    }
}