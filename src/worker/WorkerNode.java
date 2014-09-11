package worker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;




import java.util.HashMap;

import javax.management.timer.Timer;

import utility.Message;
import utility.ProcessInfo;
/*
 * The workerNode is the slave of the process manager. It is a seperate running machine which is connected 
 * to the process manager with socket. When it is started, the port and IP address of the processmanager
 * is set.  Its responsibilities including:
 * 	Response to the CMD from the process manager.
 *  send message every 5 seconds to the process manager to inform its own status. 
 *  
 *  @Author Yifan Li
 *  @Author Jian Wang
 **/
import utility.Message.msgType;
import utility.ProcessInfo.Status;
import utility.ResponseType;
import processes.MigratableProcess;

public class WorkerNode {
// properties
	// socket related properties
	private String host;
	private int port;
	private Socket socket;
	
	// object I/O port
	private ObjectInputStream obis;
	private ObjectOutputStream obos;

	private boolean failure = false; // failure == ture means the process died!
	private int workerID;
	// process related properties
	private Thread t;
	private HashMap<Integer, MigratableProcess> currentMap;
	
	//worker information report 
	WorkerInfoReport workerinfo;
	
// methods
 	//constructing method
	public WorkerNode(){
		this.host = null;
		this.port = 0;
		this.workerID = 0;
		this.currentMap = new HashMap<Integer, MigratableProcess>();
		this.workerinfo= new WorkerInfoReport();
	}
	public WorkerNode(String host, int port){
		this.host=host;
		this.port = port;
		this.workerID = 0;
		this.currentMap = new HashMap<Integer, MigratableProcess>();
		this.workerinfo = new WorkerInfoReport();
	}
	
	//command handling methods
	private void handle_kill(Message msg) {
		System.out.println("Start to kill the process!");
		//response message prepared!
		Message response = new Message(msgType.RESPONSE);
		response.setResponseId(ResponseType.KILLRES);
		MigratableProcess mp = currentMap.get(msg.getProcessId());
		currentMap.remove(msg.getProcessId());
		mp.suspend();

		currentMap.remove(mp);
		
		response.setProcessId(mp.getProcessID());
		response.setResult(Message.msgResult.SUCCESS);
		
		//send the response back
		sendToManager(response);		
		System.out.println("Killing process finished!");
	}
	
	// receive the process from the process manager
	private void handle_migratetarget(Message msg) {
		System.out.println("Start to  migrate process to this machine!");
		
		//response prepared
		Message response = new Message(msgType.RESPONSE);
		response.setResponseId(ResponseType.MIGRATETARGETRES);
		response.setProcessId(msg.getProcessId());
		response.setTargetId(workerID);
		//continue the process here
		MigratableProcess mp = msg.getProcessObject();
		System.out.println("object received, ready to go!");
		runProcess(mp);
		
		response.setResult(Message.msgResult.SUCCESS);
		
		//send the response back
		sendToManager(response);
		System.out.println("Process migration finished!");
	}
	
	// handle the migrate from command. The worker received should package the process and diliver it to the master
	private void handle_migratesource(Message msg) {
		System.out.println("Start migrate process from this machine!");
		// response prepared!
		Message response = new Message(msgType.RESPONSE);
		response.setResponseId(ResponseType.MIGARATESOURCERES);

		// package the process to be transfered!
		MigratableProcess mp = currentMap.get(msg.getProcessId());
		
		//check the status of the process
		if(mp.getStatus() != Status.RUNNING){//if the process is not in the RUNNING state,
			response.setResult((Message.msgResult.FAILURE));
			response.setCause("The process is not running!");
			response.setProcessId(msg.getProcessId());
			response.setStatus(mp.getStatus());
			sendToManager(response);
			System.out.println("Migration failed ! The process: "+msg.getProcessId()+" is not running!");
		}
		else{ // if it is running, do the migration
			mp.suspend();
			currentMap.remove(mp.getProcessID());
			response.setTargetId(msg.getTargetId());
			response.setProcessObject(mp);
			response.setProcessId(msg.getProcessId());
			response.setResult(Message.msgResult.SUCCESS);
		
			// send the response
			sendToManager(response);
			System.out.println("Migration finished!");
		}
	}
	// using reflection to construct the process and find the class by the name of it
	private void handle_start(Message msg) {
		
		System.out.println("Handle start process cmd!");
		// response message prepared!
		Message response=new Message(msgType.RESPONSE);
		response.setResponseId(ResponseType.STARTRES);
		response.setProcessId(msg.getProcessId());
		response.setProcessName(msg.getProcessName());
		//start the process
		Class processClass;
		try {
			processClass = WorkerNode.class.getClassLoader().loadClass(
					msg.getProcessName());
		
			Constructor constructor;
			constructor = processClass.getConstructor(String[].class);
			System.out.println(msg.getArgs().length);
			Object[] passed = { msg.getArgs() };
			MigratableProcess process = (MigratableProcess) constructor.newInstance(passed);
			process.setProcessID(msg.getProcessId()); // method needed to be created in MigratableProcess
			System.out.println("run process");
			runProcess(process);
		}catch (ClassNotFoundException e) {
		    failure = true;
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Class not Found!");
		}catch (NoSuchMethodException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("No such method!");
		} catch (SecurityException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Security Exception!");
		} catch (InstantiationException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Instantiation Exception!");
		} catch (IllegalAccessException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Illegal Access !");
		} catch (IllegalArgumentException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Illegal Argument!");
		} catch (InvocationTargetException e) {
			response.setResult(Message.msgResult.FAILURE);
			response.setCause("Invocation Target Exception!");
		}
		// send the response back to the master
		response.setResult(Message.msgResult.SUCCESS);
		sendToManager(response);
		System.out.println("Process has been started!");
	}
	
