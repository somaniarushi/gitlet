package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.io.File;
import java.util.Set;
import java.util.Stack;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/** A commit class is a correspondent to a commit stored in the
 * gitlet directory, saving a snapshot of the memory of the
 * directory that is staged or had been tracked previously for
 * retrieval in the future.
 * @author AMK Somani
 */
public class Commit extends Saveable implements Serializable {

    /** A commit that saves a screenshot of coherent files at a certain
     * time TIME alongside a given message LOG in a tree-like hierarchy
     * with other commits. Always use the factory constructor CREATE to create
     * commit objects, and the commit constructor is private.
     */
    private Commit(String log, long time) {
        this.msg = log;
        this.timer = time;
        this.parent = currCommit();
        this.secondParent = "";
        this.shaMap = (parent.equals("")) ? new HashMap<>() : copyHashMap();
    }

    /** Return the copy of the hashmap from parent
     *  by cloning it, or returning null if there is no parent. */
    @SuppressWarnings("unchecked")
    private HashMap<String, String> copyHashMap() {
        Commit parentObj = parentObject();
        if (parentObj == null || parentObj.shaMap == null) {
            return null;
        }
        return (HashMap<String, String>) parentObj.shaMap.clone();
    }

    /** Return a commit object with the message MSG and timestamp TIMER
     * after creating it and storing it in the objects directory in gitlet.
     */
    public static Commit create(String msg, long timer)
            throws GitletException {
        Commit c = new Commit(msg, timer);
        c.updateFromStage();
        c.saveFile();
        c.updateHead();
        Stage.clearStage();
        return c;
    }

    /** Return a commit that is created with log MSG, with time TIMER,
     * and a second parent SP.
     */
    public static Commit create(String msg, long timer, String sP)
            throws GitletException {
        Commit c = new Commit(msg, timer);
        c.secondParent = sP;
        c.updateFromStage();
        c.saveFile();
        c.updateHead();
        Stage.clearStage();
        return c;
    }

    /** Recall the saved stage. If the commit is initial commit,
     * do not do anything.Else, add every change from the staging
     * area to this commit object, throwing errors where necessary.
     */
    private void updateFromStage() throws GitletException {
        Stage curr = Stage.savedStage();
        if (timer == 0) {
            return;
        }
        checkChanges(curr);
        for (String key: curr.shaMap().keySet()) {
            shaMap.put(key, curr.shaMap().get(key));
        }
        for (String key: curr.removeList().keySet()) {
            shaMap.remove(key);
        }
    }

    /** Throw an exception if there are no changes
     * from the staging area STAGE. */
    private void checkChanges(Stage stage) throws GitletException {
        if (stage == null) {
            throw Utils.error("No changes added to the commit.");
        }
        if (stage.shaMap().isEmpty() && stage.removeList().isEmpty()) {
            throw Utils.error("No changes added to the commit.");
        }
    }

    /** Update the head of the currently active branch to the SHA1 value
     * of this commit.
     */
    private void updateHead() {
        String name = Branch.head();
        Branch.updateBranch(name, shaCode());
    }

    /** Checkout the given file FILENAME from this commit,
     * assuming that the file exists in this commit.
     */
    public void checkout(String filename) {
        String scode = shaMap.get(filename);
        File corr = Utils.join(Repo.OBJECTS, scode + ".txt");
        Blob b = Utils.readObject(corr, Blob.class);
        File write = Utils.join("", filename);
        if (!write.exists()) {
            Repo.createFile(write);
        }
        Utils.writeContents(write, b.contents());
    }

    /** Checkout all the files in the given commit. */
    public void checkout() {
        for (String filename: shaMap.keySet()) {
            checkout(filename);
        }
    }

    /** Go through all the primary parents from current commit,
     * displaying each commit as needed.
     */
    public static void log() {
        Stack<Commit> s = new Stack<>();
        s.push(Commit.currCommitObj());
        while (!s.isEmpty()) {
            Commit curr = s.pop();
            curr.display();
            if (!curr.parent.equals("")) {
                s.push(curr.parentObject());
            }
        }
    }

