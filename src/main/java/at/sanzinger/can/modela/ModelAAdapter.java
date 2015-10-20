package at.sanzinger.can.modela;

import static at.sanzinger.can.Log.error;
import static at.sanzinger.can.Log.info;
import static at.sanzinger.can.Log.warn;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import at.sanzinger.can.DataMessage;
import at.sanzinger.can.modela.ModelACANStatusMessage.ModelACANStatusMessageType;
import at.sanzinger.can.modela.ModelADataMessage.ModelADataMessageType;
import at.sanzinger.can.modela.ModelASetupMessage.ModelACANBps;
import at.sanzinger.can.modela.ModelASetupMessage.ModelAFrameMode;
import at.sanzinger.can.modela.ModelASetupMessage.ModelAMode;
import at.sanzinger.can.modela.ModelASetupMessage.ModelASendMode;

public class ModelAAdapter {
	private final int timeout = 1000;
	private final String device;
	private SerialPort port;
	private Thread transmissionThread;
	private final ModelASetupMessage setup = new ModelASetupMessage();
	private volatile boolean transmissionRunning = false;
	private volatile int runlevel = 0;
	private Consumer<DataMessage> dataConsumer;
	private boolean logBytes = false;
	
	/**
	 * Messages going to the CAN adapter
	 */
	private volatile ModelAMessage<?> out;
	
	/**
	 * Messages coming from CAN adapter
	 */
	private volatile ModelAMessage<?> in;
	
	private Object outLock = new Object();
	private Object inLock = new Object();
	private long lastTime;
	
	private static class SerialTimeoutIOException extends IOException {

		private static final long serialVersionUID = 1L;
		
	}
	
	private InputStream inputStream = new InputStream() {
		@Override
		public int read() throws IOException {
			byte b;
			try {
				b = port.readBytes(1, timeout)[0];
			} catch(SerialPortTimeoutException e) {
				throw new SerialTimeoutIOException();
			} catch (SerialPortException e) {
				throw new IOException(e);
			}
			return b;
		}
		
		public int read(byte[] b, int off, int len) throws IOException {
			byte[] tmp;
			try {
				tmp = port.readBytes(b.length, timeout);
				System.arraycopy(tmp, 0, b, 0, b.length);
				return tmp.length;
			} catch (SerialPortTimeoutException e) {
				throw new SerialTimeoutIOException();
			} catch (SerialPortException e) {
				throw new IOException(e);
			}
		};
		
	};
	private TransportRunnable transportRunnable;
	
	public ModelAAdapter(String device) {
		this.device = device;
	}
	
	public void setDataConsumer(Consumer<DataMessage> dataConsumer) {
		this.dataConsumer = dataConsumer;
	}
	
	private void initializeDefaults() {
		setup.setBps(ModelACANBps.BPS250k);
		setup.setFrameMode(ModelAFrameMode.NORMAL);
		setup.setMode(ModelAMode.NORMAL_FRAME);
		setup.setSendMode(ModelASendMode.ONLY_SEND_ONCE);
	}
	
	public void setBps(ModelACANBps bps) {
		setup.setBps(bps);
	}
	
	public ModelACANBps getBps() {
		return setup.getBps();
	}
	
