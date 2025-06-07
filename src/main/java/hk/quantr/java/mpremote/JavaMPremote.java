package hk.quantr.java.mpremote;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author Peter <peter@quantr.hk>
 */
public class JavaMPremote {

	private static SerialPort serialPort;
	private static InputStream in;
	private static OutputStream out;

	public static boolean connect(String portDescriptor, int baudRate) {
		serialPort = SerialPort.getCommPort(portDescriptor);
		serialPort.setBaudRate(baudRate);
		serialPort.setNumDataBits(8);
		serialPort.setNumStopBits(SerialPort.ONE_STOP_BIT);
		serialPort.setParity(SerialPort.NO_PARITY);
		if (serialPort.openPort()) {
			in = serialPort.getInputStream();
			out = serialPort.getOutputStream();
			return true;
		} else {
			return false;
		}
	}

	public static void disconnect() {
		try {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			if (serialPort != null) {
				serialPort.closePort();
			}
		} catch (Exception e) {
			// ignore
		}
	}

	public static void repl() throws Exception {
		if (serialPort == null || !serialPort.isOpen()) {
			throw new IllegalStateException("Not connected");
		}
		byte[] buffer = new byte[1024];
		int len;
		while ((len = in.read(buffer)) > 0) {
			String s = new String(buffer, 0, len);
			System.out.print(s);
			if (s.contains(">>> ")) {
				break; // REPL prompt
			}
		}
	}
	
	public static void sendCommand(){
		
	}

	/**
	 * List files in the given path on the MicroPython device.
	 *
	 * @param path Path to list (e.g. "/" or "/flash")
	 * @return Output from the device (file list)
	 */
	public static String fsLs(String path) throws Exception {
		ensureConnected();
		enterRawRepl();
		String cmd = "import os; print('::'.join(os.listdir('" + path + "')));";
		out.write(cmd.getBytes());
		out.flush();
		out.write(4); // Send EOT to signal end of input
		out.flush();
		String result = readUntilPrompt();
		exitRawRepl();
		// Parse result: remove echoed command and prompt
		int idx = result.indexOf("::");
		if (idx >= 0) {
			return result.substring(idx).replace("::", "\n");
		}
		return result;
	}

	/**
	 * Run a Python script on the device.
	 *
	 * @param script Python code to run
	 * @return Output from the device
	 */
	public static String run(String script) throws Exception {
		ensureConnected();
		enterRawRepl();
		out.write((script + "\r\n").getBytes());
		out.flush();
		out.write(4); // Send EOT
		out.flush();
		String result = readUntilPrompt();
		exitRawRepl();
		return result;
	}

	/**
     * Send a raw Python command to the device in raw REPL mode and return the output.
     * @param command Python code to execute
     * @return Output from the device
     */
    public static String sendCommand(String command) throws Exception {
        ensureConnected();
        enterRawRepl();
        out.write((command + "\r\n").getBytes());
        out.flush();
        out.write(4); // Send EOT
        out.flush();
        String result = readUntilPrompt();
        exitRawRepl();
        return result;
    }

	// --- Helper methods ---
	private static void ensureConnected() {
		if (serialPort == null || !serialPort.isOpen()) {
			throw new IllegalStateException("Not connected");
		}
	}

	private static void enterRawRepl() throws Exception {
		// Ctrl-A to enter raw REPL
		out.write(0x01);
		out.flush();
		Thread.sleep(100); // Give device time
		in.skip(in.available()); // Clear input buffer
	}

	private static void exitRawRepl() throws Exception {
		// Ctrl-B to exit raw REPL
		out.write(0x02);
		out.flush();
		Thread.sleep(100);
		in.skip(in.available());
	}

	private static String readUntilPrompt() throws Exception {
		StringBuilder sb = new StringBuilder();
		byte[] buffer = new byte[1024];
		int len;
		long start = System.currentTimeMillis();
		while (true) {
			if (in.available() > 0 && (len = in.read(buffer)) > 0) {
				String s = new String(buffer, 0, len);
				System.out.println(s);
				sb.append(s);
//				if (sb.toString().contains(">")){
//					System.out.println("");
//                    // print all bytes in s in hex format
//                    for (int i = 0; i < sb.length(); i++) {
//                        System.out.printf("%02X ", (int) sb.charAt(i));
//                    }
//                    System.out.println("");
//				}
				// if sb contains 0D 0A 04 04 3E 
				if (sb.toString().contains("\r\n" + (char) 4 + (char) 4 + ">")) {
					break;
				}
			}
			if (System.currentTimeMillis() - start > 10000) {
				break; // Timeout
			}
		}
		String output = sb.toString();
		//replace last 0D 0A 04 04 3E to empty
        output = output.replace("\r\n" + (char) 4 + (char) 4 + ">", "");
		return output;
	}

	// Add more methods for cp, etc. using serial communication
}
