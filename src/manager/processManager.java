package manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;

import utility.*;

/**
* ProcessManager serve as the master of this whole system. It will open a command
* line console for user input and start the connectionServer for listening to the 
* socket connect from worker node.abstract
*/

public class processManager {
    private ConcurrentHashMap<Integer,processInfo> processesMap;
    private ConcurrentHashMap<Integer,String> workersMap;/*worker node ip address for every id*/
    private ConcurrentHashMap<Integer,processServer> processServerMap;/*processServer for every worker node Id*/
    private connectionServer connServer;
    private int port;
    private BufferedReader console;
    private int processId;
    
    public void ProcessManager(int listenPort){
        port = listenPort;
        console = new BufferedReader(new InputStreamReader(System.in));
        connServer = new connectionServer(port,this);
        processesMap = new ConcurrentHashMap<Integer,processInfo>();
        workersMap = new ConcurrentHashMap<Integer,String>();
        processServerMap = new ConcurrentHashMap<Integer,processServer>();
        processId = 0;
        
    }
    
    public void startConsole(){
        System.out.println("This is process manager, type help for more information");
        String cmdLine=null;
        while(true){
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
                    //handleMigrateProcess(inputLine);
                    break;
                case "kill":
                    //handleKillProcess(inputLine);
                    break;
                case "ls":
                    handleLs(inputLine);
                    break;
                case "ps":
                    handlePs(inputLine);
                    break;
                case "help":
                    //handleHelp(inputLine);
                    break;
                case "exit":
                    //closeConsole();
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
                System.out.println("worker ID: "+i+"  IP Address: "+workersMap.get(i));
        }
    }
    
    private void handlePs(String[] cmdLine){
        if(0 == processesMap.size())
            System.out.println("no process information");
        else{
            for(int i : processesMap.keySet()){
                processInfo info = processesMap.get(i);
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
            workerId = Integer.getInteger(cmdLine[0]);
            
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
            Class process = processManager.class.getClassLoader().loadClass(cmdLine[2]);
        }catch(ClassNotFoundException e){
            System.out.println("no such process class");
            return;
        }
        //try{
            processId++;
            message cmdMessage = new message(message.msgType.COMMAND);
            cmdMessage.setCommandId(commandType.START);
            cmdMessage.setArgs(args);
            cmdMessage.setProcessName(cmdLine[2]);
            cmdMessage.setProcessId(processId);
            processServerMap.get(workerId).sendToWorker(cmdMessage);
        //}catch(IOException e){
         //   System.out.println("contact with worker node "+workerId+" failed, remove it!");
         //   removeNode(workerId);
        //}
        
            
    }
    
    private void removeNode(int id){
        processServerMap.get(id).stop();
        processServerMap.remove(id);
        workersMap.remove(id);
    }
    
}