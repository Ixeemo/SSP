package pl.edu.agh.kt;

public class Timeout {
	private short hardTimeout = 0;
	private short idleTimeout = 0;
	
	public Timeout() {
		this.idleTimeout = Flows.getIdleTimeout();
		this.hardTimeout = Flows.getHardTimeout();
	}
	public Timeout(short idle, short hard){
		this.idleTimeout = idle;
		this.hardTimeout = hard;
		Flows.setIdleTimeout(idle);
		Flows.setHardTimeout(hard);
	}
	public short getHardTimeout() {
		return hardTimeout;
	}
	public void setHardTimeout(short hardTimeout) {
		this.hardTimeout = hardTimeout;
	}
	public short getIdleTimeout() {
		return idleTimeout;
	}
	public void setIdleTimeout(short idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
}