    /** Go through all the primary and second parents
     * from current commit, displaying each commit as needed.
     */
    public static void globalLog() {
        List<String> objects = Utils.plainFilenamesIn(Repo.OBJECTS);
        for (String filename: objects) {
            File loc = Utils.join(Repo.OBJECTS, filename);
            Saveable obj = Utils.readObject(loc, Saveable.class);
            if (obj instanceof Commit) {
                ((Commit) obj).display();
            }
        }
    }

    /** Return a display string for all the files untracked in the working
     * directory.
     */
    public static String getUntracked() {
        String untracked = "";
        Commit currCommit = currCommitObj();
        Stage currStage = Stage.savedStage();
        List<String> workingfiles = Utils.plainFilenamesIn(".");
        for (String filename: workingfiles) {
            if (!Repo.neverConsider(filename)) {
                if (!currCommit.containsFile(filename)) {
                    if (currStage == null
                            || !currStage.containsFile(filename)) {
                        untracked += filename + "\n";
                    }
                }
            }
        }
        return untracked;
    }

    /** Return a fisplay string of the files modified in the working directory
     * in comparison to the current object.
     */
    public static String modified() {
        String mod = "";
        Commit curr = currCommitObj();
        Stage currStage = Stage.savedStage();
        List<String> workingfiles = Utils.plainFilenamesIn(".");
        for (String filename: curr.shaMap.keySet()) {
            if (!workingfiles.contains(filename)) {
                if (currStage == null
                        || !currStage.contains(filename)) {
                    mod += filename + " (deleted)\n";
                }
            } else {
                Blob b = Blob.checker(filename);
                if (!b.shaCode().equals(curr.shaMap.get(filename))) {
                    if (currStage == null
                        || !currStage.contains(filename)) {
                        mod += filename + " (modified)\n";
                    }
                }
            }
        }
        return mod;
    }

    /** Returns the common ancestor to two branches CURRENT and GIVEN. */
    public static Commit findCommonAncestor(String current, String given)
            throws GitletException {
        String mergeToID = Branch.getBranchCode(current);
        String mergeFromID = Branch.getBranchCode(given);
        return helper(given, mergeToID, mergeFromID);
    }

    /** Returns the common ancestor to commit IDs CURRENT and GIVEN,
     * checking out branch BRANCH when necessary.
     */
    public static Commit helper(String branch, String current, String given)
            throws GitletException {
        String currentMemory = current, givenMemory = given;
        ArrayList<String> currentHistory = new ArrayList<>();
        currentHistory.add(current);
        ArrayList<String> givenHistory = new ArrayList<>();
        givenHistory.add(given);

        while (!current.equals("")) {
            String currParent = getCommit(current).parent;
            String currSecond = getCommit(current).secondParent;
            if (!currParent.equals("")) {
                currentHistory.add(currParent);
            }
            if (!currSecond.equals("")) {
                currentHistory.add(currSecond);
            }
            current = currParent;
        }

        while (!given.equals("")) {
            String givenParent = getCommit(given).parent;
            String givenSecond = getCommit(given).secondParent;
            if (!givenSecond.equals("")) {
                givenHistory.add(givenSecond);
            }
            if (!givenParent.equals("")) {
                givenHistory.add(givenParent);
            }
            given = givenParent;
        }

        current = currentMemory; given = givenMemory;
        for (String commit: currentHistory) {
            if (givenHistory.contains(commit)) {
                if (commit.equals(given)) {
                    throw Utils.error(
                            "Given branch is an ancestor of the "
                                    + "current branch.");
                } else if (commit.equals(current)) {
                    Branch.checkout(branch);
                    throw Utils.error(
                            "Current branch fast-forwarded.");
                } else {
                    return getCommit(commit);
                }
            }
        }
        return null;
    }

    /** Return all the ancestors of the given commit. */
    public List<Commit> getHistory() {
        ArrayList<Commit> his = new ArrayList<>();
        Stack<Commit> tracker = new Stack<>();
        tracker.push(this);
        while (!tracker.isEmpty()) {
            Commit curr = tracker.pop();
            his.add(curr);
            if (!curr.parent.equals("")) {
                tracker.push(curr.parentObject());
            }
            if (!curr.secondParent.equals("")) {
                tracker.push(curr.secondParentObject());
            }
        }
        return his;
    }