	 // handle the command assign id 
	private void handle_assignID(Message msg){
		this.workerID =msg.getWorkerID(); 
	}
	
	// handle the command clear
	private void  handle_clear(Message msg){
		this.currentMap.remove(msg.getProcessId());
		System.out.println("Process:"+msg.getProcessId()+"is cleared!");
	}
	
	// hanld the command exit
	
	private void handle_exit(Message msg){
		System.out.println("shutdown!");
		System.exit(0);
	}
	private void startreport(){
		Thread t1 = new Thread(workerinfo);
		t1.start();
	}
	
	// some auxiliary methods
	
		// create a thread to run the process
		private  void runProcess(MigratableProcess mp) {
			System.out.println("start process");
			t = new Thread(mp);
			t.start();
			mp.setStatus(ProcessInfo.Status.RUNNING);
			System.out.println("mg getProcessID "+mp.getProcessID());
			currentMap.put(mp.getProcessID(), mp);
		}
		
		//send method writes object into output stream
		private void sendToManager(Message sc) {
			try {
				obos.writeObject(sc);
			} catch (IOException e) {
				System.err.println("fail to send manager");
			}
		}
		
		
	public static void main(String [] args){
		//start only when there are two arguments 
		if(args.length == 2){
			// establish the connection first.
			String host = args[0];
			int port = Integer.parseInt(args[1]);
			WorkerNode worker = new WorkerNode(host, port);
			
			
			try{
				worker.socket = new Socket(host,port);
			}catch(IOException e){
				worker.failure = true;
				System.err.println("Socket creation failure!");
				System.exit(0);
			}
			System.out.println("Socket creation succeded!");
			
			//establish the object IO tunnel

			try {
				worker.obos = new ObjectOutputStream(
						worker.socket.getOutputStream());
				worker.obis = new ObjectInputStream(
						worker.socket.getInputStream());
			} catch (IOException e) {
				worker.failure = true;
				System.err.println("cannot create stream");
				e.printStackTrace();
			}

			//worker info backend started
			worker.startreport();
		
			
			//wait for the CMDs and deal with them
			while(!worker.failure){
				try {
					Message master_cmd = (Message) worker.obis.readObject();
					switch(master_cmd.getCommandId()){
						case ASSIGNID:// this command assignes the id to the process
							worker.handle_assignID(master_cmd);
							break;
						case START:// this command tries to start a process on this worker
							worker.handle_start(master_cmd);
							break;
						case MIGARATESOURCE: // this command tries to migrate a process from this worker
							worker.handle_migratesource(master_cmd);
							break;
						case MIGRATETARGET: // this command tries to migrate a process to this worker
							worker.handle_migratetarget(master_cmd);
							break;
						case KILL:	// this command tries to kill a process on this worker
							worker.handle_kill(master_cmd);
							break;
						case SHUTDOWN:// this command shutdown this worker
							worker.handle_exit(master_cmd);
							break;
						case REMOVEPROC:
							worker.handle_clear(master_cmd);
							break;
						default:
							System.out.println("Wrong cmd:"+master_cmd.getCommandId());
							break;
					}
					
				} catch (ClassNotFoundException e) {
					worker.failure = true;
					System.out.println("Class not found!");
				} catch (IOException e) {
					worker.failure = true;
					System.out.println("Cannot read from the stream!");
				}
			}
			try {
				worker.obos.close();
				worker.obis.close();
				worker.socket.close();
				System.out.println("Process Worker closed");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			System.out.println("Please enter the host ip and port number");
		}
	}
	
	// worker info method 
	public class WorkerInfoReport implements Runnable{

		@Override
		public void run() {
			// send the info about the current process running information every 5 seconds
			while(!failure){
				Message response=new Message(msgType.RESPONSE);
				response.setResponseId(ResponseType.PULLINFORES);
				
				HashMap<Integer,ProcessInfo.Status> workerinfo = new HashMap<Integer, ProcessInfo.Status>();
				
				for(int i:currentMap.keySet()){
					MigratableProcess mp = currentMap.get(i);
					if(mp.isComplete()){
						mp.setStatus(ProcessInfo.Status.FINISHED);
					}
					workerinfo.put(i, mp.getStatus());
				}
				response.setWorkerInfo(workerinfo);
				sendToManager(response);
				//System.out.println("report the processes states to the manager!");
				try {
					Thread.sleep(5 * Timer.ONE_SECOND);
				} catch (InterruptedException e) {
					//System.out.println(e.toString());
				}
			}
		}
		
	}
}





