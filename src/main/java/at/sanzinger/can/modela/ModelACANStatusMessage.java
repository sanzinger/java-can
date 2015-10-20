package at.sanzinger.can.modela;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import at.sanzinger.can.message.ByteSpec;
import at.sanzinger.can.message.ConstantByteSpec;
import at.sanzinger.can.message.VariableByteSpec;

public class ModelACANStatusMessage extends ModelAMessage<ModelACANStatusMessage> {
	public ModelACANStatusMessage(ByteBuffer payload) {
		super(ModelACANStatusMessageType.TYPE, payload);
	}
	
	public ModelACANStatusMessage() {
		this(ByteBuffer.allocate(21));
	}
	
	/**
	 * Just to be used for pinging the CAN adapter
	 *          ,---------------------------------------------,--- Unknown
	 * AA 55 04 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 04 	
	 */
	public static class ModelACANStatusMessageType extends ModelAMessageType<ModelACANStatusMessage> {

		public static final ConstantByteSpec MESSAGE_TYPE;
		public static final VariableByteSpec UNKNOWN1;
		public static final VariableByteSpec CHECKSUM;
		
		public static final ByteSpec[] SPEC = extend(
			ModelAMessageType.SPEC, 
			MESSAGE_TYPE   = new ConstantByteSpec(ModelAMessageType.MESSAGE_TYPE, 0x4),
			UNKNOWN1       = new VariableByteSpec("UNKNOWN1",       MESSAGE_TYPE, c(16)),
			CHECKSUM       = new VariableByteSpec("CHECKSUM",       UNKNOWN1,     c(1))
		);

		public static final ModelACANStatusMessageType TYPE = new ModelACANStatusMessageType();
		
		private ModelACANStatusMessageType() {
		}
		
		@Override
		public VariableByteSpec getChecksum() {
			return CHECKSUM;
		}

		@Override
		public ByteSpec[] getByteSpec() {
			return SPEC;
		}

		@Override
		public ModelACANStatusMessage readMessage(InputStream is) throws IOException {
			ByteBuffer buff = ByteBuffer.allocate(128);
			setConstant(buff);
			buff.position(UNKNOWN1.getBegin(buff));
			read(buff, is, UNKNOWN1);
			buff.position(0);
			return new ModelACANStatusMessage(buff);
		}
	}
}
