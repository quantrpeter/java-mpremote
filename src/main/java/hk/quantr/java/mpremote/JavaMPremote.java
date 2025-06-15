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

	public static boolean connect(SerialPort serialPort) {
		JavaMPremote.serialPort = serialPort;
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

	public static void sendCommand() {

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
	 *
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
		if (result.startsWith("OK")) {
			result = result.substring(2);
		}
		exitRawRepl();
		return result;
	}

	/**
	 * Copy a file to or from the MicroPython device, supporting large files by chunking. If src starts with "/", it is on the device; otherwise, it is local. If dest starts with
	 * "/", it is on the device; otherwise, it is local.
	 *
	 * @param src Source file path (local or device)
	 * @param dest Destination file path (local or device)
	 * @throws Exception on error
	 */
	public static void fsCp(String src, String dest) throws Exception {
		ensureConnected();
		if (src.startsWith("/")) {
			// Device to local (no chunking needed, assume file fits in memory)
			enterRawRepl();
			String cmd = "with open('" + src + "', 'rb') as f: import ubinascii; print(ubinascii.b2a_base64(f.read()).decode())";
			System.out.println(cmd);
			out.write((cmd + "\r\n").getBytes());
			out.flush();
			out.write(4); // EOT
			out.flush();
			String result = readUntilPrompt();
			if (result.startsWith("OK")) {
				result = result.substring(2);
			}
			exitRawRepl();
			// Extract base64 data
			String base64 = result.replaceAll(".*?([A-Za-z0-9+/=\r\n]+).*", "$1").replaceAll("\r\n", "");
			byte[] data = java.util.Base64.getDecoder().decode(base64);
			try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
				fos.write(data);
			}
		} else if (dest.startsWith("/")) {
			int CHUNK_SIZE = 1024; // Reduce chunk size for reliability
			// Local to device (chunked)
			byte[] data;
			try (java.io.FileInputStream fis = new java.io.FileInputStream(src)) {
				data = fis.readAllBytes();
			}
			// Overwrite file first
			sendCommand("f=open('" + dest + "','wb');f.close()");
			// Send in chunks
			int offset = 0;
			while (offset < data.length) {
				System.out.println("Sending chunk at offset " + offset + " of size " + data.length);
				int len = Math.min(CHUNK_SIZE, data.length - offset);
				byte[] chunk = java.util.Arrays.copyOfRange(data, offset, offset + len);
				String base64 = java.util.Base64.getEncoder().encodeToString(chunk);

				// import ubinascii; f=open('/flash/ssd1306.py','ab'); f.write(ubinascii.a2b_base64(123)); f.close()
				sendCommand("import ubinascii; f=open('" + dest + "','ab'); f.write(ubinascii.a2b_base64('" + base64 + "')); f.close()");
				// print the real file size on device
				String sizeCmd = "import os; print(os.stat('" + dest + "')[6])"; // 6 is the size index in os.stat tuple
				String sizeResult = JavaMPremote.sendCommand(sizeCmd);
				System.out.println("Current file size on device: " + sizeResult);

				offset += len;
				Thread.sleep(100); // Allow device to process chunk
			}
		} else {
			throw new IllegalArgumentException("Either src or dest must be a device path (start with /)");
		}
	}

	/**
	 * Get free and total space on all mount points on the MicroPython device (like 'df').
	 *
	 * @return String with columns: mount, size, used, avail, use%
	 */
	public static String df() throws Exception {
		String py
				= "import os\n"
				+ "mounts = []\n"
				+ "for d in os.listdir('/'):\n"
				+ "    path = '/' + d\n"
				+ "    try:\n"
				+ "        s = os.statvfs(path)\n"
				+ "        total = s[0]*s[2]\n"
				+ "        free = s[0]*s[3]\n"
				+ "        used = total - free\n"
				+ "        percent = 0 if total == 0 else int(used*100/total)\n"
				+ "        mounts.append('%s\t%d\t%d\t%d\t%d' % (d, total, used, free, percent))\n"
				+ "    except:\n"
				+ "        pass\n"
				+ "print('\\n'.join(mounts))";
		String header = "mount\tsize\tused\tavail\tuse%";
		String result = sendCommand(py);
		return header + "\n" + result.trim();
	}

	/**
	 * Remove a file from the MicroPython device.
	 *
	 * @param path Path to the file to remove (e.g. "/flash/file.txt")
	 * @throws Exception on error
	 */
	public static void fsRm(String path) throws Exception {
		ensureConnected();
		String cmd = "import os; os.remove('" + path + "')";
		sendCommand(cmd);
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
				System.out.println("--->"+s);
				sb.append(s);
				// if sb contains 0D 0A 04 04 3E 
				if (sb.toString().contains("\r\n" + (char) 4 + (char) 4 + ">")) {
					break;
				}
			}
			if (System.currentTimeMillis() - start > 2000) {
				break; // Timeout
			}
		}
		String output = sb.toString();
		//replace last 0D 0A 04 04 3E to empty
		output = output.replace("\r\n" + (char) 4 + (char) 4 + ">", "");
		return output;
	}

	private static String readUntilPromptOK() throws Exception {
		StringBuilder sb = new StringBuilder();
		byte[] buffer = new byte[1024];
		int len;
		long start = System.currentTimeMillis();
		while (true) {
			if (in.available() > 0 && (len = in.read(buffer)) > 0) {
				String s = new String(buffer, 0, len);
//				System.out.println("--->"+s);
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
				if (sb.toString().contains("OK")) {
					break;
				}
			}
			if (System.currentTimeMillis() - start > 2000) {
				break; // Timeout
			}
		}
		String output = sb.toString();
		//replace last 0D 0A 04 04 3E to empty
		output = output.replace("OK", "");
		return output;
	}

	// Add more methods for cp, etc. using serial communication
}
