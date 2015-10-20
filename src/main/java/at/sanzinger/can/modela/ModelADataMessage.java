package at.sanzinger.can.modela;

import static at.sanzinger.can.modela.ModelADataMessage.ModelADataMessageType.FRAME_TYPE;
import static at.sanzinger.can.modela.ModelADataMessage.ModelADataMessageType.MSG_FORMAT;
import static at.sanzinger.can.modela.ModelADataMessage.ModelADataMessageType.MSG_ID;
import static at.sanzinger.can.modela.ValueEnum.getEnum;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import at.sanzinger.can.DataMessage;
import at.sanzinger.can.message.ByteSpec;
import at.sanzinger.can.message.ConstantByteSpec;
import at.sanzinger.can.message.EnumByteSpec;
import at.sanzinger.can.message.VariableByteSpec;
import at.sanzinger.can.modela.ModelASetupMessage.ModelAMode;

public class ModelADataMessage extends ModelAMessage<ModelADataMessage> {
	
	public ModelADataMessage() {
		this(ByteBuffer.allocate(20));
		setFrameType(ModelAMode.NORMAL_FRAME);
		setMessageFormat(ModelAMessageFormat.DATA_FRAME);
		payload.position(0);
	}
	
	public ModelADataMessage(ByteBuffer buffer) {
		super(ModelADataMessageType.TYPE, buffer);
	}
	
	public byte[] getData() {
		return ModelADataMessageType.MSG.getBytes(payload);
	}
	
	public void setData(byte[] bytes) {
		ModelADataMessageType.MSG_LEN.setValue(payload, bytes.length);
		ModelADataMessageType.MSG.setBytes(payload, bytes);
	}
	
	
	public void setFrameType(ModelAMode mode) {
		FRAME_TYPE.setValue(payload, mode);
	}
	
	public ModelAMode getFrameType() {
		return getEnum(FRAME_TYPE, payload);
	}
	
	public int getId() {
		return (int)MSG_ID.getValue(payload);
	}
	
	public void setId(int id) {
		MSG_ID.setValue(payload, id);
	}
	
	public ModelAMessageFormat getMessageFormat() {
		return getEnum(MSG_FORMAT, payload);
	}
	
	public void setMessageFormat(ModelAMessageFormat fmt) {
		MSG_FORMAT.setValue(payload, fmt);
	}
	
	public DataMessage getDataMessage() {
		return new DataMessage(getData(), getId());
	}
	
	public enum ModelAMessageFormat implements ValueEnum {
		DATA_FRAME(01),
		REMOTE_FRAME(02);
		private int value;

		private ModelAMessageFormat(int value) {
			this.value = value;
		}
		
		@Override
		public long getValue() {
			return value;
		}
	}

	/**
	 * Data message:
	 * <pre>
	 *            ,---,------------------------------------------------------ Start sequence
	 *            |   |  ,--------------------------------------------------- Type
	 *            |   |  | ,------------------------------------------------- Frame Type (1=Extended Frame, 2=Standard Frame)
	 *            |   |  | |  ,---------------------------------------------- Message Format (1=Dataframe, 2=Remote Frame)
	 *            |   |  | |  |  ,---------,--------------------------------- Message ID
	 *            |   |  | |  |  |         | ,------------------------------- Message length in bytes
	 *            |   |  | |  |  |         | |  ,--------------------,------- Message
	 *            |   |  | |  |  |         | |  |                    |  ,---- Unknown  
	 *            |   |  | |  |  |         | |  |                    |  |  ,- Checksum (Probably amount of ones)
	 * Length 20: AA 55 01 02 01 BE BA FE CA 08 00 01 02 03 04 05 06 07 00 68 
	 * </pre>
	 */
	public static class ModelADataMessageType extends ModelAMessageType<ModelADataMessage> {

		public static final ConstantByteSpec MESSAGE_TYPE;
		public static final EnumByteSpec<ModelAMode> FRAME_TYPE;
		public static final EnumByteSpec<ModelAMessageFormat> MSG_FORMAT;
		public static final VariableByteSpec MSG_ID;
		public static final VariableByteSpec MSG_LEN;
		public static final VariableByteSpec MSG;
		public static final VariableByteSpec UNKNOWN1;
		public static final VariableByteSpec CHECKSUM;
		
		public static final ByteSpec[] SPEC = extend(
			ModelAMessageType.SPEC, 
			MESSAGE_TYPE  = new ConstantByteSpec(ModelAMessageType.MESSAGE_TYPE, 1),
			FRAME_TYPE    = new EnumByteSpec<>(  "FRAME_TYPE",  MESSAGE_TYPE, c(1), ModelAMode.values()),
			MSG_FORMAT    = new EnumByteSpec<>(  "MSG_FORMAT",  FRAME_TYPE,   c(1), ModelAMessageFormat.values()),
			MSG_ID        = new VariableByteSpec("MSG_ID",      MSG_FORMAT,   c(4)),
			MSG_LEN       = new VariableByteSpec("MSG_LEN",     MSG_ID,       c(1)),
			MSG           = new VariableByteSpec("MSG",         MSG_LEN,      c(8)),
			UNKNOWN1      = new VariableByteSpec("UNKNOWN1",    MSG,          c(1)),
			CHECKSUM      = new VariableByteSpec("CHECKSUM",    UNKNOWN1,     c(1))
		);
		
		public static final ModelADataMessageType TYPE = new ModelADataMessageType();
		
		public ModelADataMessageType() {
			super();
		}
		
		public VariableByteSpec getChecksum() {
			return CHECKSUM;
		}
		
		@Override
		public ByteSpec[] getByteSpec() {
			return SPEC;
		}
		
		@Override
		public ModelADataMessage readMessage(InputStream is) throws IOException {
			ByteBuffer buff = ByteBuffer.allocate(256);
			setConstant(buff);
			buff.position(FRAME_TYPE.getBegin(buff));
			read(buff, is, FRAME_TYPE);
			buff.position(0);
			return new ModelADataMessage(buff);
		}
	}

}
