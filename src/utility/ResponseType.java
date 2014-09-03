package utility;

/**
* This file define the command type used to send from manager to worker and 
* the response type used to send from worker to manager
*/

public enum ResponseType {
    PULLINFORES,
    STARTRES,
    MIGARATESOURCERES,/*manager to source worker*/
    MIGRATETARGETRES, /*manager to target worker*/
    KILLRES
}