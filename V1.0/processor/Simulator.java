package processor;

import java.io.IOException;
import java.net.Socket;

import common.Settings;
import processor.server.Server;
import processor.worker.Worker;

/**
 * Class for running a simulator with a single JVM.
 *
 */
public class Simulator {

	public static void createWorkers() {
		for (int i = 0; i < Settings.numWorkers; i++) {
			final Worker worker = new Worker();
			worker.run();
		}
	}

	static boolean isPortFree(final int port) {
		try (Socket ignored = new Socket("127.0.0.1", port)) {
			return false;
		} catch (final IOException ignored) {
			return true;
		}
	}

	/**
	 * Starts the simulator.
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		Settings.isSharedJVM = true;
		Settings.isVisualize = true;
		if (isPortFree(Settings.serverListeningPortForWorkers)) {
			startSystem();
		} else {
			System.exit(0);
		}
	}

	static void startSystem() {

		new Server().run();
		createWorkers();

	}
}