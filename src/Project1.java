import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.DoubleBinaryOperator;

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
		n = temp.length;

		String file_output = "";
		file_output += fcfs_simulation(temp);

		try {
			printToFile(file_output, new File(args[1]));
		} catch (Exception e) {
			e.printStackTrace();
		}

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

	public static String srt_simulation(Process[] processes) {

		int t = 0;
		Queue<Process> q = new LinkedList<>();
		while (running(processes)) {

			// NOTE: once processes are finished, burst_amt--, io_time_current
			// starts to decrement
			for (Process p : processes) { // FOR EACH PROCESS

				if (p.state == State.TERMINATED) { // CHECK TO SEE IF TERMINATED
					q.remove(p); // REMOVE FROM QUE
					continue;
				}
				if (q.contains(p)) { // IF IN QUE
					continue; // DO NOTHING
				}
				// Not in Q
				if (p.initial_arrival_time == t) { // IF WE JUST ARRIVED
					p.state = State.READY; // PROGRAM IS READY, NOT BLOCKED
					// NEED TO ADD IN ORDER OF REMAINING TIME

					q.add(p); // JUST FOR ARRIVAL

					// NEED TO CHECK HERE TO SEE IF WE NEED TO PREEMPT
				}

				if (p.io_time_current == 0 && p.state != State.RUNNING) { // IF
																			// THE
																			// PROGRAM
																			// IS
																			// READY
																			// TO
																			// RUN
					p.io_time_current = p.io_time; // resetting io_time
					if (p.number_bursts == 0) { // IF WE HAVE NO BURSTS LEFT
						p.state = State.TERMINATED; // IF IO was the last thing
													// to do... terminate
					} else {
						q.add(p); // WE HAVE BURSTS TO DO
						p.state = State.READY; // READY THE PROCESS LESGOO
					}
				}
			}
			// END FOR EACH PROCESS

			// NOW FOR ACTUALLY RUNNING THE PROCESS
			t++;
			int running_index = running_index(processes);

			// HERE NEED TO INCREMENT EVERYTHING THAT ISN'T RUNNING

			for (Process ps : processes) {
				if (ps.state == State.BLOCKED) {
					ps.turnaround_time++;
					if (ps.io_time_current > 0)
						ps.io_time_current--;
					// ps.wait_time++;
				}
				if (ps.state == State.READY) {
					ps.turnaround_time++;
					ps.wait_time++;
				}
			}

			// DONE WITH INCREMENTATION

			if (running_index == -1) { // START A NEW PROCESS IF THERE IS NONE
										// RUNNING
				if (q.isEmpty()) {
					continue; // CHECK TO SEE IF Q HAS ANYTHING FOR US
				}
				Process run = q.remove(); // START THAT BADBOY UP

				run.state = State.RUNNING; // Now its running
				run.burst_current--; //
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
					// Todo need to fiure out remaining burst time
					System.out.println("Finished with " + run.process_id + " With remaining time "
							+ run.number_bursts * run.cpu_burst_time + " at time " + t);
					run.number_bursts--;
					run.burst_current = run.cpu_burst_time;
					run.state = State.BLOCKED;
					run.io_time_current = run.io_time;

					// NOTE: THIS ONLY WOULD APPLY IF EVERY TIME A PROCESS
					// ***ENDS*** THERE IS A CONTEXT SWITCH
					run.context_switches++;

					t += t_cs;
					for (Process ps : processes) {
						assert (ps.state != State.RUNNING);
						if (ps.state == State.BLOCKED || ps.state == State.READY) {
							ps.wait_time += t_cs;
							ps.turnaround_time += t_cs;
						}
					}
					// ADD CONTEXT SWITCH TIME

					q.remove(run);
				}
			}

		}

		int cpubursttime = 0;
		int total_cpu_bursts = 0;
		int waittime = 0;
		int turnaroundtime = 0;
		int contextswitches = 0;
		int preemptions = 0;

		for (Process p : processes) {
			System.out.println("Process: " + p.process_id);
			System.out.println("\tTurnaround: " + p.turnaround_time);
			System.out.println("\tWait time: " + p.wait_time);
			System.out.println("\tCPU Burst time: " + p.cpu_burst_time_actual);

			total_cpu_bursts += p.number_bursts_CONSTANT;
			cpubursttime += p.cpu_burst_time_actual;
			waittime += p.wait_time;
			turnaroundtime += p.turnaround_time;
			contextswitches += p.context_switches;
			preemptions += p.preemptions;

		}
		String ret = "Algorithm SRT\n";
		ret += String.format("-- average CPU burst time: %.2f ms\n", (double) cpubursttime / (double) total_cpu_bursts);
		ret += String.format("-- average wait time: %.2f ms\n", (double) waittime / (double) total_cpu_bursts);
		ret += String.format("-- average turnaround time: %.2f ms\n",
				(double) turnaroundtime / (double) total_cpu_bursts);
		ret += String.format("-- total number of context switches: %d\n", contextswitches);
		ret += String.format("-- total number of preemptions: %d\n", preemptions);
		return ret;
	}

	private static String queueToString(Queue<Process> queue) {
		Process[] xProcesses = new Process[queue.size()];
		xProcesses = queue.toArray(xProcesses);

		if (queue.isEmpty())
			return "[Q <empty>]";

		String ret = "[Q";

		for (Process ps : xProcesses) {
			ret += " " + ps.process_id;
		}

		return ret + "]";
	}

	/**
	 * The FCFS algorithm is a non-preemptive algorithm in which processes line
	 * up in the ready queue, waiting to use the CPU. This is your baseline
	 * algorithm.
	 * 
	 * @param processes
	 */
	public static String fcfs_simulation(Process[] processes) {

		int t = 0;
		Queue<Process> q = new LinkedList<>();
		System.out.printf("time %dms: Simulator started for FCFS [Q <empty>]\n", t);
		boolean waiting = false, exit = false, waiting_next = false;
		int waiting_for = 0;
		ArrayList<Process> added = new ArrayList<>();
		int time = -1;
		boolean hasout = false;

		while (running(processes) && !exit) {

			waiting = waiting_next || waiting;
			boolean arrived = false;
			// NOTE: once processes are finished, burst_amt--, io_time_current
			// starts to decrement

			for (Process p : processes) {
				if (p.state != State.TERMINATED) {
					exit = false;
				}
				if (p.number_bursts > 0) {
					exit = false;
				}
				if (p.burst_current > 0 || p.burst_current == p.cpu_burst_time) {
					exit = false;
				}
			}

			if (exit) {
				t += t_cs / 2;
				exit = true;
				break;
			}

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
					System.out.printf("time %dms: Process %s arrived and added to ready queue %s\n", t, p.process_id,
							queueToString(q));
					arrived = true;
				} else if (t == p.io_time_next) { // FOR IO FINISH
					p.io_time_current = p.io_time; // resetting io_time
					if (p.number_bursts == 0) {
						p.state = State.TERMINATED; // IF IO was the last thing
													// to do... terminate
						if (!running(processes)) {
							t += 3;
							exit = true;
							break;
						}
					} else {
						q.add(p);
						added = new ArrayList<>();
						added.add(p);
						time = t;
						hasout = true;
						p.state = State.READY;
						if (q.size() == 1 && running_index(processes) == -1) {
							waiting_next = true;
							waiting_for += 4;
						}
					}
				}
			}

			if (exit)
				continue;

			for (Process ps : processes) {
				if (ps.state == State.BLOCKED) {
					ps.turnaround_time++;
					if (ps.io_time_current > -1)
						ps.io_time_current--;
				}
				if (ps.state == State.READY) {
					ps.turnaround_time++;
					ps.wait_time++;
				}
			}

			if (arrived == true) {
				if (running_index(processes) == -1) {
					waiting_next = true;
					waiting_for += 4;
				}
				continue;
			}

			int running_index = running_index(processes);

			if (waiting) {
				waiting_next = false;
				t++;
				waiting_for--;
				if (waiting_for == 0)
					waiting = false;
			} else if (running_index == -1 && !waiting_next) { // START A NEW
																// PROCESS
				if (q.isEmpty()) {
					t++;
					if(hasout) {
						for(Process p: added) {
							System.out.printf("time %dms: Process %s completed I/O; added to ready queue %s\n", time,
									p.process_id, queueToString(q));
						}
						hasout = false;
					}
					continue;
				}
				Process run = q.remove();

				System.out.printf("time %dms: Process %s started using the CPU %s\n", t, run.process_id,
						queueToString(q));

				run.state = State.RUNNING;
				run.burst_current--;
				run.cpu_burst_time_actual++;
				run.turnaround_time++;

				if (run.burst_current == 0) {
					run.state = State.BLOCKED;
					run.io_time_current = run.io_time;
				}
				t++;
			} else { // CHECK IF PROCESS IS NOW OVER - IF SO - START IO, burst--
				if (waiting_next && running_index == -1){
					if(hasout) {
						for(Process p: added) {
							System.out.printf("time %dms: Process %s completed I/O; added to ready queue %s\n", time,
									p.process_id, queueToString(q));
						}
						hasout = false;
					}
					continue;
				}
				Process run = processes[running_index];

				run.turnaround_time++;
				run.burst_current--;
				run.cpu_burst_time_actual++;

				if (run.burst_current == -1) {

					run.number_bursts--;
					if (run.number_bursts != 0) {

						if(q.containsAll(added) && !added.isEmpty() && time == t) {
							Queue<Process> copy = new LinkedList<>();
							copy.addAll(q);
							copy.removeAll(added);
							run.io_time_next = t + run.io_time + t_cs / 2;
							System.out.printf("time %dms: Process %s completed a CPU burst; %d burst%s to go %s\n", t,
									run.process_id, run.number_bursts, run.number_bursts > 1 ? "s" : "",
									queueToString(copy));
							System.out.printf(
									"time %dms: Process %s switching out of CPU; will block on I/O until time %dms %s\n",
									t, run.process_id, run.io_time_next, queueToString(copy));
							for(Process p: added) {
								System.out.printf("time %dms: Process %s completed I/O; added to ready queue %s\n", t,
										p.process_id, queueToString(q));
							}
							hasout = false;
						} else {

							run.io_time_next = t + run.io_time + t_cs / 2;
							System.out.printf("time %dms: Process %s completed a CPU burst; %d burst%s to go %s\n", t,
									run.process_id, run.number_bursts, run.number_bursts > 1 ? "s" : "",
									queueToString(q));
							System.out.printf(
									"time %dms: Process %s switching out of CPU; will block on I/O until time %dms %s\n",
									t, run.process_id, run.io_time_next, queueToString(q));
						}
					} else {
						System.out.printf("time %dms: Process %s terminated %s\n", t, run.process_id, queueToString(q));
						run.state = State.TERMINATED;
						run.io_time_current = 0;
					}
					run.burst_current = run.cpu_burst_time;
					if (run.state != State.TERMINATED)
						run.state = State.BLOCKED;

					// NOTE: THIS ONLY WOULD APPLY IF EVERY TIME A PROCESS
					// ***ENDS*** THERE IS A CONTEXT SWITCH
					run.context_switches++;

					waiting = true;
					waiting_for = t_cs - 1;

					// ADD CONTEXT SWITCH TIME

					q.remove(run);
				}
				
				if(hasout) {
					for(Process p: added) {
						System.out.printf("time %dms: Process %s completed I/O; added to ready queue %s\n", time,
								p.process_id, queueToString(q));
					}
					hasout = false;
				}
				
				
				t++;
				if (!running(processes))
					t += t_cs / 2 - 1;
			}
		}

		System.out.printf("time %dms: Simulator ended for FCFS\n\n", t);

		int cpubursttime = 0;
		int total_cpu_bursts = 0;
		int waittime = 0;
		int turnaroundtime = 0;
		int contextswitches = 0;
		int preemptions = 0;

		for (Process p : processes) {
			total_cpu_bursts += p.number_bursts_CONSTANT;
			cpubursttime += p.cpu_burst_time_actual;
			waittime += p.wait_time;
			turnaroundtime += p.turnaround_time;
			contextswitches += p.context_switches;
			preemptions += p.preemptions;

		}
		String ret = "Algorithm FCFS\n";
		ret += String.format("-- average CPU burst time: %.2f ms\n", (double) cpubursttime / (double) total_cpu_bursts);
		ret += String.format("-- average wait time: %.2f ms\n", (double) waittime / (double) total_cpu_bursts);
		ret += String.format("-- average turnaround time: %.2f ms\n",
				(double) turnaroundtime / (double) total_cpu_bursts);
		ret += String.format("-- total number of context switches: %d\n", contextswitches);
		ret += String.format("-- total number of preemptions: %d\n", preemptions);
		return ret;
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
		ret.number_bursts_CONSTANT = ret.number_bursts;
		ret.io_time = Integer.parseInt(split[4]);
		ret.burst_current = ret.cpu_burst_time;
		ret.io_time_current = ret.io_time;
		ret.io_time_next = -1;
		return ret;
	}

	public static class Process {
		String process_id;
		int initial_arrival_time;
		int cpu_burst_time;
		int number_bursts;

		int number_bursts_CONSTANT;

		int io_time;
		State state = State.NOT_STARTED;

		int io_time_current;
		int io_time_next;
		int burst_current;

		int turnaround_time = 0;
		int wait_time = 0;
		int cpu_burst_time_actual = 0;

		int preemptions = 0;
		int context_switches = 0;

		@Override
		public String toString() {
			// return "Process: " + process_id + "; Arrived " +
			// initial_arrival_time + " ms; " + number_bursts + ", "
			// + cpu_burst_time + " ms bursts; " + io_time + " IO_TIME";
			return String.format("|%s|||io_time_current: %d|burst_current: %d|state: %s|", process_id, io_time_current,
					burst_current, state);
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

	public static void printToFile(String all, File file) throws Exception {
		file.createNewFile();
		PrintWriter printWriter = new PrintWriter(file);
		printWriter.write(all);
		printWriter.close();
		System.out.println("File made");
	}

}
