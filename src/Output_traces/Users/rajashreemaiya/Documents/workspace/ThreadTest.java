/**
 * 
 */
package Output_traces.Users.rajashreemaiya.Documents.workspace;

/**
 * @author rmaiya
 *
 */
import java.util.*;

public class ThreadTest extends Thread    {
        private String info;
        private Vector aVector = new Vector();

        public ThreadTest (String info) {
                this.info = info;
        }

        public void inProtected () {
           synchronized ( aVector )     {
                System.err.println(info + ": is in protected()");
                try {
                        sleep(3000);
                }
                catch (  InterruptedException e ) {
                        System.err.println("Interrupted!");
                }
                System.err.println(info + ": exit run");
           }
        }

        public void run () {
                inProtected();
        }

        public static void main (String args []) {
                ThreadTest aT5_0 = new ThreadTest("first");
                ThreadTest aT5_1 = new ThreadTest("second");

                aT5_0.start();
                aT5_1.start();
        }
}
