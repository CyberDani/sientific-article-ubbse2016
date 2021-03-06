package frontend.thread;

import edu.uci.ics.crawler4j.main.Controller;

public class CrawlerThread implements Runnable {
	private volatile Thread control;
	private volatile boolean suspend;
	private Controller controller;

	public CrawlerThread(Controller controller) {
		this.controller = controller;
	}

	public void startCrawler() {
		this.start();
	}

	public void run() {
		Thread currentThread = Thread.currentThread();
		while (currentThread == this.control) {
			try {
				if (this.suspend){
					synchronized(this) {
						while(this.suspend) {
							wait();
						}
					}
				}
				
				try {
					while (controller == null) {
						Thread.sleep(2000);
					}
					controller.runCrawler();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (InterruptedException ex) {
				break;
			}
		}
	}

	
	//////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Start a new thread  ////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////
	public void start() {
		this.control = new Thread(this);
		this.suspend = false;
		this.control.start();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Stop an existed thread  ////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	public void stop() {
		if ( control != null) {
			Thread acc = this.control;
			this.control = null;
			acc.interrupt();
		}
		controller.stopCrawler();
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Suspend an existed thread  ////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	public void suspend() {
		this.suspend = true;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Resume an existed thread  ////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	public void resume() {
		synchronized (this) {
			this.suspend = false;
			notifyAll(); // mindenki "felebresztese"
		}
	}
}
