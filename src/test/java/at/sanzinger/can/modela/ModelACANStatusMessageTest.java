package at.sanzinger.can.modela;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import at.sanzinger.can.modela.ModelACANStatusMessage.ModelACANStatusMessageType;

public class ModelACANStatusMessageTest {
	private final byte[] testMsg = new byte[] { (byte) 0xAA, (byte) 0x55,
			(byte) 0x04, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4 };

	@Test
	public void testRead() throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(testMsg);
		assertEquals(0xaa, bis.read());
		assertEquals(0x55, bis.read());
		assertEquals(0x4, bis.read());
		ModelACANStatusMessage msg = ModelACANStatusMessageType.TYPE.readMessage(bis);
		assertEquals(4, msg.calculateChecksum());
	}

}
