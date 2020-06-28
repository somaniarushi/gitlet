package gitlet;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author AMK Somani
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> ....
     *  init
     *  */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                throw Utils.error("Please enter a command.");
            }
            Repo repo = new Repo(args);
        } catch (GitletException e) {
            System.out.print(e.getMessage());
        } finally {
            System.exit(0);
        }
    }
}
