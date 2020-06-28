package gitlet;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** A class that acts as a correspondent to the gitlet directory,
 * handling and dispatching jobs to different classes, and throwing errors
 * where necessary.
 * @author AMK Somani
 */
public class Repo {

    /** Current Working Directory For Gitlet Repo. */
    public static final File CWD = new File(".gitlet");

    /** File for storing the staging area. */
    public static final File INDEX = Utils.join(CWD, "index.txt");

    /** Directory for storing blobs and commits.  */
    public static final File OBJECTS = Utils.join(CWD, "objects");

    /** File for storing name of currently active head branch. */
    public static final File HEAD = Utils.join(CWD, "head.txt");

    /** Folder for storing pointers to most recent commits of branches. */
    public static final File BRANCHES = Utils.join(CWD, "branches");

    /** Folder for storing remotes.*/
    public static final File REMOTES = Utils.join(CWD, "remote");

    /** A repository that takes in arguments and performs
     * actions on the corresponding gitlet repository. ARGS is the
     * arguments passed alongside gitlet call.
     */
    public Repo(String[] args) {
        String action = args[0];
        switch (action) {
        case "init":
            handleInit(args);
            break;
        case "add":
            handleAdd(args);
            break;
        case "commit":
            handleCommit(args);
            break;
        case "rm":
            handleRemove(args);
            break;
        case "log":
            handleLog(args);
            break;
        case "global-log":
            handleGlobalLog(args);
            break;
        case "find":
            handleFind(args);
            break;
        case "checkout":
            handleCheckout(args);
            break;
        case "status" :
            handleStatus(args);
            break;
        case "branch":
            handleBranch(args);
            break;
        case "rm-branch":
            handleRmBranch(args);
            break;
        case "reset":
            handleReset(args);
            break;
        case "merge":
            handleMerge(args);
            break;
        case "add-remote":
            handleRemoteAdd(args);
            break;
        case "rm-remote":
            handleRemoteRemove(args);
            break;
        case "push":
            handlePush(args);
            break;
        case "fetch":
            handleFetch(args);
            break;
        default:
            throw Utils.error(
                    "No command with that name exists.");
        }
    }

    /** Handles a request to initialize gitlet. If the form
     * of ARGS is incorrect or gitlet is already initialized,
     * throws error. Otherwise, initialize all repos,
     * make branch master, and make the initial commit.
     */
    private void handleInit(String[] args) throws GitletException {
        checkForm(args, 1);
        if (initialized()) {
            throw Utils.error(
                    "A Gitlet version-control system already "
                            + "exists in the current directory.");
        } else {
            makeGitlet();
            Branch master = new Branch("master");
            master.setHead();
            Commit.create("initial commit", 0);
        }
    }

    /** Handles a request to add a file to gitlet. If the form
     * of ARGS is incorrect or gitlet is not initialized, throws error.
     * Otherwise, blobs the file to the objects repo.
     */
    private void handleAdd(String[] args) throws GitletException {
        checkForm(args, 2);
        checkInitialized();
        Stage.stage(args[1]);
    }

    /** Handles a request to commit the currently staged files
     * with message from ARGS.
     * */
    private void handleCommit(String[] args) throws GitletException {
        if (args.length != 2 || args[1].equals("")) {
            throw Utils.error("Please enter a commit message.");
        }
        checkInitialized();
        Commit.create(args[1], Instant.now().getEpochSecond());
    }

    /** Handles a request to remove a certain file form ARGS from
     * staging area if present there,
     * and untracking it and removing from working directory if
     * it exists there, throwing
     * exceptions if no file available to remove.
     */
    private void handleRemove(String[] args) {
        checkForm(args, 2);
        checkInitialized();
        Stage.antistage(args[1]);
    }

    /** Handles a request to output the log from ARGS, chasing
     * each commit's parents.
     */
    private void handleLog(String[] args) {
        checkForm(args, 1);
        checkInitialized();
        Commit.log();
    }

    /** Handles a request to output the global log from ARGS. */
    private void handleGlobalLog(String[] args) {
        checkForm(args, 1);
        checkInitialized();
        Commit.globalLog();
    }

    /** Handles a request to find all the commit ids with the
     * same message as that in ARGS.
     */
    private void handleFind(String[] args) {
        checkForm(args, 2);
        checkInitialized();
        Commit.find(args[1]);
    }

    /** Handles a request to display the current status of
     * the repo from ARGS. */
    private void handleStatus(String[] args) {
        checkForm(args, 1);
        checkInitialized();
        Repo.display();
    }

