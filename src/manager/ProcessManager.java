package manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.net.Socket;









import java.net.SocketException;

import utility.*;
import utility.ProcessInfo.Status;

/**
* ProcessManager serve as the master of this whole system. It will open a command
* line console for user input and start the connectionServer for listening to the 
* socket connect from worker node.abstract
*/

public class ProcessManager {
    public ConcurrentHashMap<Integer,ProcessInfo> processesMap;
    public ConcurrentHashMap<Integer,Socket> workersMap;/*worker node socket for every id*/
    public ConcurrentHashMap<Integer,ManagerServer> processServerMap;/*processServer for every worker node Id*/
    public ConcurrentHashMap<Integer,Integer> workerStatusMap;/*worker will report the status every 5 seconds, a timer task will
                                                             check this*/
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
        workerStatusMap = new ConcurrentHashMap<Integer,Integer>();
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
            for(int i : workersMap.keySet()){
                //System.out.println("id "+i+"status "+workerStatusMap.get(i));
                if(workerStatusMap.get(i) == -1)
                    System.out.println("worker ID: "+i+"  IP Address: "+workersMap.get(i).getInetAddress()+" FAILED");
                else
                    System.out.println("worker ID: "+i+"  IP Address: "+workersMap.get(i).getInetAddress()+" ALIVE");
            }
        }
    }
    
    private void handlePs(String[] cmdLine){
        if(0 == processesMap.size())
            System.out.println("no process information");
        else{
            for(int i : processesMap.keySet()){
                ProcessInfo info = processesMap.get(i);
                System.out.println("Process ID: "+i+" Process Name: "+info.getName()+" Process Status: "+info.getStatus()+" Worker ID: "+info.getWorkerId());
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
            workerId = Integer.valueOf(cmdLine[1]);
            
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
            System.out.println("no such process class "+cmdLine[2]);
            return;
        }
        
        processId++;
        Message cmdMessage = new Message(Message.msgType.COMMAND);
        cmdMessage.setCommandId(CommandType.START);
        cmdMessage.setArgs(args);
        cmdMessage.setProcessName(cmdLine[2]);
        cmdMessage.setProcessId(processId);
        
        
        if(processServerMap.containsKey(workerId)){
            try{
                processServerMap.get(workerId).sendToWorker(cmdMessage);
                ProcessInfo procInfo = new ProcessInfo();
                procInfo.setId(processId);
                procInfo.setName(cmdLine[2]);
                procInfo.setWorkerId(workerId);
                procInfo.setStatus(Status.STARTING);
                processesMap.put(processId, procInfo);
            }catch (IOException e) {
                e.printStackTrace();
                System.out.println("start Command sent failed, remove worker "+workerId);
                removeNode(workerId);
            }
        }
        else{
            System.out.println("there is no server for workerId "+workerId);
        }
            
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
        if (procInfo.getStatus().equals(ProcessInfo.Status.RUNNING.toString())) {
                Message killCommand =new Message(Message.msgType.COMMAND);
                killCommand.setCommandId(CommandType.KILL);
                killCommand.setProcessId(procId);
                int workerId = procInfo.getWorkerId();
                try{
                    processServerMap.get(workerId).sendToWorker(killCommand);
                    processesMap.get(procId).setStatus(Status.TERMINATING);
                }catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Kill Command sent failed, remove worker "+workerId);
                    removeNode(workerId);
                }
                
                
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
        
        if(processesMap.get(procId).getStatus() != Status.RUNNING.toString()){
            System.out.println("the process "+procId+"is not running");
        }
        
        Message migrateCommand = new Message(Message.msgType.COMMAND);
        migrateCommand.setCommandId(CommandType.MIGARATESOURCE);
        migrateCommand.setProcessId(procId);
        migrateCommand.setSourceId(sourceId);
        migrateCommand.setTargetId(targetId);
        
        try{
            processServerMap.get(sourceId).sendToWorker(migrateCommand);
            processesMap.get(procId).setStatus(Status.TRASFERING);
        }catch (IOException e) {
            e.printStackTrace();
            System.out.println("Migrate Command sent failed, remove worker "+sourceId);
            removeNode(sourceId);
        }
        



    }
    
    private void handleHelp(String[] cmdLine){
        System.out.println("Commands List:");
        System.out.println("ls : list all the worker node in the system");
        System.out.println("ps : list all the processes");
        System.out.println("start <worker id> <process name> <args[]> : start the process on the designated worker");
        System.out.println("migrate <process id> <source id> <target id> : migrate process from source to target worker");
        System.out.println("kill <process id> : kill the process");
    }
    public void removeNode(int id){
        processServerMap.get(id).stop();
        //processServerMap.remove(id);
        //workersMap.remove(id);
        workerStatusMap.put(id, -1);
        Set<Integer> procIdSet = processesMap.keySet();
        Iterator<Integer> idIterator = procIdSet.iterator();
        int procId;
        while(idIterator.hasNext()){
            procId = idIterator.next();
            if(processesMap.get(procId).getWorkerId() == id){
                processesMap.get(procId).setStatus(Status.FAILED);
                
            }
        }
    }
    
    private void startServer(int port){

        Thread t1 = new Thread(connServer);
        t1.start();
    }
    
    private void terminate(){
        int workerId;
        Message tmntCommand = new Message(Message.msgType.COMMAND);
        tmntCommand.setCommandId(CommandType.SHUTDOWN);
        Set<Integer> workerIdSet = processServerMap.keySet();
        Iterator<Integer> workerIterator= workerIdSet.iterator();
        /*shut down the worker node*/
        while(workerIterator.hasNext()){
            workerId = workerIterator.next();
            try{
                processServerMap.get(workerId).sendToWorker(tmntCommand);
                processServerMap.get(workerId).stop();
            }catch (IOException e) {
                processServerMap.get(workerId).stop();
                e.printStackTrace();
                System.out.println("shutdown Command sent failed, remove worker "+workerId);
                removeNode(workerId);
            }
            
        }
        /*shut down the console and connection server*/
        try{
            console.close();
        }catch(IOException e){
            
        }
        connServer.stop();
        
    }
    
    private void checkWorkerLiveness(){
        //System.out.println("monitor timer expire!");
        
        Set<Integer> workerIdSet = workerStatusMap.keySet();
        Iterator<Integer> idIterator = workerIdSet.iterator();
        while(idIterator.hasNext()){
            int id = idIterator.next();
            if(workerStatusMap.get(id).intValue() == -1)
                continue;
            else if(workerStatusMap.get(id).intValue() > 1){
                System.out.println("worker "+id+" is not alive. remove it");
                removeNode(id);
            }
            else{
                workerStatusMap.put(id, workerStatusMap.get(id).intValue()+1);
            }
                
        }
    }
    
    public void startMoniterTimer(){
        Timer timer = new Timer(true);
        TimerTask task = new TimerTask(){
            public void run(){
                checkWorkerLiveness();
            }
        };
        timer.schedule(task, 0, 10*1000);
        System.out.println("start the monitor timer");
        
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
        System.out.println("go to start timer");
        manager.startMoniterTimer();
        manager.startConsole();
        
        
        
        
    }
    
}