package at.sanzinger.can.modela;

import static at.sanzinger.can.Log.error;
import static at.sanzinger.can.Log.info;
import static at.sanzinger.can.Log.warn;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import at.sanzinger.can.DataMessage;
import at.sanzinger.can.message.MessageType;
import at.sanzinger.can.modela.ModelACANStatusMessage.ModelACANStatusMessageType;
import at.sanzinger.can.modela.ModelADataMessage.ModelADataMessageType;
import at.sanzinger.can.modela.ModelAMessage.ModelAMessageType;
import at.sanzinger.can.modela.ModelASetupMessage.ModelACANBps;
import at.sanzinger.can.modela.ModelASetupMessage.ModelAFrameMode;
import at.sanzinger.can.modela.ModelASetupMessage.ModelAMode;
import at.sanzinger.can.modela.ModelASetupMessage.ModelASendMode;
import jssc.SerialPort;
import jssc.SerialPortException;

public class ModelAAdapter implements AutoCloseable {
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
	private volatile byte[] out;
	
	/**
	 * Messages coming from CAN adapter
	 */
	private volatile ModelAMessage<?> in;
	
	private Object outLock = new Object();
	private Object inLock = new Object();
	
	
	private InputStream inputStream = 
			new BufferedInputStream(
					new InputStream() {
		@Override
		public int read() throws IOException {
			int b;
			try {
				int available = port.getInputBufferBytesCount();
				if(available > 0) {
					b = 0xff & port.readBytes(1)[0];
				} else {
					b = -1;
				}
			} catch (SerialPortException e) {
				throw new IOException(e);
			}
			return b;
		}
		public int read(byte[] b, int off, int len) throws IOException {
			byte[] tmp;
			assert off == 0;
			try {
				int available = Math.min(len, port.getInputBufferBytesCount());
				if(available > 0) {
					tmp = port.readBytes(available);
					System.arraycopy(tmp, 0, b, 0, available);
				}
				return available;
			} catch (SerialPortException e) {
				throw new IOException(e);
			}
		}
		
		public int available() throws IOException {
			try {
				return port.getInputBufferBytesCount();
			} catch (SerialPortException e) {
				throw new IOException(e);
			}
		};
	}, 4096 * 2);
	private TransportRunnable transportRunnable;
	
	public ModelAAdapter(String device) {
		this.device = device;
		initializeDefaults();
	}
	
	public void setDataConsumer(Consumer<DataMessage> dataConsumer) {
		this.dataConsumer = dataConsumer;
	}
	
	private void initializeDefaults() {
		setup.setBps(ModelACANBps.BPS250k);
		setup.setFrameMode(ModelAFrameMode.NORMAL);
		setup.setMode(ModelAMode.NORMAL_FRAME);
		setup.setSendMode(ModelASendMode.NOT_ONLY_SEND_ONCE);
	}
	
	public void setBps(ModelACANBps bps) {
		setup.setBps(bps);
	}
	
	public ModelACANBps getBps() {
		return setup.getBps();
	}
	
	public boolean start() throws SerialPortException {
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
				close();
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
			return false;
		}
		silentSleep(1); // We need to wait after we have sent the configure message
		if(sendReceive(new ModelACANStatusMessage(), ModelACANStatusMessageType.TYPE) == null) {
			error("CAN adapter did not respond.");
			return false;
		}
		info("CAN adapter tested - ready for use");
		runlevel = 5;
		return true;
	}
	
	private static void silentSleep(long ms) {
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			warn("Sleep interrupted");
		}
	}
	
	public ModelACANStatusMessage getStatus() {
		return sendReceive(new ModelACANStatusMessage(), ModelACANStatusMessageType.TYPE);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends ModelAMessage<T>> T sendReceive(ModelAMessage<?> toSend, MessageType<T> expectedReturn) {
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
		return sendMessage(msg.getBody());
	}
	
	private synchronized boolean sendMessage(byte[] body) {
		synchronized (outLock) {
			assert out == null;
			out = body;
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
	
	@Override
	public void close() {
		if(runlevel == 0) {
			return;
		}
		runlevel = 0;
		try {
			synchronized(transmissionThread) {
				if(transmissionRunning) {
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
			runlevel = 0;
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
	
	private class TransportRunnable implements Runnable {
		
		
		@Override
		public void run() {
			synchronized(transmissionThread) {
				transmissionRunning = true;
				transmissionThread.notify();
			}
			try {
				while(runlevel > 0) {
					try {
						doReceiveMessage();
						doSendMessage();
						if(inputStream.available() == 0 && out == null) {
							Thread.sleep(0,500);
						}
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
				if(inputStream.available() > 0) {
					ReceiverState state = ReceiverState.HEAD0;;
					loop: do {
						int b = inputStream.read();
						ReceiverState nextState = null;
						switch(state) {
						case HEAD0:
							nextState = b == 0xAA ? ReceiverState.HEAD1 : ReceiverState.HEAD0;
							break;
						case HEAD1:
							nextState = b == 0x55 ? ReceiverState.MSG_TYPE : ReceiverState.HEAD0;
							break;
						case MSG_TYPE:
							try {
								processMessage(b); 
							} finally {						
								nextState = ReceiverState.HEAD0;
							}
							break loop;
						default:
							assert false : "Should not reach here";
						}
						assert nextState != null;
						state = nextState;
					} while(state != ReceiverState.HEAD0);
				}
			} catch(IOException e) {
				warn("Exception happened when reading bytes %s", e.getMessage());
			}
		}

		private void processMessage(int messageType) {
			ModelAMessageType<?> type = ModelAMessageType.getTypeById((byte)messageType);
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
					byte[] tmp = out;
					out = null;
					try {
						byte[] bytesOut = tmp;
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