    /** Handles a request to checkout the given file with the
     * given commit id, or a file from the most recent commit,
     * or a branch from ARGS, throwing necessary errors.
     */
    private void handleCheckout(String[] args) {
        if (args.length == 2) {
            handleBranchCheckout(args[1]);
        } else if (args.length == 3) {
            if (args[1].equals("--")) {
                handleFileCheckout(args[2]);
            } else {
                checkForm(args, 2);
            }
        } else if (args.length == 4) {
            if (args[2].equals("--")) {
                handleFileCheckout(args[3], args[1]);
            } else {
                checkForm(args, 3);
            }
        } else {
            checkForm(args, 3);
        }
    }

    /** Handles a request to create a new branch from ARGS,
     * throwing errors where necessary.
     * */
    private void handleBranch(String[] args)
            throws GitletException {
        checkForm(args, 2);
        checkInitialized();
        Branch.checkDouble(args[1]);
        Branch b = new Branch(args[1]);
    }

    /** Handles a request to remove a branch from ARGS,
     * throwing errors where necessary. */
    private void handleRmBranch(String[] args)
            throws GitletException {
        checkForm(args, 2);
        checkInitialized();
        Branch.checkExistsRm(args[1]);
        Branch.checkIsActiveRm(args[1]);
        Branch.delete(args[1]);
    }

    /** Checkout all the files from a certain commit
     * from ARGS, unless there is an untracked file in the
     * way. Remove all tracked files not tracked in given commit,
     * clear the stage.
     */
    private void handleReset(String[] args)
            throws GitletException {
        checkForm(args, 2);
        checkInitialized();
        String id = Commit.fullCommit(args[1]);
        Commit.checkExists(id);
        Commit.checkUntracked(id);
        Commit.reset(id);
        Stage.clearStage();
        Branch.updateBranch(Branch.head(), id);
    }

    /** Merge another branch from ARGS into the current branch. */
    public void handleMerge(String[] args) throws GitletException {
        checkForm(args, 2);
        checkInitialized();
        Stage.checkStaged();
        Branch.checkExistsRm(args[1]);
        if (Branch.head().equals(args[1])) {
            throw Utils.error(
                    "Cannot merge a branch with itself.");
        }
        Branch.checkUntracked(args[1]);

        Branch.merge(args[1]);
    }

    /** Handles the display for all the current states of
     * the gitlet directory.
     * */
    private static void display() {
        String branchdisplay = "=== Branches ===\n";
        branchdisplay += Branch.display();

        String stageDisplay = "\n=== Staged Files ===\n";
        stageDisplay += Stage.stageDisplay();

        String removeDisplay = "\n=== Removed Files ===\n";
        removeDisplay += Stage.removeDisplay();

        String mods =
                "\n=== Modifications Not Staged For Commit ===\n";
        mods += Commit.modified();

        String untracked = "\n=== Untracked Files ===\n";
        untracked += Commit.getUntracked();

        String display = branchdisplay + stageDisplay
                + removeDisplay + mods + untracked;
        Utils.message(display);
    }

    /** Handle a request to check out a file FILENAME out
     * of the most recent commit. */
    private void handleFileCheckout(String filename) {
        handleFileCheckout(filename, Commit.currCommit());
    }

    /** Handle a request to check out a file FILENAME out of
     * the commit with code COMMIT. */
    private void handleFileCheckout(String filename, String commit)
            throws GitletException {
        checkInitialized();
        commit = Commit.fullCommit(commit);
        Commit.checkExists(commit);

        Commit c = Commit.getCommit(commit);
        if (!c.containsFile(filename)) {
            throw Utils.error(
                    "File does not exist in that commit.");
        }
        c.checkout(filename);
        Stage.removeFromStage(filename);
    }

    /** Handle a request to check out a branch named BRANCH. */
    private void handleBranchCheckout(String branch)
            throws GitletException {
        checkInitialized();
        Branch.checkExists(branch);
        Branch.checkIsActive(branch);
        Branch.checkUntracked(branch);
        Branch.checkout(branch);
        Stage.clearStage();
        Branch.setHeadBranch(branch);
    }

    /** Remotely add to a given remote directory from ARGS. */
    private void handleRemoteAdd(String[] args) throws GitletException {
        checkForm(args, 3);
        String remoteName = args[1];
        String path = args[2];
        File f = Utils.join(REMOTES, remoteName + ".txt");
        if (f.exists()) {
            throw Utils.error("A remote with that name already exists.");
        } else {
            createFile(f);
            Utils.writeContents(f, path);
        }
    }

    /** Remove a remote repository from ARGS. */
    private void handleRemoteRemove(String[] args) throws GitletException {
        checkForm(args, 2);
        String remoteName = args[1];
        File f = Utils.join(REMOTES, remoteName + ".txt");
        if (!f.exists()) {
            throw Utils.error("A remote with that name does not exist.");
        } else {
            f.delete();
        }
    }

