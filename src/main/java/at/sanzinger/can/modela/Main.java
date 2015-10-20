package at.sanzinger.can.modela;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import at.sanzinger.can.modela.ModelADataMessage.ModelAMessageFormat;
import at.sanzinger.can.modela.ModelASetupMessage.ModelAMode;

public class Main {
	
	public static void main(String[] args) throws Exception {
		ModelAAdapter a = new ModelAAdapter(args[0]);
		a.setDataConsumer((m) -> {
			//a.send(m);
			System.out.println("Received data " + m);
		});
		a.start();
//		DataMessage msg = new DataMessage(new byte[]{0x12,0,0,0,0,0,0,0}, 0x0);
		//AA 55 02 05 02 00 00 00 00 00 00 00 00 00 01 00 00 00 00 0A 	

		//AA 55 01 02 01 00 00 00 00 08 00 01 02 03 04 05 06 07 00 28 	

		if(args.length >= 2 && args[1].equals("1")) {
			ModelADataMessage msg = new ModelADataMessage();
			msg.setFrameType(ModelAMode.EXTENDED_FRAME);
			msg.setMessageFormat(ModelAMessageFormat.DATA_FRAME);
			byte[] buff = new byte[8];
			ByteBuffer bbuff = ByteBuffer.wrap(buff);
			bbuff.order(ByteOrder.LITTLE_ENDIAN);
			int i = 0;
			while(true) {
				System.out.println(String.format("0x%x", i));
				bbuff.putInt(0, i++);
				msg.setData(buff);
				a.send(msg);
//				Thread.sleep(1);
			}
		}
	}
}
