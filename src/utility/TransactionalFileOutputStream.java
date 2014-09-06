package utility;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.RandomAccessFile;

import java.io.File;
/*the TransctionalFileInputStream class will remember the state of the file and use
 * RandmAcessFile to seek to the offset and continue the reading. It's useful when migrate the process*/
public class TransactionalFileOutputStream extends OutputStream implements
Serializable{
    private File outputFile;
    private int offSet;
    boolean migrateState;
    private transient RandomAccessFile fileHdl;
    public TransactionalFileOutputStream(String string) {
        offSet =0;
        migrateState = false;
        outputFile = new File(string);
        
    }

    @Override
    /*when migrate, need to reopen the file and seek to the offset position*/
    public void write(int w) throws IOException {
        
        if(migrateState || (fileHdl == null)){
           fileHdl = new RandomAccessFile(outputFile,"rw"); 
           fileHdl.seek(offSet);
           migrateState = false;
        }
           fileHdl.write(w);
           offSet++;
           
        
    }
    @Override
    public void close() throws IOException {
        fileHdl.close();
    }
    
    public void setMigrateState(boolean state){
        migrateState = state;
    }
    
}