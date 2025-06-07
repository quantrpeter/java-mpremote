import hk.quantr.java.mpremote.JavaMPremote;
import org.junit.Test;

public class TestSendCommand {
	@Test
	public void test() {
		String port = "/dev/tty.usbmodem209D367E42472"; // Change to your device's port
		int baud = 115200;
		try {
			boolean connected = JavaMPremote.connect(port, baud);
			org.junit.Assert.assertTrue("Connection failed", connected);
			String command="import sys\n" + //
								"from machine import Pin, I2C, SoftI2C\n" + //
								"import ssd1306\n" + //
								"\n" + //
								"# Get x and y from script arguments, default to 0 if not provided\n" + //
								"print(sys.argv)\n" + //
								"\n" + //
								"i2c = I2C(1)  # Create I2C object\n" + //
								"display = ssd1306.SSD1306_I2C(128, 64, i2c)  # Pass I2C object\n" + //
								"\n" + //
								"display.fill(0)\n" + //
								"display.text(\"Hello, World\", 10, 20)\n" + //
								"display.show()\n" + //
								"\n";
			String result = JavaMPremote.sendCommand(command);
			System.out.println("sendCommand output: " + result);
			org.junit.Assert.assertTrue(result.contains("12345"));
		} catch (Exception e) {
			e.printStackTrace();
			org.junit.Assert.fail(e.getMessage());
		} finally {
			JavaMPremote.disconnect();
		}
	}
}
