package at.sanzinger.can.modela;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import at.sanzinger.can.modela.ModelASetupMessage.ModelACANBps;
import at.sanzinger.can.modela.ModelASetupMessage.ModelAFrameMode;
import at.sanzinger.can.modela.ModelASetupMessage.ModelAMode;
import at.sanzinger.can.modela.ModelASetupMessage.ModelASendMode;
import at.sanzinger.can.modela.ModelASetupMessage.ModelASetupMessageType;

public class ModelASetupMessageTest {
	byte[] testMsg = new byte[] {
			(byte)0xaa, 0x55, 0x02, 0x03, 0x02, (byte)0xBE, (byte)0xBA, (byte)0xFE, (byte)0xCA, (byte)0xEF, 
			(byte)0xBE, (byte)0xAD, (byte)0xDE, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, (byte)0x80
		};
	@Test
	public void testRead() throws Exception {
		ByteArrayInputStream bis = new ByteArrayInputStream(testMsg);
		assertEquals(0xaa, bis.read());
		assertEquals(0x55, bis.read());
		assertEquals(0x2, bis.read());
		ModelASetupMessage msg = ModelASetupMessageType.TYPE.readMessage(bis);
		assertEquals(ModelACANBps.BPS500k, msg.getBps());
		assertEquals(0xcafebabe, msg.getFilterId());
		assertEquals(0xdeadbeef, msg.getMask());
		assertEquals(ModelAFrameMode.NORMAL, msg.getFrameMode());
		assertEquals(ModelAMode.NORMAL_FRAME, msg.getMode());
		assertEquals(ModelASendMode.NOT_ONLY_SEND_ONCE, msg.getSendMode());
	}
	
	@Test
	public void testGenerate() throws Exception {
		ModelASetupMessage msg = new ModelASetupMessage();
		msg.setBps(ModelACANBps.BPS500k);
		msg.setFilterId(0xcafebabe);
		msg.setMask(0xdeadbeef);
		msg.setFrameMode(ModelAFrameMode.NORMAL);
		msg.setMode(ModelAMode.NORMAL_FRAME);
		msg.setSendMode(ModelASendMode.NOT_ONLY_SEND_ONCE);
		assertArrayEquals(testMsg, msg.getBody());
	}
}
