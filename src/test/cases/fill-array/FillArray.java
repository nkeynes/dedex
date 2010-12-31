import org.junit.Test;
import static org.junit.Assert.*;

public class FillArray {

    public final static byte[] BYTE_ARRAY = { 0, 1, 2, 3, 4 };
    public final static char[] CHAR_ARRAY = { 'A', 'B', 'C', 'D' };
    public final static short[] SHORT_ARRAY = { 0x0123, 0x2345, 0x4567, 0x6789 };
    public final static int[] INT_ARRAY = { 0x01234567, 0x12345678, 0x23456789 };
    public final static float[] FLOAT_ARRAY = { 1.0F, 2.0F, 3.0F };
    public final static long[] LONG_ARRAY = { 0x0123456789ABCDEFL, 0x123456789ABCDEF0L };
    public final static double[] DOUBLE_ARRAY = { 1.0, 2.0, 3.0 };
    
    @Test
    public void testByte() {
        assertEquals(BYTE_ARRAY.length, 5);
        for( int i=0; i<5; i++ ) {
            assertEquals(BYTE_ARRAY[i], i);
        }
    }
    
    @Test
    public void testChar() {
        assertEquals(CHAR_ARRAY.length, 4);
        for( int i=0; i<4; i++ ) {
            assertEquals(CHAR_ARRAY[i], 'A'+i);
        }
    }
            
}
