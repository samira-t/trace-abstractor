import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

public class Main {

	public static void main(String[] args) {
		runActorScheudleAbstractor();

	}

	public static void runMTScheduleAbstractor() {
		String fileName = "C:\\Users\\tasharo1\\Desktop\\actors\\mttraces\\BoundedBufferTestLog";// SimpleIntTestLog";
		// String fileName =
		// "C:\\Users\\tasharo1\\Desktop\\actors\\mttraces\\TestLog.txt";
		// String fileName =
		// "C:\\Users\\tasharo1\\Desktop\\actors\\mttraces\\SimpleIntTestLog";
		ScheduleAbstractor abstractor = new MTScheduleAbstractor();
		try {
			abstractor.abstractSchedules(new String[] { fileName, "putThread", "getThread" });

			// abstractor.abstractSchedules(new String[]{fileName,"t2","t1"});

			// abstractor.abstractSchedules(new String[]{fileName,"t1","t2"});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void runActorScheudleAbstractor() {
		String bugTraceFolder = "./inputs/full/";
		String dropboxTraceFolder = "C:\\Users\\Samira\\Dropbox\\jpf-actor (1)\\bugtraces\\buginfo\\full\\";
		String outputFolder1 = ".\\output1\\";
		String outputFolder2 = ".\\output2\\";
		String outputFolder_noparam = ".\\output-noparam\\";
		String outputFolder_newSeq = ".\\output-newseq\\";
		String outputFolder_symmetry = ".\\output-symmetry-new\\";
		String outputFolder_symmetry_minimalconst = ".\\output-sym-minimalconst\\";
		String temp = ".//temp//";

		File traceFolder = new File(bugTraceFolder);
		String outputFolder = temp;
		Logger.setSummaryWriter(outputFolder + "summary-2.txt");
		HashSet<String> processedFiles = new HashSet<String>();
		File[] files = traceFolder.listFiles();

		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return (f1.getName().compareTo(f2.getName()) * -1);
			}
		});

		for (File traceFile : files) {
			String subject = traceFile.getName().split("_")[0];
			if (traceFile.getName().contains("leader_election_3_1"))
				subject = subject + "0";
			else if (traceFile.getName().contains("leader_election_"))
				subject = subject + "2";
			if (traceFile.length() > 0 // && subject.contains("procreg")

					&& traceFile.length() < (1024 * 1024 * 15) && traceFile.getName().contains("eca") && !processedFiles.contains(subject)

			/* && traceFile.getName().contains("procreg-test") */) {
				// the
				processedFiles.add(subject);

				// String traceFile =
				// "minesweeper_bugtraces_bugbound_limit_2_heuristic_stack.txt";
				// String traceFile =
				// "leader_election_3_1_bugtraces_bugbound_limit_2_heuristic_messageflow_zeroleader.txt";
				// String traceFile =
				// "minesweeper_bugtraces_bugbound_limit_1_heuristic_lca.txt";
				// String traceFile =
				// "procreg1_bugtraces_bugbound_limit_2_heuristic_lca.txt";
				// String traceFile =
				// "sleepingbarber_bugtraces_bugbound_limit_2_heuristic_stack.txt";
				// String traceFile =
				// "musicplayer3_bugtraces_bugbound_limit_1_heuristic_eca.txt";
				// String traceFile =
				// "activestandby_bugtraces_bugbound_limit_2_heuristic_queue.txt";
				// String traceFile =
				// "leader_election_3_1_bugtraces_bugbound_limit_1_heuristic_stack_zeroleader.txt";
				// String fileName =
				// "C:\\Users\\tasharo1\\Documents\\My Dropbox\\all-traces\\leader_election_3_1\\twoleader\\leader_election_3_1_bugtraces_bugbound_limit_1_heuristic_eca_twoleader.txt";//SimpleIntTestLog";
				// String fileName =
				// "C:\\Users\\tasharo1\\Documents\\My Dropbox\\jpf-actor (1)\\bugtraces\\buginfo\\full\\leader_election_3_1_bugtraces_bugbound_limit_3_heuristic_eca_twoleader.txt";
				// String fileName = bugTraceFolder + traceFile;

				ScheduleAbstractor abstractor = new ActorScheduleAbstractor();
				try {
					String outputFile = outputFolder + traceFile.getName().replace(".txt", "-out.txt");
					Logger.setScheuleWriter(outputFile);
					abstractor.abstractSchedules(new String[] { traceFile.getAbsolutePath() });
					// break;
					// "C:\\Users\\tasharo1\\workspace_scala\\traceparser\\output2\\"
					// + traceFile.replace(".txt", "-out.txt") });

					// abstractor.abstractSchedules(new
					// String[]{fileName,"t2","t1"});

					// abstractor.abstractSchedules(new
					// String[]{fileName,"t1","t2"});
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println(traceFile.getName());
				} finally {
					Logger.closeScheduleWriter();
				}
			}
		}
		Logger.closeSummaryWriter();

	}
}
