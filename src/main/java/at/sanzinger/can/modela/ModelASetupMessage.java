package at.sanzinger.can.modela;

import static at.sanzinger.can.modela.ModelASetupMessage.ModelASetupMessageType.CAN_BPS;
import static at.sanzinger.can.modela.ModelASetupMessage.ModelASetupMessageType.CAN_FILTER_ID;
import static at.sanzinger.can.modela.ModelASetupMessage.ModelASetupMessageType.CAN_FRAME_MODE;
import static at.sanzinger.can.modela.ModelASetupMessage.ModelASetupMessageType.CAN_MASK_ID;
import static at.sanzinger.can.modela.ModelASetupMessage.ModelASetupMessageType.CAN_MODE;
import static at.sanzinger.can.modela.ModelASetupMessage.ModelASetupMessageType.CAN_NOT_ONCE;
import static at.sanzinger.can.modela.ValueEnum.getEnum;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import at.sanzinger.can.message.ByteSpec;
import at.sanzinger.can.message.ConstantByteSpec;
import at.sanzinger.can.message.EnumByteSpec;
import at.sanzinger.can.message.VariableByteSpec;

public class ModelASetupMessage extends ModelAMessage<ModelASetupMessage> {
	
	public ModelASetupMessage(ByteBuffer payload) {
		super(ModelASetupMessageType.TYPE, payload);
	}
	
	public ModelASetupMessage() {
		this(ByteBuffer.allocate(21));
	}
	
	public void setBps(ModelACANBps bps) {
		CAN_BPS.setValue(payload, bps);
	}
	
	public ModelACANBps getBps() {
		return getEnum(CAN_BPS, payload);
	}
	
	public void setFrameMode(ModelAFrameMode mode) {
		CAN_FRAME_MODE.setValue(payload, mode);
	}
	
	public ModelAFrameMode getFrameMode() {
		return getEnum(CAN_FRAME_MODE, payload);
	}
	
	public void setMask(int mask) {
		CAN_MASK_ID.setValue(payload, mask);
	}
	
	public int getMask() {
		return (int)CAN_MASK_ID.getValue(payload);
	}
	
	public void setFilterId(int filter) {
		CAN_FILTER_ID.setValue(payload, filter);
	}
	
	public int getFilterId() {
		return (int)CAN_FILTER_ID.getValue(payload);
	}
	
	public void setMode(ModelAMode mode) {
		CAN_MODE.setValue(payload, mode);
	}
	
	public ModelAMode getMode() {
		return getEnum(CAN_MODE, payload);
	}
	
	public void setSendMode(ModelASendMode mode) {
		CAN_NOT_ONCE.setValue(payload, mode);
	}
	
	public ModelASendMode getSendMode() {
		return getEnum(CAN_NOT_ONCE, payload);
	}
	
	public enum ModelASendMode implements ValueEnum {
		//00: Only send once 01: NOT Only send once
		ONLY_SEND_ONCE(00),
		NOT_ONLY_SEND_ONCE(01);
		private byte value;
		
		private ModelASendMode(int value) {
			this.value = (byte)value;
		}

		@Override
		public long getValue() {
			return value;
		}
	}
	
	public enum ModelAFrameMode implements ValueEnum {
		//00: normal 01: loopback 02: silent 03: loop+silent
		NORMAL(0),
		LOOPBACK(2),
		LOOP_SILENT(3);
		
		private byte value;
		
		
		private ModelAFrameMode(int value) {
			this.value = (byte)value;
		}

		@Override
		public long getValue() {
			return value;
		}
	}
	
	public enum ModelAMode implements ValueEnum {
		EXTENDED_FRAME(0x1),
		NORMAL_FRAME(0x2);
		
		public byte value;
		private ModelAMode(int value) {
			this.value = (byte)value;
		}
		
		@Override
		public long getValue() {
			return value;
		}
	}
	
