package usage;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.VoltProcedure.VoltAbortException;

public class NewCDR extends VoltProcedure {

    
    private static final SQLStmt INSERT_GRP = new SQLStmt("");
    
    public VoltTable run() throws VoltAbortException {
        return null;
    }
}
