package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/** A class representing the stage in gitlet, with the option
 * to stage and unstage files.
 * @author AMK Somani
 */
public class Stage extends Saveable implements Serializable  {

    /** A representation of the staging area for files, which files
     * can be staged for addition or removal.
     */
    private Stage() {
        Stage old = savedStage();
        shaMap = (old == null) ? new HashMap<>() : old.shaMap;
        removeList = (old == null) ? new HashMap<>() : old.removeList;
    }

    /** Returns the stage after staging FILENAME for addition. */
    public static Stage stage(String filename) throws GitletException {
        Stage s = new Stage();
        Blob blob = s.parseFile(filename);
        s.stageAddition(blob);
        s.saveFile();
        return s;
    }

    /** Returns the stage after staging FILENAME for removal. */
    public static Stage antistage(String filename) throws GitletException {
        Stage s = new Stage();
        s.stageRemoval(filename);
        s.saveFile();
        return s;
    }

    /** Checks conditions for addition of blob B to the
     * stage, and adds and removes it from the stage object
     * as necessary. */
    private void stageAddition(Blob b) {
        Commit lastCommit = Commit.currCommitObj();
        if (lastCommit.getBlobCode(b.filename()).equals(b.shaCode())) {
            if (shaMap.containsKey(b.filename())) {
                shaMap.remove(b.filename());
            }
        } else {
            if (!shaMap.containsKey(b.filename())) {
                shaMap.put(b.filename(), b.shaCode());
            }
        }
        if (removeList.containsKey(b.filename())) {
            removeList.remove(b.filename());
        }
    }

    /** Stage the removal of file FILENAME. */
    private void stageRemoval(String filename) {
        boolean change = false;
        if (shaMap.containsKey(filename)) {
            shaMap.remove(filename);
            change = true;
        } else {
            Commit curr = Commit.currCommitObj();
            if (curr.containsFile(filename)) {
                removeList.put(filename, filename);
                Utils.restrictedDelete(filename);
                change = true;
            }
        }
        if (!change) {
            throw Utils.error("No reason to remove the file.");
        }
    }

    /** Removes the file FILENAME from the stage. */
    public static void removeFromStage(String filename) {
        Stage s = savedStage();
        if (s == null) {
            return;
        }
        if (s.shaMap.containsKey(filename)) {
            s.shaMap.remove(filename);
        }
        s.saveFile();
    }

    /** Returns the previously saved stage from the index file in the
     * gitlet repo. */
    public static Stage savedStage() {
        String s = Utils.readContentsAsString(Repo.INDEX).trim();
        if (s.equals("")) {
            return null;
        }
        return Utils.readObject(Repo.INDEX, Stage.class);
    }

    /** Clears the currently saved stage. */
    public static void clearStage() {
        Utils.writeContents(Repo.INDEX, "");
    }

    /** Returns string of the files of the stage as per status call. */
    public static String stageDisplay() {
        Stage s = savedStage();
        String disp = "";
        if (s == null) {
            return disp;
        } else {
            for (String code: s.shaMap.keySet()) {
                disp += code + "\n";
            }
        }
        return disp;
    }

    /** Returns a string of all the removed files as per status call. */
    public static String removeDisplay() {
        Stage s = savedStage();
        String disp = "";
        if (s == null) {
            return disp;
        } else {
            for (String code: s.removeList.keySet()) {
                disp += code + "\n";
            }
        }
        return disp;
    }

    /** Returns the blob associated with the given file.
     * Throws error if the FILENAME
     * does not exist in the directory. */
    private Blob parseFile(String filename) throws GitletException {
        checkFileExistence(filename);
        Blob b = Blob.create(filename);
        return b;
    }

    /** Checks if the file FILENAME exists in user
     * directory, and throws an error
     * if it does not. */
    public void checkFileExistence(String filename) throws GitletException {
        List<String> filelist = Utils.plainFilenamesIn(".");
        if (!filelist.contains(filename)) {
            throw Utils.error("File does not exist.");
        }
    }

    /** Check if anything is currently staged, and
     * throw an error if it is. */
    public static void checkStaged() throws GitletException {
        Stage s = savedStage();
        if (s != null) {
            if (!s.shaMap.isEmpty() || !s.removeList.isEmpty()) {
                throw Utils.error("You have uncommitted changes.");
            }
        }
    }

    /** Returns true if this stage contains
     * the file FILENAME. */
    public boolean containsFile(String filename) {
        return shaMap.containsKey(filename);
    }

    /** Returns true if this stage contains the file FILENAME,
     * both add and delete. */
    public boolean contains(String filename) {
        return shaMap.containsKey(filename)
                || removeList.containsKey(filename);
    }

    /** Returns the unique SHA1 String code for the stage object. */
    public String shaCode() {
        return Utils.sha1(Utils.serialize(shaMap),
                Utils.serialize(removeList));
    }

    @Override
    public void saveCode(String code) {
        assert Repo.INDEX.exists();
        Utils.writeObject(Repo.INDEX, this);
    }

    /** Returns the shamap associated with this stage. */
    public HashMap<String, String> shaMap() {
        return shaMap;
    }

    /** returns the removelist associated with this stage. */
    public HashMap<String, String> removeList() {
        return removeList;
    }

    /** Maintains a mapping from filename to SHA1
     * code of its corresponding blob
     * for files to be added. */
    private HashMap<String, String> shaMap;

    /** Maintains a mapping from filename to SHA1
     * code of its corresponding blob
     * for files to be removed. */
    private HashMap<String, String> removeList;
}
