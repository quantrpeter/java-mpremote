
import hk.quantr.java.mpremote.JavaMPremote;
import org.junit.Test;

public class TestFS_RM {

	@Test
	public void testRemoveFile() throws Exception {
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
		String testFile = "black and text.png";
		String deviceFile = "/flash/" + testFile;
		try {
			boolean connected = JavaMPremote.connect(port, baud);
			org.junit.Assert.assertTrue("Connection failed", connected);
			// Create a file on device
			JavaMPremote.sendCommand("with open('" + deviceFile + "', 'w') as f: f.write('to be deleted')");
			// Ensure file exists
			String files = JavaMPremote.fsLs("/flash");
			org.junit.Assert.assertTrue(files.contains(testFile));
			// Remove the file
			JavaMPremote.fsRm(deviceFile);
			// Ensure file is gone
			String filesAfter = JavaMPremote.fsLs("/flash");
			org.junit.Assert.assertFalse(filesAfter.contains(testFile));
		} finally {
			JavaMPremote.disconnect();
		}
	}
}