	public boolean start() throws SerialPortException {
		initializeDefaults();
		port = new SerialPort(device);
		port.openPort();
		port.setParams(1228800, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
		runlevel = 1;
		
		// Start receiver thread
		this.transportRunnable = new TransportRunnable();
		transmissionThread = new Thread(transportRunnable);
		synchronized(transmissionThread) {
			transmissionThread.start();
			runlevel = 2;
			try {
				transmissionThread.wait(timeout);
			} catch(InterruptedException e) {
				error("Transmission thread did not start within %dms", timeout);
				stop();
				return false;
			}
		}
		runlevel = 3;
		info("Transmission thread started up");
		if(sendMessage(setup)) {
			info("CAN adapter configured");
			runlevel = 4;
		} else {
			error("CAN adapter could not be configured for unknown reason");
		}
		if(sendReceive(new ModelACANStatusMessage(), ModelACANStatusMessageType.TYPE) == null) {
			error("CAN adapter did not respond.");
		}
		info("CAN adapter tested - ready for use");
		runlevel = 5;
		return true;
	}
	
	public ModelACANStatusMessage getStatus() {
		return sendReceive(new ModelACANStatusMessage(), ModelACANStatusMessageType.TYPE);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends ModelAMessage<T>> T sendReceive(ModelAMessage<?> toSend, ModelAMessageType<T> expectedReturn) {
		synchronized(inLock) {
			sendMessage(new ModelACANStatusMessage());
			ModelAMessage<?> msg = receiveMessage();
			if(msg != null && msg.getType().equals(expectedReturn)) {
				return (T) msg;
			} else {
				error("CAN adapter did not respond as expected. Got status message: %s", msg);
				return null;
			}
		}
	}
	
	private ModelAMessage<?> receiveMessage() {
		synchronized(inLock) {
			if(in == null) {
				try {
					inLock.wait(timeout);
				} catch(InterruptedException e) {
					warn("Receive was interrupted");
				}
			}
			ModelAMessage<?> tmp = in;
			in = null;
			return tmp;
		}
	}
	
	private boolean sendMessage(ModelAMessage<?> msg) {
		synchronized (outLock) {
			assert out == null;
			out = msg;
			if(Thread.currentThread().equals(transmissionThread)) {
				// We're already in the transmission thread
				transportRunnable.doSendMessage();
			} else {
				try {
					outLock.wait(timeout);
				} catch(InterruptedException e) {
					warn("Send message wait was interrupted");
				}
			}
			return out == null;
		}
	}
	
	public void stop() {
		runlevel = 0;
		try {
			synchronized(transmissionThread) {
				if(!transmissionRunning) {
					try {
						transmissionThread.wait(timeout);
					} catch(InterruptedException e) {
						warn("Shutdown wait was interrupted");
					}
				}
			}
			if(transmissionRunning) {
				error("Transmission thread did not stop in a reasonable time, closing port forcefully.");
				transmissionThread.interrupt();
			}
		} finally {
			try {
				port.closePort();
			} catch(SerialPortException e) {
				warn("Exception happened when closing device %s: %s", device, e.getMessage());
			} finally {
				port = null;
			}
		}
	}
	
	public void send(ModelADataMessage msg) {
		if(!sendMessage(msg)) {
			System.out.println("Message was not sent.");
		}
	}
	
	private enum ReceiverState {
		HEAD0, // Receive 0xAA
		HEAD1, // Receive 0x55
		MSG_TYPE, // Receive message type byte
	}

	
	private byte[] readByte(int n, int timeout) throws SerialPortException, SerialPortTimeoutException {
		byte[] bytes = port.readBytes(n, timeout);
		if(logBytes) {
			if(System.currentTimeMillis() - lastTime > 300) {
				System.out.print("Incoming bytes: ");
			}
			for(byte b : bytes) {				
				System.out.print(String.format("0x%02x, ", b));
			}
		}
		lastTime = System.currentTimeMillis();
		return bytes;
	}
	
	private class TransportRunnable implements Runnable {
		ReceiverState state;
		
		@Override
		public void run() {
			synchronized(transmissionThread) {
				transmissionRunning = true;
				transmissionThread.notify();
			}
			
			state = ReceiverState.HEAD0;
			try {
				while(runlevel > 0) {
					try {
						doReceiveMessage();
						doSendMessage();
					} catch(Exception e) {
						error("Error happened during send/receive %s", e.getMessage());
					}
				}
			} finally {
				synchronized (transmissionThread) {
					transmissionRunning = false;
					transmissionThread.notify();
				}
			}
		}
		
		private void doReceiveMessage() {
			try {
				do {
					byte b = readByte(1, 20)[0];
					ReceiverState nextState = null;
					switch(state) {
					case HEAD0:
						nextState = b == (byte)0xAA ? ReceiverState.HEAD1 : ReceiverState.HEAD0;
						break;
					case HEAD1:
						nextState = b == (byte)0x55 ? ReceiverState.MSG_TYPE : ReceiverState.HEAD0;
						break;
					case MSG_TYPE:
						try {
							processMessage(b); 
						} finally {						
							nextState = ReceiverState.HEAD0;
						}
						break;
					default:
						assert false : "Should not reach here";
					}
					assert nextState != null;
					state = nextState;
				} while(state != ReceiverState.HEAD0);
			} catch(SerialPortTimeoutException e) {
				// ignore
			} catch(SerialPortException e) {
				warn("Exception happened when reading bytes %s", e.getMessage());
			}
		}

		private void processMessage(byte messageType) {
			ModelAMessageType<?> type = ModelAMessageType.getTypeById(messageType);
			try {
				ModelAMessage<?> msg = type.readMessage(inputStream);
				if(msg.verify()) {
					if(msg.getType().equals(ModelADataMessageType.TYPE)) {
						if(dataConsumer != null) {
							dataConsumer.accept(((ModelADataMessage)msg).getDataMessage());
						}
					} else {
						synchronized(inLock) {
							in = msg;
							inLock.notify();
						}
					}
				} else {
					error("Verification of incoming message %s failed. Expected checksum: 0x%x", msg, msg.calculateChecksum());
				}
			} catch(IOException e) {
				warn("Cannot read message of type %s got error %s", type, e.getMessage());
			}
		}

		protected void doSendMessage() {
			synchronized(outLock) {
				if(out != null) {
					ModelAMessage<?> tmp = out;
					out = null;
					try {
						byte[] bytesOut = tmp.getBody();
//						System.out.println(port.isCTS());
						port.writeBytes(bytesOut);
						if(logBytes) {
							try {
								System.out.print("Outgoing bytes: ");
								for(byte b : bytesOut) {
									System.out.print(String.format("0x%02x, ", b));
								}
								System.out.println();
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
						outLock.notify();
					} catch (SerialPortException e) {
						error("Cannot send message %s: %s", tmp, e.getMessage());
					}
				}
			}
		}
	}
}
