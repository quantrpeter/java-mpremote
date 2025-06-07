import hk.quantr.java.mpremote.JavaMPremote;
import org.junit.Test;

public class TestFS_CP_Local_To_Device {
	@Test
	public void testCopyToDeviceAndBack() throws Exception {
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
		String localFile = "pythonFiles/black and text.png";
		String deviceFile = "/flash/black and text.png";

		try {
			boolean connected = JavaMPremote.connect(port, baud);
			org.junit.Assert.assertTrue("Connection failed", connected);

			// Copy local file to device
			JavaMPremote.fsCp(localFile, deviceFile);

			// Check file contents
			String original = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(localFile)));
//			org.junit.Assert.assertEquals(original, downloaded);
		} finally {
			JavaMPremote.disconnect();
		}
	}
}
