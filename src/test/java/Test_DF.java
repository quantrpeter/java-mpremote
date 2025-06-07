import hk.quantr.java.mpremote.JavaMPremote;
import org.junit.Test;

public class Test_DF {
    @Test
    public void testDF() throws Exception {
        // Find the first /dev/tty.usbmod* device
        String port = null;
        java.io.File devDir = new java.io.File("/dev");
        String[] candidates = devDir.list(new java.io.FilenameFilter() {
            @Override
            public boolean accept(java.io.File dir, String name) {
                return name.startsWith("tty.usbmod");
            }
        });
        if (candidates != null && candidates.length > 0) {
            port = "/dev/" + candidates[0];
        } else {
            throw new RuntimeException("No /dev/tty.usbmod* device found");
        }

        int baud = 115200;
        try {
            boolean connected = JavaMPremote.connect(port, baud);
            org.junit.Assert.assertTrue("Connection failed", connected);
            String dfResult = JavaMPremote.df();
            System.out.println("DF Result:\n" + dfResult);
        } finally {
            JavaMPremote.disconnect();
        }
    }
}
