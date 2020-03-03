import java.io.*;
import java.util.Arrays;
import java.util.List;

public class SimpleShell {

    public static void main(String[] args) throws java.io.IOException {
        boolean done = false;
        String cmd;
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        ProcessBuilder pb = new ProcessBuilder();
        // create history if not exists
        // history is kept in a text file so it's persistent
        File history = new File("history.txt");
        history.createNewFile();
        while (!done) {
            // read what the user entered
            System.out.print("MyShell>");
            cmd = console.readLine();
            if (cmd.equals("")) {
                continue;
            }
            List<String> cmd_list = Arrays.asList(cmd.trim().split(" "));
            if (cmd_list.get(0).equals("exit")) {
                // made the design choice that exit should work even when given arguments
                done = true;
                write("exit");
                continue;
            }
            // last command
            if (cmd_list.get(0).equals("!!")) {
                if (cmd_list.size() == 1) {
                    BufferedReader historyRead = new BufferedReader((new FileReader("history.txt")));
                    String line;
                    String lastline = "";
                    while ((line = historyRead.readLine()) != null) {
                        lastline = line;
                    }
                    historyRead.close();

                    // if there was no previous command
                    if (lastline.equals("")) {
                        System.out.println("No previous command found.");
                        continue;
                    }
                    // easier to detect exits here than redo code structure
                    if (lastline.equals("exit")) {
                        done = true;
                        write("exit");
                        continue;
                    }
                    // set command to last command and DO NOT continue
                    cmd = lastline;
                    cmd_list = Arrays.asList(lastline.split(" "));
                } else {
                    System.out.println("Command \"!!\" does not take any further arguments.");
                    continue;
                }
            }
            // another previous command
            else if (cmd_list.get(0).startsWith("!")) {
                if (cmd_list.size() == 1) {
                    try {
                        int index = Integer.parseInt(cmd_list.get(0).substring(1));
                        if (index < 0) {
                            System.out.println(String.format("Invalid command: \"%s\"", cmd_list.get(0)));
                            continue;
                        }
                        BufferedReader historyReader = new BufferedReader(new FileReader("history.txt"));
                        historyReader.readLine(); // first line is always blank
                        String line = "";
                        // read up to desired command
                        for (int i = 0; i <= index; i++) {
                            line = historyReader.readLine();
                        }

                        // same execution as !!
                        historyReader.close();
                        if (line.equals("exit")) {
                            done = true;
                            write("exit");
                            continue;
                        }
                        cmd = line;
                        cmd_list = Arrays.asList(line.split(" "));
                    } catch (NumberFormatException | NullPointerException e) {
                        System.out.println(String.format("Invalid command: \"%s\"", cmd_list.get(0)));
                        continue;
                    }
                } else {
                    System.out.println("Command \"![COMMAND_NUMBER]\" does not take any further arguments.");
                    continue;
                }
            }

            // history
            if (cmd_list.get(0).equals("history")) {
                // check if usage is right
                if (cmd_list.size() == 1) {
                    BufferedReader historyRead = new BufferedReader(new FileReader("history.txt"));
                    String line;
                    int index = 0;
                    historyRead.readLine(); // first line is always blank
                    while ((line = historyRead.readLine()) != null) {
                        System.out.println(String.valueOf(index++).concat("\t").concat(line));
                    }

                    // history repeats itself
                    System.out.println(String.valueOf(index).concat("\t").concat("history"));

                    write(cmd);
                    historyRead.close();
                } else {
                    System.out.println("Command \"history\" does not take any further arguments.");
                }
                continue;
            }
            // change directories
            if (cmd_list.get(0).equals("cd") || cmd_list.get(0).equals("chdir")) {
                // make sure they've entered the right number of args
                if (cmd_list.size() > 2) {
                    System.out.println("Usage is \"cd [DIRECTORY NAME]\"");
                } else if (cmd_list.size() == 1) {
                    // go to home directory if given no commands
                    pb.directory(new File(System.getProperty("user.dir")));


                    write(cmd);
                } else {
                    String path = cmd_list.get(1);
                    File cur = pb.directory();
                    String current;
                    if (cur == null) { // ProcessBuilder.directory() returns null if in home dir
                        current = System.getProperty("user.dir");
                    } else {
                        current = cur.toString();
                    }
                    // add new filepath to old filepath
                    File new_dir = new File(current.concat("/".concat(path)));
                    // make sure the directory exists
                    if (new_dir.isDirectory()) {
                        pb.directory(new_dir);
                        write(cmd);
                    } else {
                        System.out.println(String.format("Directory %s does not exist.", new_dir));
                    }
                }
                continue;
            }

            // attempt to run all other commands
            try {
                // start process
                pb.command(cmd_list);
                Process process = pb.start();

                // get output
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                // close reader
                reader.close();

                // write to history
                write(cmd);
            } catch (IOException e) {
                System.out.println(String.format("Unrecognized command: \"%s\"", cmd));
            }

            /* The steps are:
             (1) parse the input to obtain the command and  any parameters
             (2) create a ProcessBuilder object
             (3) start the process
             (4) obtain the output stream
             (5) output the contents returned by the command */
        }
        // close buffer
        console.close();
    }

    private static void write(String command) throws IOException {
        BufferedWriter historyWrite = new BufferedWriter(new FileWriter("history.txt", true));
        historyWrite.write("\n".concat(command));
        historyWrite.close();
    }
}

