
import hk.quantr.java.mpremote.JavaMPremote;
import org.junit.Test;

public class TestSendCommand {

	@Test
	public void test() {
		// list all files starts with /dev/tty.usbmod and pick the first one to variable "port"
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
			String command = "import sys\n"
					+ "from machine import Pin, I2C, SoftI2C\n"
					+ "import ssd1306\n"
					+ "\n"
					+ "# Get x and y from script arguments, default to 0 if not provided\n"
					+ "print(sys.argv)\n"
					+ "\n"
					+ "i2c = I2C(1)  # Create I2C object\n"
					+ "display = ssd1306.SSD1306_I2C(128, 64, i2c)  # Pass I2C object\n"
					+ "\n"
					+ "display.fill(0)\n"
					+ "display.text(\"Hello, World\", 15, 3)\n"
					+ "display.show()\n"
					+ "\n";
			System.out.println(command);
			String result = JavaMPremote.sendCommand(command);
			System.out.println("sendCommand output: " + result);
		} catch (Exception e) {
			e.printStackTrace();
			org.junit.Assert.fail(e.getMessage());
		} finally {
			JavaMPremote.disconnect();
		}
	}
}