    /** Return a list of the files that have been changed or added from commit
     * ANCESTOR to commit BRANCH. */
    public static List<String> changedFiles(Commit branch, Commit ancestor) {
        List<String> changed = new ArrayList<>();
        for (String filename: branch.shaMap.keySet()) {
            String ancestorCode = ancestor.shaMap.get(filename);
            String currentCode = branch.shaMap.get(filename);
            if (ancestorCode == null || !ancestorCode.equals(currentCode)) {
                changed.add(filename);
            }
        }
        return changed;
    }

    /** Return a list the files that were present in ANCESTOR but
     * aren't in BRANCH. */
    public static List<String> removedFiles(Commit branch, Commit ancestor) {
        List<String> removed = new ArrayList<>();
        for (String filename: ancestor.shaMap.keySet()) {
            String currentCode = branch.shaMap.get(filename);
            if (currentCode == null) {
                removed.add(filename);
            }
        }
        return removed;
    }


    /** Display this commit according to given format. */
    public void display() {
        if (secondParent.equals("")) {
            Utils.message(FORM, _SHA1, datetime(), msg);
        } else {
            Utils.message(FORM_MERGE, shaCode(),
                    parent.substring(0, 7), secondParent.substring(0, 7),
                    datetime(),
                    msg);
        }
    }

    /** Find and display the SHA1 Codes of all the commits with
     * log messages the same as LOG.
     */
    public static void find(String log) {
        boolean change = false;
        List<String> objects = Utils.plainFilenamesIn(Repo.OBJECTS);
        for (String filename: objects) {
            File loc = Utils.join(Repo.OBJECTS, filename);
            Saveable obj = Utils.readObject(loc, Saveable.class);
            if (obj instanceof Commit) {
                if (Commit.matchDisplay((Commit) obj, log)) {
                    change = true;
                }
            }
        }
        if (!change) {
            throw Utils.error("Found no commit with that message");
        }
    }

    /** Display the SHA code of commit CURR if the log message
     * associated with it is equal to LOG.
     * Return true if match, else return false.
     */
    private static boolean matchDisplay(Commit curr, String log) {
        if (curr.msg.equals(log)) {
            Utils.message("%s", curr._SHA1);
            return true;
        }
        return false;
    }

    /** Return string representation of this commit's timer. */
    private String datetime() {
        Date d = new Date(timer * 1000);
        SimpleDateFormat form = new
                SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
        return form.format(d);
    }

    /** Return the SHA1 blob code corresponding to given FILENAME,
     * and null if it does not exist.
     */
    public String getBlobCode(String filename) {
        if (shaMap.containsKey(filename)) {
            return shaMap.get(filename);
        } else {
            return "";
        }
    }

    /** Returns true if the given FILENAME exists in this
     * commit's mapping. */
    public boolean containsFile(String filename) {
        return shaMap.containsKey(filename);
    }

    /** Returns the object associated with the parent's SHA1 string.
     *  Returns null if parent does not exist. */
    public Commit parentObject() {
        if (this.parent.equals("")) {
            return null;
        }
        File parentFile = Utils.join(Repo.OBJECTS,
                this.parent + ".txt");
        return Utils.readObject(parentFile,
                Commit.class);
    }

    /** Returns the object associated with the second parent's SHA1 string.
     *  Returns null if second parent does not exist. */
    public Commit secondParentObject() {
        if (this.secondParent.equals("")) {
            return null;
        }
        File parentFile = Utils.join(Repo.OBJECTS,
                this.secondParent + ".txt");
        return Utils.readObject(parentFile,
                Commit.class);
    }

    /** Return the most recent commit sha code string from
     * the currently active branch. */
    public static String currCommit() {
        String headBranchName = Branch.head();
        return Branch.getBranchCode(headBranchName);
    }

    /** Return the most recent commit from the currently
     * active branch in object format. */
    public static Commit currCommitObj() {
        String id = currCommit();
        return getCommit(id);
    }

