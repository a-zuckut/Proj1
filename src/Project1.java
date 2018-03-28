import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Queue;
import java.util.Vector;

public class Project1 {

	public static final Process NULL = null;

	public static void print(String arg) {
		System.out.println(arg);
	}

	public static int n; // TIMES TO SIMULATE
	public static int t_cs = 8; // TIME TO PERFORM CONTEXT SWITCH // DEFAULT 8

	// FOR RR Algorithm
	public static int t_slice = 80; // TIME SLICE
	public static boolean frontOfQueue = false; // PROCESSES TO BE ADDED
	// TO FRONT/END OF READY
	// QUEUE

	public static void main(String[] args) {

		if (args.length < 2) {
			print("ERROR: Invalid arguments\nUSAGE: ./a.out <input-file> <stats-output-file> [<rr-add>]\n");
		}

		String filename = args[0];
		Process[] xProcesses = parseProcesses(new File(filename));

		Process[] temp = xProcesses.clone();
		Process[] temp2 = parseProcesses(new File(filename));
		Process[] temp3 = parseProcesses(new File(filename));
		n = temp.length;

		if (args.length == 3) {
			String rr_add = args[2];
			if (rr_add.equals("BEGINNING")) {
				frontOfQueue = true;
			}
		}

		String file_output = "";
		file_output += fcfs(temp);
		System.out.println("");
		temp = xProcesses.clone();
		file_output += srt_simulation(temp2);
		System.out.println("");
		file_output += rr_simulation(temp3);

		n = temp2.length;
		// file_output += srt_simulation(temp2);

		try {
			printToFile(file_output, new File(args[1]));
		} catch (Exception e) {
			// error in file
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
	public static String rr_simulation(Process[] p) {
		int tBurstTime = 0;
		int tCS = 0;
		int tWait = 0;
		int tTurnaround = 0;
		int tPreemption = 0;
		boolean preempt = false;
		Vector<Process> initial_copy = new Vector<Process>();
		for (Process ps : p)
			initial_copy.addElement(ps);
		Vector<Process> process_in_io = new Vector<>();
		Vector<Process> queue = new Vector<>();

		Sim simulator = new Sim();

		int globalTime = -1; // start out with -1 -> go to 0 at first timing
		boolean new_process_into_IO = false;
		int tBursts = 0;

		// Initial calculations because yaknow
		for (int i = 0; i < p.length; i++) {
			tBursts += p[i].burstsLeft();
			tBurstTime += p[i].burstsLeft() * p[i].cpu_burst_time;
			tTurnaround += p[i].burstsLeft() * simulator.t_cs / 2;
			p[i].remainingTime = p[i].cpu_burst_time;
		}
		System.out.print("time 0ms: Simulator started for RR " + queueToString(queue));
		Vector<Process> added_turn = new Vector<>();
		Vector<String> added_prints = new Vector<>();
		Vector<Preemptee> preempts = new Vector<>();

		while (!queue.isEmpty() || !process_in_io.isEmpty() || !initial_copy.isEmpty() || !simulator.idle()) {
			added_turn = new Vector<>(); // resetting printing vectors
			added_prints = new Vector<>();
			globalTime++; // increase t
			for (int i = 0; i < queue.size(); i++) {
				queue.get(i).wait_time++;
			}
			if (!preempts.isEmpty()) {
				for (int i = 0; i < preempts.size(); i++) {
					// System.out.println("pre: " + preempts.get(i).p.process_id
					// + " remaining: " + preempts.get(i).timeRemaining);
					preempts.get(i).timeRemaining--;
					if (preempts.get(i).timeRemaining == 0) {
						queue.add(preempts.get(i).p);
						preempts.remove(preempts.get(i));
					}
				}
			}

			while (initial_copy.size() != 0) {
				if (globalTime == initial_copy.get(0).initial_arrival_time) {
					Process ps = initial_copy.get(0);
					added_turn.add(ps);
					initial_copy.remove(0);
					ps.ready = globalTime;
					added_prints.add("time " + globalTime + "ms: Process " + ps.process_id
							+ " arrived and added to ready queue ");
				} else
					break;
			}

			simulator.setCounter(globalTime);
			if (new_process_into_IO) {
				process_in_io.sort(new Comparator<Process>() {
					@Override
					public int compare(Process o1, Process o2) {
						return Integer.valueOf(o1.io_time_current).compareTo(Integer.valueOf(o2.io_time_current));
					}
				});
			}

			for (int i = 0; i < process_in_io.size(); i++) {
				process_in_io.get(i).io_time_current--;
			}

			while (!process_in_io.isEmpty()) {
				if (process_in_io.get(0).io_time_current == -4) {
					Process ps = process_in_io.get(0);
					added_turn.add(ps);
					added_prints.add("time " + globalTime + "ms: Process " + ps.process_id
							+ " completed I/O; added to ready queue ");
					ps.ready = globalTime;
					process_in_io.remove(0);
				} else
					break;
			}

			// if we context switch, we need to break out of the loop
			if (simulator.contextSwitch()) {
				printAll(added_turn, added_prints, queue);
				continue;
			}

			// CPU
			if (simulator.idle()) {
				printAll(added_turn, added_prints, queue);
				if (!queue.isEmpty()) {
					Process ps = queue.get(0);
					simulator.load(ps);
					tCS++;
					queue.remove(0);
					simulator.setIdle(false);
				}
			} else {
				// burst is either done, not done or has reached the time slice

				// If the Burst time reaches the time slice allocated, send it
				// back to queue
				// Bring in the next process
				// Also check that the process did not end at the same time as
				// the time slice
				if (simulator.getCurrentProcess().remainingTime - simulator.getCurrentProcess().burst_current == t_slice
						&& simulator.getCurrentProcess().burst_current > 0) {
					if (!queue.isEmpty()) {
						System.out.print("time " + globalTime + "ms: Time slice expired; process "
								+ simulator.getCurrentProcess().process_id + " preempted with "
								+ simulator.getCurrentProcess().burst_current + "ms to go " + queueToString(queue));
						preempts.add(new Preemptee(simulator.getCurrentProcess()));
						simulator.getCurrentProcess().remainingTime = simulator.getCurrentProcess().burst_current;
						simulator.setIdle(true);
						simulator.unload();
						tPreemption++;
						preempt = true;
						continue;
					} else {
						System.out.print("time " + globalTime
								+ "ms: Time slice expired; no preemption because ready queue is empty "
								+ queueToString(queue));
						simulator.getCurrentProcess().remainingTime = simulator.getCurrentProcess().burst_current;
						preempt = false;
					}

				}
				// Process still running, and hasn't been preempted by the time
				// slice
				if (simulator.getCurrentProcess().burst_current > 0) { // process
																		// still
					// Process just started running // running
					Process ps = simulator.getCurrentProcess();
					if (ps.burst_current == ps.cpu_burst_time) {
						System.out.print("time " + globalTime + "ms: Process " + ps.process_id
								+ " started using the CPU " + queueToString(queue));
					}
					// Process returning to running after having been preempted
					else if (ps.burst_current == ps.remainingTime && preempt) {
						System.out.print(
								"time " + globalTime + "ms: Process " + ps.process_id + " started using the CPU with "
										+ ps.remainingTime + "ms remaining " + queueToString(queue));
					}
					simulator.burst();
				}
				// Process Finishes Burst
				else { // process is done...
						// finishes burst
					Process ps = simulator.getCurrentProcess();
					ps.remainingTime = ps.cpu_burst_time;
					if (ps.burstsLeft() > 0) {
						System.out.print(
								"time " + globalTime + "ms: Process " + ps.process_id + " completed a CPU burst; "
										+ ps.burstsLeft() + " burst" + (ps.burstsLeft() > 1 ? "s" : "") + " to go ");
						tTurnaround += globalTime - ps.ready;
					} else {
						System.out.print("time " + globalTime + "ms: Process " + ps.process_id + " terminated ");
						tWait += ps.getWait();
						tTurnaround += globalTime - ps.ready;
					}
					System.out.print(queueToString(queue));
					ps.burst_current = ps.cpu_burst_time;
					ps.io_time_current = ps.io_time;
					simulator.setIdle(true);
					simulator.unload();
					if (ps.burstsLeft() > 0) {
						System.out.printf(
								"time %dms: Process %s switching out of CPU; will block on I/O until time %dms %s",
								globalTime, ps.process_id, globalTime + ps.io_time + simulator.t_cs / 2,
								queueToString(queue));
						queueToString(queue);
						process_in_io.add(ps);
						new_process_into_IO = true;
					}
				}
				printAll(added_turn, added_prints, queue);
			}

		}

		System.out.print("time " + (globalTime + 4) + "ms: Simulator ended for RR");

		double wait = (tWait) / (double) tBursts;
		double turnaround = tTurnaround / (double) tBursts;
		double burst = tBurstTime / (double) tBursts;

		String ret = "Algorithm RR\n";
		ret += String.format("-- average CPU burst time: %s ms\n", Project1.format(burst));
		ret += String.format("-- average wait time: %s ms\n", Project1.format(wait));
		ret += String.format("-- average turnaround time: %s ms\n", Project1.format(turnaround));
		ret += String.format("-- total number of context switches: %d\n", tCS);
		ret += String.format("-- total number of preemptions: %d\n", tPreemption);

		return ret;

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

	public static int getShortestProcessIndex(Process[] processes) {
		int max = 999999;
		int index = 0;
		int i = 0;
		for (Process ps : processes) {
			if ((ps.state == State.READY || ps.state == State.RUNNING) && ps.burst_current < max) {
				max = ps.burst_current;
				index = i;
				// System.out.printf("Max is %d \n", max );
			}
			i++;
		}
		return index;
	}

	public static void printProcess(Process[] processes) {
		for (Process ps : processes) {
			System.out.printf("Process: %s ProcessState is %s : ProcessTimeRemaining is %d \n", ps.process_id, ps.state,
					ps.burst_current);
		}
	}

	public static String srt_simulation(Process[] p) {
		int tBurstTime = 0;
		int tCS = 0;
		int tWait = 0;
		int tTurnaround = 0;
		int tPreemption = 0;
		Vector<Process> initial_copy = new Vector<Process>();
		for (Process ps : p)
			initial_copy.addElement(ps);
		Vector<Process> process_in_io = new Vector<>();
		Vector<Process> queue = new Vector<>();
		Vector<Process> csWaitQue = new Vector<>();
		Vector<Integer> csWaitTime = new Vector<>();
		boolean preempt = false;

		Sim simulator = new Sim();

		int global_counter = -1; // start out with -1 -> go to 0 at first timing
		boolean new_process_into_IO = false;
		int tBursts = 0;

		// Initial calculations because yaknow
		for (int i = 0; i < p.length; i++) {
			tBursts += p[i].burstsLeft();
			tBurstTime += p[i].burstsLeft() * p[i].cpu_burst_time;
			tTurnaround += p[i].burstsLeft() * simulator.t_cs / 2;
			p[i].remainingTime = p[i].cpu_burst_time;
		}

		System.out.print("time 0ms: Simulator started for SRT " + queueToString(queue));
		Vector<Process> added_turn = new Vector<>();
		Vector<String> added_prints = new Vector<>();
		Vector<Preemptee> preempts = new Vector<>();

		while (!queue.isEmpty() || !process_in_io.isEmpty() || !initial_copy.isEmpty() || !simulator.idle()) {
			added_turn = new Vector<>(); // resetting printing vectors
			added_prints = new Vector<>();
			global_counter++; // increase t

			for (int i = 0; i < queue.size(); i++) {
				queue.get(i).wait_time++;
			}

			for (int i = 0; i < preempts.size(); i++) {
				preempts.get(i).timeRemaining--;
				if (preempts.get(i).timeRemaining == 0) {
					queue.add(preempts.get(i).p);
					preempts.remove(preempts.get(i));
				}

			}

			while (initial_copy.size() != 0) {
				if (global_counter == initial_copy.get(0).initial_arrival_time) {
					Process ps = initial_copy.get(0);
					initial_copy.remove(0);
					ps.ready = global_counter;
					// If the current running process has more bursts to fin
					// then the other,
					if (simulator.getCurrentProcess() != null
							&& simulator.getCurrentProcess().burst_current > ps.burst_current
							&& simulator.getCurrentProcess().state == State.RUNNING) {
						Process old = simulator.getCurrentProcess();
						old.remainingTime = old.burst_current;
						simulator.loadNewProcessForSRT2(ps);
						preempts.add(new Preemptee(old));
						System.out.print("time " + global_counter + "ms: Process " + ps.process_id
								+ " arrived and will preempt " + old.process_id + " " + queueToString(queue));
						preempt = true;
						tCS++;
						tPreemption++;
					} else {
						added_turn.add(ps);
						added_prints.add("time " + global_counter + "ms: Process " + ps.process_id
								+ " arrived and added to ready queue ");
					}
				} else
					break;
			}

			simulator.setCounter(global_counter);
			if (new_process_into_IO) {
				process_in_io.sort(new Comparator<Process>() { // Sort ready que
					@Override
					public int compare(Process o1, Process o2) {
						return Integer.valueOf(o1.io_time_current).compareTo(Integer.valueOf(o2.io_time_current));
					}
				});
			}

			for (int i = 0; i < process_in_io.size(); i++) {
				process_in_io.get(i).io_time_current--;
			}

			while (!process_in_io.isEmpty()) {
				if (process_in_io.get(0).io_time_current == -4) {
					Process ps = process_in_io.get(0);
					if (simulator.idle()) {
						added_turn.add(ps);
						added_prints.add("time " + global_counter + "ms: Process " + ps.process_id
								+ " completed I/O; added to ready queue ");
						ps.ready = global_counter;
						process_in_io.remove(0);
					} else {
						// If the current running process has more bursts to fin
						// then the other,
						// Preform a context switch
						if (simulator.getCurrentProcess().burst_current > ps.burst_current) {
							Process old = simulator.getCurrentProcess();
							ps.ready = global_counter;
							old.remainingTime = old.burst_current;
							simulator.loadNewProcessForSRT(ps);
							preempts.add(new Preemptee(old));
							System.out.print("time " + global_counter + "ms: Process " + ps.process_id
									+ " completed I/O and will preempt " + old.process_id + " " + queueToString(queue));
							process_in_io.remove(0);
							preempt = true;
							tCS++;
							tPreemption++;

						} else if (simulator.getCurrentProcess().process_id != ps.process_id) {
							added_turn.add(ps);
							added_prints.add("time " + global_counter + "ms: Process " + ps.process_id
									+ " completed I/O; added to ready queue ");
							ps.ready = global_counter;
							process_in_io.remove(0);
							preempt = false;
						}
					}
				} else
					break;
			}
			// if we context switch, we need to break out of the loop
			if (simulator.contextSwitch()) {
				printAllSRT(added_turn, added_prints, queue);
				continue;
			}

			// CPU
			if (simulator.idle()) {
				queue.sort(new Comparator<Process>() { // Sort ready queue
					public int compare(Process o1, Process o2) {
						return Integer.valueOf(o1.burst_current).compareTo(Integer.valueOf(o2.burst_current));
					}
				});
				printAllSRT(added_turn, added_prints, queue);
				if (!queue.isEmpty()) {
					Process ps = queue.get(0);
					simulator.load(ps);
					tCS++;
					queue.remove(0);
					simulator.setIdle(false);
				}
			} else {
				// burst is either done or not done
				if (simulator.getCurrentProcess().burst_current > 0) { // process
																		// still
																		// running
					Process ps = simulator.getCurrentProcess();
					queue.sort(new Comparator<Process>() { // Sort ready que
						@Override
						public int compare(Process o1, Process o2) {
							return Integer.valueOf(o1.burst_current).compareTo(Integer.valueOf(o2.burst_current));
						}
					});
					if (ps.burst_current == ps.cpu_burst_time) {
						System.out.print("time " + global_counter + "ms: Process " + ps.process_id
								+ " started using the CPU " + queueToString(queue));
						simulator.last_process = ps;
					} else if (ps.burst_current == ps.remainingTime && simulator.last_process != ps) {
						System.out.print("time " + global_counter + "ms: Process " + ps.process_id
								+ " started using the CPU with " + ps.remainingTime + "ms remaining "
								+ queueToString(queue));
					}
					simulator.burst();
				} else { // process is done...
					// finishes burst
					Process ps = simulator.getCurrentProcess();
					if (ps.burstsLeft() > 0) {
						System.out.print(
								"time " + global_counter + "ms: Process " + ps.process_id + " completed a CPU burst; "
										+ ps.burstsLeft() + " burst" + (ps.burstsLeft() > 1 ? "s" : "") + " to go ");
						tTurnaround += global_counter - ps.ready;
					} else {
						System.out.print("time " + global_counter + "ms: Process " + ps.process_id + " terminated ");
						tWait += ps.getWait();
						tTurnaround += global_counter - ps.ready;
					}
					System.out.print(queueToString(queue));
					ps.burst_current = ps.cpu_burst_time;
					ps.io_time_current = ps.io_time;
					simulator.setIdle(true);
					simulator.unload();
					if (ps.burstsLeft() > 0) {
						System.out.printf(
								"time %dms: Process %s switching out of CPU; will block on I/O until time %dms %s",
								global_counter, ps.process_id, global_counter + ps.io_time + simulator.t_cs / 2,
								queueToString(queue));
						queueToString(queue);
						process_in_io.add(ps);
						new_process_into_IO = true;
					}
				}
				printAllSRT(added_turn, added_prints, queue);
			}

		}

		System.out.println("time " + (global_counter + 4) + "ms: Simulator ended for SRT");

		double wait = tWait / (double) tBursts;
		double turnaround = tTurnaround / (double) tBursts;
		double burst = tBurstTime / (double) tBursts;

		String ret = "Algorithm SRT\n";
		ret += String.format("-- average CPU burst time: %s ms\n", Project1.format(burst));
		ret += String.format("-- average wait time: %s ms\n", Project1.format(wait));
		ret += String.format("-- average turnaround time: %s ms\n", Project1.format(turnaround));
		ret += String.format("-- total number of context switches: %d\n", tCS);
		ret += String.format("-- total number of preemptions: %d\n", tPreemption);

		return ret;
	}

	public static void printAllSRT(Vector<Process> added_turn, Vector<String> added_prints,
			Vector<Process> ready_queue) {
		if (added_turn.size() == 0)
			return;
		int[] order = new int[added_turn.size()];
		String[] process = new String[added_turn.size()];
		for (int i = 0; i < added_turn.size(); i++) {
			process[i] = added_turn.elementAt(i).process_id;
		}
		String[] temp = process.clone();
		Arrays.sort(process);

		for (int i = 0; i < process.length; i++) {
			int j = 0;
			for (j = 0; j < temp.length; j++) {
				if (temp[i].equals(process[j])) {
					break;
				}
			}
			order[i] = j;
		}

		for (int i = 0; i < added_turn.size(); i++) {
			ready_queue.add(added_turn.get(order[i]));
			System.out.print(added_prints.get(order[i]) + queueToStringSRT(ready_queue));
		}

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
	/**
	 * public static String fcfs_simulation(Process[] processes) {
	 * 
	 * int t = 0; Queue<Process> q = new LinkedList<>(); System.out.printf("time
	 * %dms: Simulator started for FCFS [Q <empty>]\n", t); boolean waiting =
	 * false, exit = false, waiting_next = false; int waiting_for = 0;
	 * ArrayList<Process> add = new ArrayList<>(); ArrayList<Process> arrive =
	 * new ArrayList<>(); int time = -1; boolean hasout = false;
	 * 
	 * while (running(processes) && !exit) {
	 * 
	 * if (t == 74) System.out.println(waiting_for); add = new ArrayList<>();
	 * arrive = new ArrayList<>();
	 * 
	 * waiting = waiting_next || waiting; boolean arrived = false; // NOTE: once
	 * processes are finished, burst_amt--, io_time_current // starts to
	 * decrement
	 * 
	 * for (Process p : processes) { if (p.state != State.TERMINATED) { exit =
	 * false; } if (p.number_bursts > 0) { exit = false; } if (p.burst_current >
	 * 0 || p.burst_current == p.cpu_burst_time) { exit = false; } }
	 * 
	 * if (exit) { t += t_cs / 2; exit = true; break; }
	 * 
	 * for (Process p : processes) { if (p.state == State.TERMINATED) {
	 * q.remove(p); continue; } if (q.contains(p)) { continue; }
	 * 
	 * if (p.initial_arrival_time == t) { p.state = State.READY; arrive.add(p);
	 * // JUST FOR ARRIVAL time = t; hasout = true; arrived = true; } else if (t
	 * == p.io_time_next) { // FOR IO FINISH p.io_time_current = p.io_time; //
	 * resetting io_time if (p.number_bursts == 0) { p.state = State.TERMINATED;
	 * // IF IO was the last thing // to do... terminate } else { add.add(p);
	 * time = t; hasout = true; p.state = State.READY; if (q.size() == 0 &&
	 * running_index(processes) == -1) { waiting_next = true; if (waiting_for <
	 * 4) waiting_for = 4; } } } }
	 * 
	 * if (exit) continue;
	 * 
	 * for (Process ps : processes) { if (ps.state == State.BLOCKED) { if
	 * (ps.io_time_current > -1) ps.io_time_current--; } }
	 * 
	 * if (arrived == true) { if (running_index(processes) == -1) { waiting_next
	 * = true; if (waiting_for < 4) waiting_for = 4; }
	 * 
	 * for (Process ps : processes) { if (ps.state == State.READY) {
	 * ps.wait_time++; } } }
	 * 
	 * int running_index = running_index(processes);
	 * 
	 * if (waiting) { for (Process ps : processes) { if (State.READY ==
	 * ps.state) { ps.turnaround_time++; } if (State.RUNNING == ps.state) {
	 * ps.turnaround_time++; } } waiting_next = false; waiting_for--; if
	 * (waiting_for == 0) waiting = false; if (hasout) { ArrayList<Process>
	 * total = new ArrayList<>(); total.addAll(add); total.addAll(arrive);
	 * total.sort(new Comparator<Process>() {
	 * 
	 * @Override public int compare(Process o1, Process o2) { return
	 *           o1.process_id.compareTo(o2.process_id); } }); for (Process p :
	 *           total) { q.add(p); if (add.contains(p)) {
	 *           System.out.printf("time %dms: Process %s completed I/O; added
	 *           to ready queue %s\n", time, p.process_id, queueToString(q)); }
	 *           else { System.out.printf("time %dms: Process %s arrived and
	 *           added to ready queue %s\n", t, p.process_id, queueToString(q));
	 * 
	 *           } } hasout = false; } t++; } else if (running_index == -1 &&
	 *           !waiting_next) { // START A NEW // PROCESS for (Process ps :
	 *           processes) { if (State.READY == ps.state) {
	 *           ps.turnaround_time++; ps.wait_time++; } } if (q.isEmpty()) {
	 *           t++; if (hasout) { ArrayList<Process> total = new
	 *           ArrayList<>(); total.addAll(add); total.addAll(arrive);
	 *           total.sort(new Comparator<Process>() {
	 * @Override public int compare(Process o1, Process o2) { return
	 *           o1.process_id.compareTo(o2.process_id); } }); for (Process p :
	 *           total) { q.add(p); if (add.contains(p)) {
	 *           System.out.printf("time %dms: Process %s completed I/O; added
	 *           to ready queue %s\n", time, p.process_id, queueToString(q)); }
	 *           else { System.out.printf("time %dms: Process %s arrived and
	 *           added to ready queue %s\n", t, p.process_id, queueToString(q));
	 * 
	 *           } } hasout = false; } continue; } Process run = q.remove(); //
	 *           Additional wait time because we are just 'starting'
	 *           run.wait_time++;
	 * 
	 *           System.out.printf("time %dms: Process %s started using the CPU
	 *           %s\n", t, run.process_id, queueToString(q));
	 * 
	 *           run.state = State.RUNNING; run.burst_current--;
	 *           run.turnaround_time++;
	 * 
	 *           if (run.burst_current == 0) { run.state = State.BLOCKED;
	 *           run.io_time_current = run.io_time; } t++; for (Process ps :
	 *           processes) { if (State.READY == ps.state) {
	 *           ps.turnaround_time++; ps.wait_time++; } if (State.RUNNING ==
	 *           ps.state) { ps.turnaround_time++; } }
	 * 
	 *           } else { // CHECK IF PROCESS IS NOW OVER - IF SO - START IO,
	 *           burst-- for (Process ps : processes) { if (State.READY ==
	 *           ps.state) { ps.turnaround_time++; ps.wait_time++; } } if
	 *           (waiting_next && running_index == -1) { if (hasout) {
	 *           ArrayList<Process> total = new ArrayList<>();
	 *           total.addAll(add); total.addAll(arrive); total.sort(new
	 *           Comparator<Process>() {
	 * @Override public int compare(Process o1, Process o2) { return
	 *           o1.process_id.compareTo(o2.process_id); } }); for (Process p :
	 *           total) { q.add(p); if (add.contains(p)) {
	 *           System.out.printf("time %dms: Process %s completed I/O; added
	 *           to ready queue %s\n", time, p.process_id, queueToString(q)); }
	 *           else { System.out.printf("time %dms: Process %s arrived and
	 *           added to ready queue %s\n", t, p.process_id, queueToString(q));
	 * 
	 *           } } hasout = false; } continue; } Process run =
	 *           processes[running_index];
	 * 
	 *           run.turnaround_time++; run.burst_current--;
	 *           run.cpu_burst_time_actual++;
	 * 
	 *           if (run.burst_current == -1) {
	 * 
	 *           run.number_bursts--; if (run.number_bursts != 0) {
	 * 
	 *           if (q.containsAll(add) && !add.isEmpty() && time == t) {
	 *           Queue<Process> copy = new LinkedList<>(); copy.addAll(q);
	 *           copy.removeAll(add); run.io_time_next = t + run.io_time + t_cs
	 *           / 2; System.out.printf("time %dms: Process %s completed a CPU
	 *           burst; %d burst%s to go %s\n", t, run.process_id,
	 *           run.number_bursts, run.number_bursts > 1 ? "s" : "",
	 *           queueToString(copy)); System.out.printf( "time %dms: Process %s
	 *           switching out of CPU; will block on I/O until time %dms %s\n",
	 *           t, run.process_id, run.io_time_next, queueToString(copy)); for
	 *           (Process p : add) { q.add(p); System.out.printf("time %dms:
	 *           Process %s completed I/O; added to ready queue %s\n", t,
	 *           p.process_id, queueToString(q)); } hasout = false; } else {
	 * 
	 *           run.io_time_next = t + run.io_time + t_cs / 2;
	 *           System.out.printf("time %dms: Process %s completed a CPU burst;
	 *           %d burst%s to go %s\n", t, run.process_id, run.number_bursts,
	 *           run.number_bursts > 1 ? "s" : "", queueToString(q));
	 *           System.out.printf( "time %dms: Process %s switching out of CPU;
	 *           will block on I/O until time %dms %s\n", t, run.process_id,
	 *           run.io_time_next, queueToString(q)); } } else {
	 *           System.out.printf("time %dms: Process %s terminated %s\n", t,
	 *           run.process_id, queueToString(q)); run.state =
	 *           State.TERMINATED; run.io_time_current = 0; } run.burst_current
	 *           = run.cpu_burst_time; if (run.state != State.TERMINATED)
	 *           run.state = State.BLOCKED;
	 * 
	 *           // NOTE: THIS ONLY WOULD APPLY IF EVERY TIME A PROCESS //
	 *           ***ENDS*** THERE IS A CONTEXT SWITCH run.context_switches++;
	 * 
	 *           waiting = true; waiting_for = t_cs - 1;
	 * 
	 *           // ADD CONTEXT SWITCH TIME
	 * 
	 *           q.remove(run); }
	 * 
	 *           if (hasout) { ArrayList<Process> total = new ArrayList<>();
	 *           total.addAll(add); total.addAll(arrive); total.sort(new
	 *           Comparator<Process>() {
	 * @Override public int compare(Process o1, Process o2) { return
	 *           o1.process_id.compareTo(o2.process_id); } }); for (Process p :
	 *           total) { q.add(p); if (add.contains(p)) {
	 *           System.out.printf("time %dms: Process %s completed I/O; added
	 *           to ready queue %s\n", time, p.process_id, queueToString(q)); }
	 *           else { System.out.printf("time %dms: Process %s arrived and
	 *           added to ready queue %s\n", t, p.process_id, queueToString(q));
	 * 
	 *           } } hasout = false; }
	 * 
	 *           t++; if (!running(processes)) t += t_cs / 2 - 1; } }
	 * 
	 *           System.out.printf("time %dms: Simulator ended for FCFS\n\n",
	 *           t);
	 * 
	 *           int cpubursttime = 0; int total_cpu_bursts = 0; int waittime =
	 *           0; int turnaroundtime = 0; int contextswitches = 0; int
	 *           preemptions = 0;
	 * 
	 *           for (Process p : processes) { total_cpu_bursts +=
	 *           p.number_bursts_CONSTANT; cpubursttime +=
	 *           p.cpu_burst_time_actual; waittime += p.wait_time;
	 *           turnaroundtime += p.turnaround_time; contextswitches +=
	 *           p.context_switches; preemptions += p.preemptions;
	 * 
	 *           } String ret = "Algorithm FCFS\n"; ret += String.format("--
	 *           average CPU burst time: %.2f ms\n", (double) cpubursttime /
	 *           (double) total_cpu_bursts); ret += String.format("-- average
	 *           wait time: %.2f ms(%d/%d)\n", (double) waittime / (double)
	 *           total_cpu_bursts, waittime, total_cpu_bursts); ret +=
	 *           String.format("-- average turnaround time: %.2f ms(%d/%d)\n",
	 *           (double) turnaroundtime / (double) total_cpu_bursts,
	 *           turnaroundtime, total_cpu_bursts); ret += String.format("--
	 *           total number of context switches: %d\n", contextswitches); ret
	 *           += String.format("-- total number of preemptions: %d\n",
	 *           preemptions); return ret; }
	 */

	public static String fcfs(Process[] p) {
		int tBurstTime = 0;
		int tCS = 0;
		int tWait = 0;
		int tTurnaround = 0;
		int tPreemption = 0;
		Vector<Process> initial_copy = new Vector<Process>();
		for (Process ps : p)
			initial_copy.addElement(ps);
		Vector<Process> process_in_io = new Vector<>();
		Vector<Process> queue = new Vector<>();

		Sim simulator = new Sim();

		int global_counter = -1; // start out with -1 -> go to 0 at first timing
		boolean new_process_into_IO = false;
		int tBursts = 0;

		// Initial calculations because yaknow
		for (int i = 0; i < p.length; i++) {
			tBursts += p[i].burstsLeft();
			tBurstTime += p[i].burstsLeft() * p[i].cpu_burst_time;
			tTurnaround += p[i].burstsLeft() * simulator.t_cs / 2;
		}

		System.out.print("time 0ms: Simulator started for FCFS " + queueToString(queue));
		Vector<Process> added_turn = new Vector<>();
		Vector<String> added_prints = new Vector<>();

		while (!queue.isEmpty() || !process_in_io.isEmpty() || !initial_copy.isEmpty() || !simulator.idle()) {
			added_turn = new Vector<>(); // resetting printing vectors
			added_prints = new Vector<>();
			global_counter++; // increase t

			for (int i = 0; i < queue.size(); i++) {
				queue.get(i).wait_time++;
			}

			while (initial_copy.size() != 0) {
				if (global_counter == initial_copy.get(0).initial_arrival_time) {
					Process ps = initial_copy.get(0);
					added_turn.add(ps);
					initial_copy.remove(0);
					ps.ready = global_counter;
					added_prints.add("time " + global_counter + "ms: Process " + ps.process_id
							+ " arrived and added to ready queue ");
				} else
					break;
			}

			simulator.setCounter(global_counter);
			if (new_process_into_IO) {
				process_in_io.sort(new Comparator<Process>() {
					@Override
					public int compare(Process o1, Process o2) {
						return Integer.valueOf(o1.io_time_current).compareTo(Integer.valueOf(o2.io_time_current));
					}
				});
			}

			for (int i = 0; i < process_in_io.size(); i++) {
				process_in_io.get(i).io_time_current--;
			}

			while (!process_in_io.isEmpty()) {
				if (process_in_io.get(0).io_time_current == -4) {
					Process ps = process_in_io.get(0);
					added_turn.add(ps);
					added_prints.add("time " + global_counter + "ms: Process " + ps.process_id
							+ " completed I/O; added to ready queue ");
					ps.ready = global_counter;
					process_in_io.remove(0);
				} else
					break;
			}

			// if we context switch, we need to break out of the loop
			if (simulator.contextSwitch()) {
				printAll(added_turn, added_prints, queue);
				continue;
			}

			// CPU
			if (simulator.idle()) {
				printAll(added_turn, added_prints, queue);
				if (!queue.isEmpty()) {
					Process ps = queue.get(0);
					simulator.load(ps);
					tCS++;
					queue.remove(0);
					simulator.setIdle(false);
				}
			} else {
				// burst is either done or not done
				if (simulator.getCurrentProcess().burst_current > 0) { // process
																		// still
																		// running
					Process ps = simulator.getCurrentProcess();
					if (ps.burst_current == ps.cpu_burst_time) {
						System.out.print("time " + global_counter + "ms: Process " + ps.process_id
								+ " started using the CPU " + queueToString(queue));
					}
					simulator.burst();
				} else { // process is done...
					// finishes burst
					Process ps = simulator.getCurrentProcess();
					if (ps.burstsLeft() > 0) {
						System.out.print(
								"time " + global_counter + "ms: Process " + ps.process_id + " completed a CPU burst; "
										+ ps.burstsLeft() + " burst" + (ps.burstsLeft() > 1 ? "s" : "") + " to go ");
						tTurnaround += global_counter - ps.ready;
					} else {
						System.out.print("time " + global_counter + "ms: Process " + ps.process_id + " terminated ");
						tWait += ps.getWait();
						tTurnaround += global_counter - ps.ready;
					}
					System.out.print(queueToString(queue));
					ps.burst_current = ps.cpu_burst_time;
					ps.io_time_current = ps.io_time;
					simulator.setIdle(true);
					simulator.unload();
					if (ps.burstsLeft() > 0) {
						System.out.printf(
								"time %dms: Process %s switching out of CPU; will block on I/O until time %dms %s",
								global_counter, ps.process_id, global_counter + ps.io_time + simulator.t_cs / 2,
								queueToString(queue));
						queueToString(queue);
						process_in_io.add(ps);
						new_process_into_IO = true;
					}
				}
				printAll(added_turn, added_prints, queue);
			}

		}

		System.out.println("time " + (global_counter + 4) + "ms: Simulator ended for FCFS");

		double wait = tWait / (double) tBursts;
		double turnaround = tTurnaround / (double) tBursts;
		double burst = tBurstTime / (double) tBursts;

		String ret = "Algorithm FCFS\n";
		ret += String.format("-- average CPU burst time: %s ms\n", Project1.format(burst));
		ret += String.format("-- average wait time: %s ms\n", Project1.format(wait));
		ret += String.format("-- average turnaround time: %s ms\n", Project1.format(turnaround));
		ret += String.format("-- total number of context switches: %d\n", tCS);
		ret += String.format("-- total number of preemptions: %d\n", tPreemption);

		return ret;
	}
	
	 private static String format(double d)
	    {
	        DecimalFormat df = new DecimalFormat();
	        df.setMinimumFractionDigits(2);
	        df.setMaximumFractionDigits(2);
	        df.setGroupingUsed(false);
	        df.setRoundingMode(RoundingMode.HALF_EVEN);
	        return df.format(d);
	    }

	public static void printAll(Vector<Process> added_turn, Vector<String> added_prints, Vector<Process> ready_queue) {
		if (added_turn.size() == 0)
			return;
		int[] order = new int[added_turn.size()];
		String[] process = new String[added_turn.size()];
		for (int i = 0; i < added_turn.size(); i++) {
			process[i] = added_turn.elementAt(i).process_id;
		}
		String[] temp = process.clone();
		Arrays.sort(process);

		for (int i = 0; i < process.length; i++) {
			int j = 0;
			for (j = 0; j < temp.length; j++) {
				if (temp[i].equals(process[j])) {
					break;
				}
			}
			order[i] = j;
		}

		for (int i = 0; i < added_turn.size(); i++) {
			if (frontOfQueue) {
				ready_queue.add(0, added_turn.get(order[i]));
			} else {
				ready_queue.add(added_turn.get(order[i]));
			}
			System.out.print(added_prints.get(order[i]) + queueToString(ready_queue));
		}

	}

	private static String queueToStringSRT(Vector<Process> ready_queue) {
		String f = "[Q";

		ready_queue.sort(new Comparator<Process>() {

			@Override
			public int compare(Process o1, Process o2) {
				return ((Integer) o1.burst_current).compareTo(o2.burst_current);
			}
		});

		if (ready_queue == null || ready_queue.isEmpty())
			f += " <empty>";
		else
			for (Process p : ready_queue)
				f += " " + p.process_id;
		f += "]\n";
		return f;
	}

	private static String queueToString(Vector<Process> ready_queue) {
		String f = "[Q";
		if (ready_queue == null || ready_queue.isEmpty())
			f += " <empty>";
		else
			for (Process p : ready_queue)
				f += " " + p.process_id;
		f += "]\n";
		return f;
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

	public static class Pair {
		Process t;
		int v;

		public Pair(Process t, int v) {
			this.t = t;
			this.v = v;
		}
	}

	public static class Preemptee {
		int timeRemaining = 4;
		Process p;

		public Preemptee(Process p) {
			this.p = p;
		}

	}

	public static class Process implements Comparable<Process> {
		int ready;
		String process_id;
		int initial_arrival_time;
		int cpu_burst_time;
		int number_bursts;
		int remainingTime;

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
			return String.format("|%s|||io_time_current: %d|burst_current: %d|state: %s|bursts: %d|", process_id,
					io_time_current, burst_current, state, burstsLeft());
		}

		public int getWait() {
			return wait_time;
		}

		public int burstsLeft() {
			return number_bursts;
		}

		@Override
		public int compareTo(Process o) {
			return Integer.compare(this.burst_current, o.burst_current);
		}

		public void burst() {
			if (burstsLeft() == 0) {
				return;
			}
			burst_current--;
			if (burst_current == 0) {
				number_bursts--;
				io_time_current = io_time;
			}
		}
	}

	public static class Sim {

		int t_cs; // input
		Process current_process = null;
		long counter; // timing
		long switch_over; // how long to be in context switch (t_cs/2 : x < 4)
		boolean in_switch; // context switch or na
		boolean idle; // idle or not]
		boolean load; // loading a new process -> context switch
		Process last_process = null;
		// Add in methods/variables for TSing

		public Sim() {
			t_cs = 8;
			counter = 0;
			idle = true;
			switch_over = -1;
		}

		Process getCurrentProcess() {
			return current_process;
		}

		boolean idle() {
			return idle;
		}

		boolean contextSwitch() {
			if (counter >= switch_over) {
				return false;
			}
			return true;
		}

		void setCounter(long time_ms) {
			counter = time_ms;
		}

		void setProcess(Process p) {
			current_process = p;
		}

		void setIdle(boolean state) {
			idle = state;
		}

		// HELPER
		void load(Process p) {
			current_process = p;
			switch_over = counter + t_cs / 2;
		}

		void loadNewProcessForSRT(Process p) {
			current_process = p;
			switch_over = counter + t_cs;
		}

		void loadNewProcessForSRT2(Process p) {
			current_process = p;
			switch_over = counter + 9;
		}

		void unload() {
			switch_over = counter + t_cs / 2;
		}

		void burst() {
			current_process.burst();
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
	}

}
