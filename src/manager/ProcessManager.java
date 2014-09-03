package manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.net.Socket;





import utility.*;

/**
* ProcessManager serve as the master of this whole system. It will open a command
* line console for user input and start the connectionServer for listening to the 
* socket connect from worker node.abstract
*/

public class ProcessManager {
    public ConcurrentHashMap<Integer,ProcessInfo> processesMap;
    public ConcurrentHashMap<Integer,Socket> workersMap;/*worker node socket for every id*/
    public ConcurrentHashMap<Integer,ManagerServer> processServerMap;/*processServer for every worker node Id*/
    private ConnectionServer connServer;

    private int port;
    private BufferedReader console;
    private int processId;
    private boolean running;
    
    

    public ProcessManager(int listenPort){
        port = listenPort;
        console = new BufferedReader(new InputStreamReader(System.in));
        connServer = new ConnectionServer(port,this);
        processesMap = new ConcurrentHashMap<Integer,ProcessInfo>();
        workersMap = new ConcurrentHashMap<Integer,Socket>();
        processServerMap = new ConcurrentHashMap<Integer,ManagerServer>();
        processId = 0;
        running = true;
        
    }
    
    public void startConsole(){
        System.out.println("This is process manager, type help for more information");
        
        String cmdLine=null;
        while(true){
            System.out.print(">>");
            try{
                cmdLine = console.readLine();
                
            }catch(IOException e){
                System.out.println("IO error while reading the command,console will be closed");
                //closeConsle();
            }            
            
            String[] inputLine = cmdLine.split(" ");
            switch(inputLine[0]){
                case "start":
                    handleStartProcess(inputLine);
                    break;
                case "migrate":
                    handleMigrateProcess(inputLine);
                    break;
                case "kill":
                    handleKillProcess(inputLine);
                    break;
                case "ls":
                    handleLs(inputLine);
                    break;
                case "ps":
                    handlePs(inputLine);
                    break;
                case "help":
                    handleHelp(inputLine);
                    break;
                case "shutdown":
                    terminate();
                    System.exit(0);
                    break;
                default:
                    System.out.println(inputLine[0]+"is not a valid command");
            }
        }
    }
    
    private void handleLs(String[] cmdLine){
        if(0 == workersMap.size())
            System.out.println("no worker in system");
        else{
            for(int i : workersMap.keySet())
                System.out.println("worker ID: "+i+"  IP Address: "+workersMap.get(i).getInetAddress());
        }
    }
    
    private void handlePs(String[] cmdLine){
        if(0 == processesMap.size())
            System.out.println("no process information");
        else{
            for(int i : processesMap.keySet()){
                ProcessInfo info = processesMap.get(i);
                System.out.println("Process ID: "+i+" Process Name: "+info.getName()+" Process Status: "+info.getStatus());
            }
                
        }
    }
    
    private void handleStartProcess(String[] cmdLine){
        if(cmdLine.length < 3){
            System.out.println("invalid argument, see help for more information");
            return;
        }
        int workerId;
        try{
            workerId = Integer.getInteger(cmdLine[1]);
            
        }catch(Exception e){
            System.out.println("the worker id is not a number");
            return;
            
        }
        if(!workersMap.containsKey(workerId)){
            System.out.println("there is no worker with id number "+workerId);
            return;
        }
        /*pass all the checkes, now get the argument for the process*/
        String[] args = new String[cmdLine.length - 3];
        for(int i=3;i<cmdLine.length;i++){
            args[i-3] = cmdLine[i];
        }
        try{
            Class process = ProcessManager.class.getClassLoader().loadClass(cmdLine[2]);
        }catch(ClassNotFoundException e){
            System.out.println("no such process class");
            return;
        }
        
        processId++;
        Message cmdMessage = new Message(Message.msgType.COMMAND);
        cmdMessage.setCommandId(CommandType.START);
        cmdMessage.setArgs(args);
        cmdMessage.setProcessName(cmdLine[2]);
        cmdMessage.setProcessId(processId);
        processServerMap.get(workerId).sendToWorker(cmdMessage);
            
    }
    
