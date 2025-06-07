import hk.quantr.java.mpremote.JavaMPremote;
import org.junit.Test;

public class TestFS {
	@Test
	public void test() {
		// Example: adjust port and baud rate as needed for your device
		String port = "/dev/tty.usbmodem209D367E42472"; // Change to your device
		int baud = 115200;
		try {
			boolean connected = JavaMPremote.connect(port, baud);
			org.junit.Assert.assertTrue("Connection failed", connected);
			String files = hk.quantr.java.mpremote.JavaMPremote.fsLs("/flash");
			System.out.println("Files: " + files);
			org.junit.Assert.assertNotNull(files);
			org.junit.Assert.assertFalse(files.isEmpty());
		} catch (Exception e) {
			e.printStackTrace();
			org.junit.Assert.fail(e.getMessage());
		} finally {
			hk.quantr.java.mpremote.JavaMPremote.disconnect();
		}
	}
}