    /** Return the commit object associated with ID from repo
     * REMOTEREPO. */
    public static Commit remoteGetCommit(String id, String remoteRepo) {
        File f = Utils.join(remoteRepo, "objects", id + ".txt");
        return Utils.readObject(f, Commit.class);
    }

    /** Return the commit object associated with the given string ID. */
    public static Commit getCommit(String id) {
        if (id.equals("")) {
            return null;
        } else {
            File loc = Utils.join(Repo.OBJECTS, id + ".txt");
            return Utils.readObject(loc, Commit.class);
        }
    }

    /** Reset to the given commit id ID. */
    public static void reset(String id) {
        Commit commit = getCommit(id);
        Commit obj = currCommitObj();
        commit.checkout();
        for (String filename: obj.shaMap.keySet()) {
            if (!commit.containsFile(filename)) {
                if (!Repo.neverConsider(filename)) {
                    Utils.restrictedDelete(filename);
                }
            }
        }
    }

    /** Handles the opportunity to return the corresponding full
     * commit to a possibly partial commit id COMMIT. */
    public static String fullCommit(String commit) {
        int length = commit.length();
        if (length == Utils.UID_LENGTH) {
            return commit;
        }
        List<String> objs = Utils.plainFilenamesIn(".gitlet/objects");
        for (String id: objs) {
            id = id.substring(0, id.length() - 4);
            String shortid = id.substring(0, length);
            if (shortid.equals(commit)) {
                return id;
            }
        }
        return commit;
    }

    /** Check if the the commit associated with ID exists, and throw an error
     * if it does not.
     * */
    public static void checkExists(String id) throws GitletException {
        if (id.equals("")) {
            throw Utils.error("No commit with that id exists.");
        } else {
            File loc = Utils.join(Repo.OBJECTS, id + ".txt");
            if (!loc.exists()) {
                throw Utils.error("No commit with that id exists.");
            }
        }
    }

    /** Check if there is an untracked file, that is not staged or
     * tracked in the current commit, but is changed by commit
     * with string ID.
     */
    public static void checkUntracked(String id)
            throws GitletException {
        assert !id.equals("");
        Commit commit = getCommit(id);
        Commit currCommit = currCommitObj();
        Stage currStage = Stage.savedStage();
        List<String> workingfiles = Utils.plainFilenamesIn(".");
        for (String filename: workingfiles) {
            if (!Repo.neverConsider(filename)) {
                if (!currCommit.containsFile(filename)) {
                    if (currStage == null
                            || !currStage.containsFile(filename)) {
                        if (commit.containsFile(filename)) {
                            throw Utils.error(
                                    "There is an untracked file in the way; "
                                            + "delete it, "
                                            + "or add and commit it first.");
                        }
                    }
                }
            }
        }
    }

    /** Return the Unix EPOCH time of commit creation. */
    public long getTime() {
        return timer;
    }


    /** Return the set of all files associated with this commit. */
    public Set<String> getFileSet() {
        return shaMap.keySet();
    }

    /** Return the code associated with ID in this commit. */
    public String fileCode(String id) {
        return shaMap.get(id);
    }

    /** Returns the SHA1 code of the this commit. */
    public String shaCode() {
        _SHA1 = Utils.sha1(msg, Long.toString(timer),
                parent, secondParent, Utils.serialize(shaMap));
        return _SHA1;
    }

    /** Return the sha1 code associated with this commit. */
    public String sha1() {
        return _SHA1;
    }

    /** The log message associated with this commit. */
    private String msg;

    /** The timestamp of this commit. */
    private long timer;

    /** Saving the SHA1 commit object string of this
     * commit's parent. */
    private String parent;

    /** Saving the SHA1 commit object string of this
     * commit's second parent, if any. Results from merging. */
    private String secondParent;

    /** A mapping from file name to its SHA1 blob object string. */
    private HashMap<String, String> shaMap;

    /** Formatted representation of the non-merged commit. */
    private static final String FORM =
            "===\ncommit %s\nDate: %s\n%s\n";

    /** Formatted representation of a merged commit. */
    private static final String FORM_MERGE =
            "===\ncommit %s\nMerge: %s %s\nDate: %s\n%s\n";

    /** Storing the SHA String Code of the Commit. */
    private String _SHA1;
}