	public enum ModelACANBps implements ValueEnum {
		BPS5k(0xc),
		BPS10k(0xb),
		BPS20k(0xa),
		BPS50k(0x9),
		BPS100k(0x8),
		BPS125k(0x7),
		BPS200k(0x6),
		BPS250k(0x5),
		BPS400k(0x4),
		BPS500k(0x3),
		BPS800k(0x2),
		BPS1000k(0x1);
		
		public byte value;
		
		private ModelACANBps(int number) {
			this.value = (byte)number;
		}
		
		@Override
		public long getValue() {
			return value;
		}
	}

	/**
	 * Setup message:
	 * <pre>
	 *        ,--------------------------------------- Control type (alwasys 0x02 for this message)
	 *        |  ,------------------------------------ can bps {@link #ModelASetupMessage.ModelACANBps}
	 *        |  |  ,--------------------------------- 01: extended 02: normal fame
	 *        |  |  | ,---------,--------------------- Filter ID (Little Endian)
	 *        |  |  | |         | ,---------,--------- Mask ID (Little Endian)
	 *        |  |  | |         | |         |  ,------ Mode: 00: normal 01: loopback 02: silent 03: loop+silent
	 *        |  |  | |         | |         |  |  ,--- 00: Only send once 01: NOT Only send once
	 * AA 55 02 03 02 BE BA FE CA EF BE AD DE 00 01 00 00 00 00 80 	
	 * </pre>
	 */
	public static class ModelASetupMessageType extends ModelAMessageType<ModelASetupMessage> {
		
		public static final ConstantByteSpec MESSAGE_TYPE;
		public static final EnumByteSpec<ModelACANBps> CAN_BPS;
		public static final EnumByteSpec<ModelAMode> CAN_MODE;
		public static final VariableByteSpec CAN_FILTER_ID;
		public static final VariableByteSpec CAN_MASK_ID;
		public static final EnumByteSpec<ModelAFrameMode> CAN_FRAME_MODE;
		public static final EnumByteSpec<ModelASendMode> CAN_NOT_ONCE;
		public static final VariableByteSpec UNKNOWN1;
		public static final VariableByteSpec CHECKSUM;
		
		public static final ByteSpec[] SPEC = extend(
			ModelAMessageType.SPEC, 
			MESSAGE_TYPE   = new ConstantByteSpec(ModelAMessageType.MESSAGE_TYPE, 2),
			CAN_BPS        = new EnumByteSpec<>  ("CAN_BPS",        MESSAGE_TYPE,   c(1), ModelACANBps.values()),
			CAN_MODE       = new EnumByteSpec<>  ("CAN_FRAME",      CAN_BPS,        c(1), ModelAMode.values()),
			CAN_FILTER_ID  = new VariableByteSpec("CAN_FILTER_ID",  CAN_MODE,       c(4)),
			CAN_MASK_ID    = new VariableByteSpec("CAN_MASK_ID",    CAN_FILTER_ID,  c(4)),
			CAN_FRAME_MODE = new EnumByteSpec<>  ("CAN_FRAME_MODE", CAN_MASK_ID,    c(1), ModelAFrameMode.values()),
			CAN_NOT_ONCE   = new EnumByteSpec<>  ("CAN_NOT_ONCE",   CAN_FRAME_MODE, c(1), ModelASendMode.values()),
			UNKNOWN1       = new VariableByteSpec("UNKNOWN1",       CAN_NOT_ONCE,   c(4)),
			CHECKSUM       = new VariableByteSpec("CHECKSUM",       UNKNOWN1,       c(1))
		);
		
		public static final ModelASetupMessageType TYPE = new ModelASetupMessageType();
		
		private ModelASetupMessageType() {
			super();
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
		public ModelASetupMessage readMessage(InputStream is) throws IOException {
			ByteBuffer buff = ByteBuffer.allocate(128);
			setConstant(buff);
			buff.position(CAN_BPS.getBegin(buff));
			read(buff, is, CAN_BPS);
			buff.position(0);
			return new ModelASetupMessage(buff);
		}
	}
}
