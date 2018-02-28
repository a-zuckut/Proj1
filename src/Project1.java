import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class Project1 {

	public static void print(String arg) {
		System.out.println(arg);
	}

	public static int n; // TIMES TO SIMULATE
	public static int t_cs = 8; // TIME TO PERFORM CONTEXT SWITCH // DEFAULT 8

	// FOR RR Algorithm
	public static int t_slice = 80; // TIME SLICE
	public static boolean front_of_ready_queue = false; // PROCESSES TO BE ADDED
														// TO FRONT/END OF READY
														// QUEUE

	public static void main(String[] args) {

		if (args.length < 2) {
			print("ERROR: Invalid arguments\nUSAGE: ./a.out <input-file> <stats-output-file> [<rr-add>]\n");
		}

		String filename = args[0];
		Process[] xProcesses = parseProcesses(new File(filename));

		Process[] temp = xProcesses.clone();
		fcfs_simulation(temp);

	}

	/**
	 * The RR algorithm is essentially the FCFS algorithm with predefined time
	 * slice t_slice. Each process is given t_slice amount of time to complete
	 * its CPU burst. If this time slice expires, the process is preempted and
	 * added back (v1.2) to the end of the ready queue. If a process completes
	 * its CPU burst before a time slice expiration, the next process on the
	 * ready queue is immediately context-switched into the CPU. (v1.2) Note
	 * that arriving processes and processes that have completed I/O adhere to
	 * the rr_add parameter. For your simulation, if a preemption occurs and
	 * there are no other processes on the ready queue, do not perform a context
	 * switch. For example, if process A is using the CPU and the ready queue is
	 * empty, if process A is preempted by a time slice expiration, do not
	 * context-switch process A back to the empty queue. Instead, keep process A
	 * running with the CPU and do not count this as a context switch. In other
	 * words, when the time slice expires, check the queue to determine if a
	 * context switch should occur
	 * 
	 * @param processes
	 */
	public static void rr_simulation(Process[] processes) {

	}

	/**
	 * The SRT algorithm is a preemptive version of the Shortest Job First (SJF)
	 * algorithm. In both SJF and SRT, processes are stored in the ready queue
	 * in order of priority based on their CPU burst times. More specifically,
	 * the process with the shortest CPU burst time will be selected as the next
	 * process executed by the CPU. In SRT, when a process arrives, before it
	 * enters the ready queue, if it has a CPU burst time that is less than the
	 * remaining time of the currently running process, a preemption occurs.
	 * When such a preemption occurs, the currently running process is added
	 * back to the ready queue
	 * 
	 * @param processes
	 */
	public static void srt_simulation(Process[] processes) {

	}

	/**
	 * The FCFS algorithm is a non-preemptive algorithm in which processes line
	 * up in the ready queue, waiting to use the CPU. This is your baseline
	 * algorithm.
	 * 
	 * @param processes
	 */
	public static void fcfs_simulation(Process[] processes) {

		int t = 0;
		Queue<Process> q = new LinkedList<>();
		while (running(processes)) {

			// NOTE: once processes are finished, burst_amt--, io_time_current
			// starts to decrement
			for (Process p : processes) {

				if (p.state == State.TERMINATED) {
					q.remove(p);
					continue;
				}
				if (q.contains(p)) {
					continue;
				}

				if (p.initial_arrival_time == t) {
					p.state = State.READY;
					q.add(p); // JUST FOR ARRIVAL
				}

				if (p.io_time_current == 0 && p.state != State.RUNNING) {
					p.io_time_current = p.io_time; // resetting io_time
					if (p.number_bursts == 0) {
						p.state = State.TERMINATED; // IF IO was the last thing
													// to do... terminate
					} else {
						q.add(p);
						p.state = State.READY;
					}
				}
			}

			// NOW FOR ACTUALLY RUNNING THE PROCESS
			t++;
			int running_index = running_index(processes);
			// HERE NEED TO INCREMENT EVERYTHING THAT ISN'T RUNNING

			for (Process ps : processes) {
				if (ps.state == State.BLOCKED) {
					ps.turnaround_time++;
					if (ps.io_time_current > 0)
						ps.io_time_current--;
					ps.wait_time++;
				}
				if (ps.state == State.READY) {
					ps.turnaround_time++;
					ps.wait_time++;
				}
			}

			if (running_index == -1) { // START A NEW PROCESS
				if (q.isEmpty()) {
					continue;
				}
				Process run = q.remove();

				run.state = State.RUNNING;
				run.burst_current--;
				run.cpu_burst_time_actual++;
				run.turnaround_time++;

				if (run.burst_current == 0) {
					run.state = State.BLOCKED;
					run.io_time_current = run.io_time;

				}

			} else { // CHECK IF PROCESS IS NOW OVER - IF SO - START IO, burst--
				Process run = processes[running_index];

				run.turnaround_time++;
				run.burst_current--;
				run.cpu_burst_time_actual++;

				if (run.burst_current == 0) {
					System.out.println("Finished with " + run.process_id + " at time " + t);

					run.number_bursts--;
					run.burst_current = run.cpu_burst_time;
					run.state = State.BLOCKED;
					run.io_time_current = run.io_time;

					q.remove(run);
				}
			}

		}

		for (Process p : processes) {
			System.out.println("Process: " + p.process_id);
			System.out.println("\tTurnaround: " + p.turnaround_time);
			System.out.println("\tWait time: " + p.wait_time);
			System.out.println("\tCPU Burst time: " + p.cpu_burst_time_actual);
		}

	}

	private static int running_index(Process[] processes) {
		for (int i = 0; i < processes.length; i++) {
			if (processes[i].state == State.RUNNING)
				return i;
		}
		return -1;
	}

	private static boolean running(Process[] p) {
		for (Process ps : p) {
			if (ps.state != State.TERMINATED)
				return true;
		}
		return false;
	}

	public static Process[] parseProcesses(File file) {
		ArrayList<String> file_lines = parseFiles(file);
		ArrayList<Process> processes = new ArrayList<>();
		Process[] ret;

		for (String string : file_lines) {
			if (string.startsWith("#"))
				continue; // skip comments
			string = string.replace(" ", "");

			if (string.equals(""))
				continue; // skip whitespace lines

			try {
				processes.add(parseProcessLine(string));
			} catch (Exception e) {
				print("ERROR: Invalid input file format");
			}
		}

		ret = new Process[processes.size()];

		processes.toArray(ret);
		return ret;
	}

	private static Process parseProcessLine(String line) throws Exception {
		String[] split = line.split("\\|");

		Process ret = new Process();
		ret.process_id = split[0];
		ret.initial_arrival_time = Integer.parseInt(split[1]);
		ret.cpu_burst_time = Integer.parseInt(split[2]);
		ret.number_bursts = Integer.parseInt(split[3]);
		ret.io_time = Integer.parseInt(split[4]);
		ret.burst_current = ret.cpu_burst_time;
		ret.io_time_current = ret.io_time;
		return ret;
	}

	public static class Process {
		String process_id;
		int initial_arrival_time;
		int cpu_burst_time;
		int number_bursts;
		int io_time;
		State state = State.NOT_STARTED;

		int io_time_current;
		int burst_current;

		int turnaround_time = 0;
		int wait_time = 0;
		int cpu_burst_time_actual = 0;

		int preemptions = 0;
		int context_switches = 0;

		@Override
		public String toString() {
			return "Process: " + process_id + "; Arrived " + initial_arrival_time + " ms; " + number_bursts + ", "
					+ cpu_burst_time + " ms bursts; " + io_time + " IO_TIME";
		}
	}

	public enum State {
		READY, RUNNING, BLOCKED, TERMINATED, NOT_STARTED
	}

	/**
	 * Simple file parser
	 * 
	 * @param string
	 *            File name.
	 * @return The file in form of Strings for each line in the file
	 */
	public static ArrayList<String> parseFiles(File file) {
		ArrayList<String> ret = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String x = null;
			while ((x = br.readLine()) != null) {
				ret.add(x);
			}
			br.close();
		} catch (FileNotFoundException e) {
			print("ERROR: Invalid arguments\nUSAGE: ./a.out <input-file> <stats-output-file> [<rr-add>]\n");
			return null;
		} catch (IOException e) {
			print("ERROR: Invalid input file format");
			e.printStackTrace();
		}

		return ret;
	}

}
