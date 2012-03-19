import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class TraceSimplifier {

  public void analyzeTraceFile(String[] args) throws IOException {
    String resultFile = args[0]// args[0].substring(0, args[0].lastIndexOf('\\')
                               // + 1)
        + "\\result\\result.txt";
    BufferedWriter resultWriter = new BufferedWriter(new FileWriter(resultFile,
        false));

    String files;
    File folder = new File(args[0]);
    File[] listOfFiles = folder.listFiles();

    for (int i = 0; i < listOfFiles.length; i++) {

      if (listOfFiles[i].isFile()) {
        files = listOfFiles[i].getAbsolutePath();
        if (files.endsWith(".txt") || files.endsWith(".TXT")) {

          BufferedReader reader = new BufferedReader(new FileReader(files));
          String line = "";
          ArrayList<String> buggyTraceContent = new ArrayList<String>();
          try {
            while ((line = reader.readLine()) != null
                && !line.startsWith("##BUG"))
              ;
            buggyTraceContent.add(line);
            while ((line = reader.readLine()) != null && !line.startsWith("##")) {
              buggyTraceContent.add(line);
            }
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }

          int[] bugActors = null;
          if (line != null && line.startsWith("##BUGGY_ACTORS")) {
            bugActors = new int[Integer.parseInt((line.split(":")[1].trim()))];
            for (int j = 0; j < bugActors.length; j++) {
              bugActors[j] = Integer.parseInt(reader.readLine());
            }
            // if ((line = reader.readLine())!= null &&
            // line.startsWith("##UNDELIVERED_MSGS")){
            // int underliveredCount  =
            // Integer.parseInt(line.substring(line.lastIndexOf(":")+1).trim());
            // for (int c =0 ;c<underliveredCount;c++){
            // int receiver = reader.readLine().;
            //
            // }
            // }
          }
          int removedLines = 0;
          if (bugActors != null) {
            removedLines = simplifyTrace(bugActors, buggyTraceContent, files);
            resultWriter.write(files.substring(files.lastIndexOf('\\')+1) + ": " + removedLines +"/"+
                buggyTraceContent.size()+"= "+(float) removedLines
                / (float) buggyTraceContent.size() + "\n");
          }

        }
      }
    }
    resultWriter.close();

  }

  private int simplifyTrace(int[] bugActors,
      ArrayList<String> traceContent, String fileName) throws IOException {
    String simplifiedFileName = fileName.substring(0, fileName.indexOf('.'))
        + "-simplified" + fileName.substring(fileName.indexOf('.'));
    BufferedWriter writer = new BufferedWriter(new FileWriter(
        simplifiedFileName, false));
    // writer.write(traceContent.get(0));
    // writer.newLine();
    ArrayList<String> lastLines = findLastBugRelatedLines(bugActors,
        traceContent);
    int nrOfRemovedLines = 0;
    boolean breakLoop = false;
    traceContent.remove(0);
    for (String line : traceContent) {
      String[] vc = getVC(line);
      breakLoop = false;
      for (String lastLine : lastLines) {
        String[] lastVC = getVC(lastLine);
        if (isLessThanOrEqual(vc, lastVC)) {
          writer.write(line);
          writer.newLine();
          breakLoop = true;
          break;
        }
      }
      if (!breakLoop) {
        System.out.println(line);
        nrOfRemovedLines++;
      }

    }

    writer.write("RemovedLines: " + nrOfRemovedLines + " out of "
        + traceContent.size() + ", " + (float) nrOfRemovedLines * 100.00
        / (float) traceContent.size() + "%");
    writer.close();
    return nrOfRemovedLines;

  }

  private ArrayList<String> findLastBugRelatedLines(int[] bugActors,
      ArrayList<String> traceContent) {
    ArrayList<Integer> bugActorsList = new ArrayList<Integer>();
    for (int actorID : bugActors) {
      bugActorsList.add(actorID);
    }
    ArrayList<String> result = new ArrayList<String>();
    // String firstLine = traceContent.get(0);
    for (int i = traceContent.size() - 1; i >= 0 & bugActorsList.size() > 0; i--) {
      int receiver = Integer.parseInt((traceContent.get(i).split(":"))[1]);
      if (bugActorsList.contains(receiver)) {
        // receivers.add(receiver);
        bugActorsList.remove((Object) receiver);
        result.add(traceContent.get(i));
      }
    }

    return result;
  }

  private ArrayList<String> findLastRecievers(
      ArrayList<String> traceContent) {
    ArrayList<String> result = new ArrayList<String>();
    String firstLine = traceContent.get(0);
    String[] receivers = firstLine
        .substring(firstLine.indexOf('[') + 1, firstLine.indexOf(']')).trim()
        .split(",");
    ArrayList<Integer> receiversID = new ArrayList<Integer>();
    for (String receiver : receivers) {
      receiversID.add(Integer.parseInt(receiver.trim()));
    }
    for (int i = traceContent.size() - 1; i > 0; i--) {
      int receiver = Integer.parseInt((traceContent.get(i).split(":"))[1]);
      if (receiversID.contains(receiver)) {
        // receivers.add(receiver);
        receiversID.remove((Object) receiver);
        result.add(traceContent.get(i));
      }
    }

    return result;
  }

  private String[] getVC(String line) {
    return line.substring(line.indexOf('[') + 1, line.indexOf(']')).trim()
        .split(",");
  }

  private boolean isLessThanOrEqual(String[] vc1, String[] vc2) {
    for (int i = 0; i < vc1.length; i++) {
      if (Integer.parseInt(vc1[i].trim()) > Integer.parseInt(vc2[i].trim()))
        return false;
    }
    return true;
  }

  public void generateDelta(String fileName) {

  }

}