    private void handleKillProcess(String[] cmdLine){
        int procId=-1;
        if(cmdLine.length < 2){
            System.out.println("invalid argument number, please type help for more information");
            return;
        }
        try{
            procId=Integer.valueOf(cmdLine[1]);
        }catch(Exception e){
            System.out.println("the worker id is not a number");
            return;
        }
        
        if(!processesMap.containsKey(procId)){
            System.out.println("no such process");
            return;
        }
        ProcessInfo procInfo = processesMap.get(procId);
        if (procInfo.getStatus().equals(ProcessInfo.Status.RUNNING)) {
                Message killCommand =new Message(Message.msgType.COMMAND);
                killCommand.setCommandId(CommandType.KILL);
                killCommand.setProcessId(procId);
                int workerId = procInfo.getWorkerId();
                
                processServerMap.get(workerId).sendToWorker(killCommand);
                
                
                /*update the process status when receive the reply from worker*/

            } else {
                System.out.println("That process is not currently running");
            }
    } 
    
    private void handleMigrateProcess(String[] cmdLine){
        if(cmdLine.length != 4){
            System.out.println("wrong arguments number");
            return;
        }
        int procId,sourceId,targetId;
        try{
            procId=Integer.valueOf(cmdLine[1]);
        }catch(Exception e){
            System.out.println("the process id is not a number");
            return;
        }
        try{
            sourceId=Integer.valueOf(cmdLine[2]);
        }catch(Exception e){
            System.out.println("the source worker id is not a number");
            return;
        }
        try{
            targetId=Integer.valueOf(cmdLine[3]);
        }catch(Exception e){
            System.out.println("the target worker id is not a number");
            return;
        }
        
        if(!processesMap.containsKey(procId)){
            System.out.println("no process with "+procId+"exist");
            return;
        }
        
        if(!processServerMap.containsKey(sourceId)){
            System.out.println("the source worker "+sourceId+" does not exist");
            return;
        }
     
        if(!processServerMap.containsKey(targetId)){
            System.out.println("the target worker "+targetId+" does not exist");
            return;
        }
        
        Message migrateCommand = new Message(Message.msgType.COMMAND);
        migrateCommand.setCommandId(CommandType.MIGARATESOURCE);
        migrateCommand.setProcessId(procId);
        migrateCommand.setSourceId(sourceId);
        migrateCommand.setTargetId(targetId);
        
        
        processServerMap.get(sourceId).sendToWorker(migrateCommand);
        



    }
    
    private void handleHelp(String[] cmdLine){
        System.out.println("Commands List:");
        System.out.println("ls : list all the worker node in the system");
        System.out.println("ps : list all the processes");
        System.out.println("start <process name> <args[]> <worker id> : start the process on the designated worker");
        System.out.println("migrate <process id> <source id> <target id> : migrate process from source to target worker");
        System.out.println("kill <process id> : kill the process");
    }
    private void removeNode(int id){
        processServerMap.get(id).stop();
        processServerMap.remove(id);
        workersMap.remove(id);
    }
    
    private void startServer(int port){

        Thread t1 = new Thread(connServer);
        t1.start();
    }
    
    private void terminate(){
        Message tmntCommand = new Message(Message.msgType.COMMAND);
        tmntCommand.setCommandId(CommandType.SHUTDOWN);
        Set<Integer> workerIdSet = processServerMap.keySet();
        Iterator<Integer> workerIterator= workerIdSet.iterator();
        while(workerIterator.hasNext()){
            processServerMap.get(workerIterator.next()).sendToWorker(tmntCommand);
            processServerMap.get(workerIterator.next()).stop();
        }
        
        try{
            console.close();
        }catch(IOException e){
            
        }
        
    }
    public static void main(String[] args){
        if(args.length != 1){
            System.out.println("wrong arguments, usage: processManager <port number>");
            return;
        }
        int port;
        try{
            port = Integer.valueOf(args[0]);
        }catch(Exception e){
           System.out.println("invalid port number");
           return;
        }
        ProcessManager manager = new ProcessManager(port);
        manager.startServer(port);
        manager.startConsole();
        
        
    }
    
}