package at.sanzinger.can.modela;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import at.sanzinger.can.modela.ModelADataMessage.ModelADataMessageType;

public class ModelADataMessageTest {
	private final byte[] testMsg = new byte[] {
			(byte)0xAA, (byte)0x55, (byte)0x01, (byte)0x02, (byte)0x01, (byte)0xBE, (byte)0xBA, (byte)0xFE, (byte)0xCA, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x00, (byte)0x68};
	
	
			
	@Test
	public void testRead() throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(testMsg);
		assertEquals(0xaa, bis.read());
		assertEquals(0x55, bis.read());
		assertEquals(0x1, bis.read());
		ModelADataMessage msg = ModelADataMessageType.TYPE.readMessage(bis);
		assertArrayEquals(new byte[] {0x0, 1,2,3,4,5,6,7}, msg.getData());
	}

}
