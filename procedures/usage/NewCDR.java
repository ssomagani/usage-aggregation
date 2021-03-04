package usage;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.types.TimestampType;

public class NewCDR extends VoltProcedure {

    
    private static final SQLStmt INSERT_COUNTER = new SQLStmt("INSERT INTO counter_list values (?, ?, ?, ?, ?, ?)");
    
    public VoltTable[] run(int userId, int subId, int counterId, 
            int counterValue, int columnType, TimestampType insertTs) 
            throws VoltAbortException {
        voltQueueSQL(INSERT_COUNTER, counterId, subId, userId, counterValue, columnType, insertTs);
        return voltExecuteSQL();
    }
}
