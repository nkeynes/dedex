import org.junit.Test;
import static org.junit.Assert.*;

public class InnerClass {

	private boolean done = false;
	
    protected class Internal {
        public int count;
        Runnable runner;
        
        Internal( Runnable runner ) {
            this.runner = runner;
            this.count = 0;
        }
        
        protected void runOnce() {
        	if( this.runner != null ) {
        		this.runner.run();
        		this.runner = null;
        	}
        }
        
        protected Runnable getRunner() {
        	return runner;
        }
    }
    
    public Internal getInternal() {
        
        Runnable run = new Runnable() { 
            public void run() {
            	done = true;
            }
        };
        
        return new Internal(run);
    }
    
    @Test
    public void test() {
    	Internal inner = getInternal();
    	assertEquals(done, false);
    	assertNotNull(inner.runner);
    	inner.runOnce();
    	assertEquals(done, true);
    	assertNull(inner.runner);
    }
}
