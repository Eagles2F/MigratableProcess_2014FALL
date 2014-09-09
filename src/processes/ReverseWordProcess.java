package processes;
import java.io.PrintStream;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.Thread;
import java.lang.InterruptedException;

import utility.*;
import utility.ProcessInfo.Status;
/*this process reverse the word in the line from the source file and 
 * save to the output file and it will remove the extra space. 
 * for example:
 * input file: this is distributed system
 * output file: system distributed is this*/

public class ReverseWordProcess extends MigratableProcess
{
	private TransactionalFileInputStream  inFile;
	private TransactionalFileOutputStream outFile;
	private boolean completed;
	private volatile boolean suspending;

	public ReverseWordProcess(String args[]) throws Exception
	{
		if (args.length != 2) {
			System.out.println("usage: ReverseWordProcess <inputFile> <outputFile>");
			throw new Exception("Invalid Arguments");
		}
		
		
		inFile = new TransactionalFileInputStream(args[0]);
		outFile = new TransactionalFileOutputStream(args[1]);
		this.completed =false;
	}

	public void run()
	{
		PrintStream out = new PrintStream(outFile);
		DataInputStream in = new DataInputStream(inFile);
		
		try {
		    int i,m,j=0;
            int space =0;
            String output = "";
            String word = "";
		
			while (!suspending) {
				String line = in.readLine();
				output = "";
				word = "";
				if(line == null)
				    break;
		        if(line.length() == 0){
		            continue;
		        }
		        
		        for(i=0;i<line.length();i++){
		            
		            if((line.charAt(i) == ' ') ){
		                if(space == 0){
		                    output = output.equals("")?word:word +" " +output;
		                    word = "";
		                    space = 1;
		                }
		            }else{
		                
		                space = 0;
		                word += String.valueOf(line.charAt(i));
		            }
		        }
		        if(word.equals("")){
		            
		        }else{
		            if(output.equals("")){
		                output = word;
		            }else{
		                output = word +" "+ output;
		            }
		        }
				out.println(output);
				// Make grep take longer so that we don't require extremely large files for interesting results
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// ignore it
				}
			}
		//}
		} catch (EOFException e) {
			//End of File
		    System.out.println("ReverseWordProcess: end of file");
		} catch (IOException e) {
			System.out.println ("ReverseProcess: Error: " + e);
		}

		if(suspending == true){
    		System.out.println("set suspending false");
    		suspending = false;
		}else{
		    System.out.println("process RverseWordProcess finished");
		    complete = true;
		}
	}

	public void suspend()
	{
		suspending = true;
		while(suspending);
            
	}

	
    

}