    /** Handle a push command on the given remote repo from ARGS. */
    private void handlePush(String[] args) throws GitletException {
        checkForm(args, 3);
        File f = Utils.join(REMOTES, args[1] + ".txt");
        if (!f.exists()) {
            throw Utils.error("Remote directory not found.");
        }
        File remote = Utils.join(Utils.readContentsAsString(f));
        if (!remote.exists()) {
            throw Utils.error("Remote directory not found.");
        }
        Commit commit = Branch.getRemoteHead(args[2], f);
        if (commit == null) {
            createAndAppend(args[2],
                    Utils.join(Utils.readContentsAsString(f)));
        } else {
            Commit head = Commit.getCommit(Branch.getBranchCode(Branch.head()));
            List<Commit> history = head.getHistory();
            if (!history.contains(commit)) {
                throw Utils.error(
                        "Please pull down remote changes before pushing.");
            }
            String lastSHA = append(history, commit,
                            Utils.join(Utils.readContentsAsString(f)));
            Branch.setRemoteHead(f, args[2], lastSHA);
        }
    }

    /** Handles a request to fetch the head of a given file
     * from ARGS. */
    private void handleFetch(String[] args) throws GitletException {
        checkForm(args, 3);
        File f = Utils.join(REMOTES, args[1] + ".txt");
        if (!f.exists()) {
            throw Utils.error("Remote directory not found.");
        }
        File remote = Utils.join(Utils.readContentsAsString(f));
        if (!remote.exists()) {
            throw Utils.error("Remote directory not found.");
        }
        File branch = Utils.join(remote, "branches", args[2] + ".txt");
        if (!branch.exists()) {
            throw Utils.error("That remote does not have that branch.");
        }
    }

    /** Create a branch BRANCHNAME, append all the files in current head's
     * history to REPO and then update the head of that file to the last commit.
     */
    private void createAndAppend(String branchName, File repo) {
        File branchFile = Utils.join(repo, "branches", branchName + ".txt");
        createFile(branchFile);
        Commit head = Commit.getCommit(Branch.head());
        List<Commit> history = head.getHistory();
        File objects = Utils.join(repo, "objects");
        Commit last = null;
        for (int i = 0; i < history.size(); i += 1) {
            Commit curr = history.get(i);
            String sha = curr.sha1();
            File f = Utils.join(objects, sha + ".txt");
            createFile(f);
            Utils.writeObject(f, curr);
            last = curr;
        }
        Utils.writeContents(branchFile, last.sha1());
    }

    /** Returns after Append all the commits made after COMMIT in HISTORY
     * to the gitlet repository REPO. Returns the SHACode of the
     * last committed object.
     */
    private String append(List<Commit> history, Commit commit, File repo) {
        File objects = Utils.join(repo, "objects");
        Commit last = null;
        for (int i = 0; i < history.size(); i += 1) {
            Commit curr = history.get(i);
            if (curr.getTime() > commit.getTime()) {
                String sha = curr.sha1();
                File f = Utils.join(objects, sha + ".txt");
                createFile(f);
                Utils.writeObject(f, curr);
                last = curr;
            }
        }
        if (last != null) {
            return last.sha1();
        } else {
            return "unreachable";
        }
    }

    /** Making the gitlet directory structure. */
    private void makeGitlet() {
        createDir(CWD);
        createDir(OBJECTS);
        createDir(BRANCHES);
        createDir(REMOTES);
        createFile(INDEX);
        createFile(HEAD);
    }

    /** Creates a new file in the gitlet repo through given file FILE.
     * Assumes that the definition for creation is correct.
     * Supposed to be useable by ever class in package.
     */
    public static void createFile(File file) {
        if (file.exists()) {
            return;
        }
        try {
            file.createNewFile();
        } catch (IOException unreachable) {
            System.out.println(unreachable);
        }
    }

    /** Creates a new directory in the gitlet repo through
     * given file FILE. Assumes that the definition for creation
     * is correct. Supposed to be usable by every class in package.
     */
    public static void createDir(File file) {
        if (file.exists()) {
            return;
        }
        file.mkdir();
    }

    /** Returns true if file NAME is one of the files that shouldn't
     * be deleted or changed in any form, ever. */
    public static boolean neverConsider(String name) {
        if (name.equals("Makefile")) {
            return true;
        }
        if (name.equals("proj3.iml")) {
            return true;
        }
        return false;
    }

    /** Throws an error if the passed ARGS does not have
     * FORM number of arguments. */
    private void checkForm(String[] args, int form)
            throws GitletException {
        if (args.length != form) {
            throw Utils.error(
                    "Incorrect operands.");
        }
    }

    /** Throws an error if the gitlet repo is not initialized. */
    private void checkInitialized() throws GitletException {
        if (!initialized()) {
            throw Utils.error(
                    "Not in an initialized Gitlet directory.");
        }
    }

    /** Returns true if the gitlet repo is initialized. */
    private boolean initialized() {
        return CWD.exists();
    }

}
