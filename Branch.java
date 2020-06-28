package gitlet;

import java.io.File;
import java.util.List;
import java.time.Instant;

/** An object that represents a branch in the gitlet tree.
 * @author AMK Somani
 * */
public class Branch {

    /** A class that represents the branches of a gitlet repo,
     * such that each branch has a name TITLE and may or may not be the
     * head (currently active) branch.
     */
    public Branch(String title) {
        this.name = title;
        makeBranchInDir();
    }

    /** Makes this branch the currently active branch.
     * Assumes that the head file exists in the gitlet repo*/
    public void setHead() {
        assert Repo.HEAD.exists();
        Utils.writeContents(Repo.HEAD, name);
    }

    /** Add this branch to the gitlet directory if it does not
     * exist there already. Save in this branch the SHA1 String
     * code for the branches' most recent commit.
     */
    public void makeBranchInDir() {
        File branch = Utils.join(Repo.BRANCHES, name + ".txt");
        if (branch.exists()) {
            return;
        }
        Repo.createFile(branch);
        updateBranch();
    }

    /** Assign the contents of the file associated with this branch as
     * the contents of the most recent commit on the head branch.
     */
    public void updateBranch() {
        String commit = Commit.currCommit();
        updateBranch(commit);
    }

    /** Update the contents of the file associated with this branch as
     * the SHA1 String code COMMIT.
     * Assume that the branch exists in the repository.
     */
    public void updateBranch(String commit) {
        updateBranch(this.name, commit);
    }

    /** Sets the contents of the branch file with the name NAME
     * to the SHA1 code String COMMIT.
     */
    public static void updateBranch(String name, String commit) {
        File branch = Utils.join(Repo.BRANCHES,
                name + ".txt");
        assert branch.exists();
        if (!commit.equals("")) {
            Utils.writeContents(branch, commit);
        }
    }

    /** Merge the given branch NAME with the current head branch. */
    public static void merge(String name) {
        merge(head(), name);
    }

    /** Merge the branch names CURRENT into GIVEN. */
    public static void merge(String current, String given)
            throws GitletException {
        Commit ancestor = Commit.findCommonAncestor(current, given);
        if (ancestor == null) {
            return;
        }
        Commit currentCommit = Commit.getCommit(getBranchCode(current));
        Commit givenCommit = Commit.getCommit(getBranchCode(given));
        List<String> changedCurrent =
                Commit.changedFiles(currentCommit, ancestor);
        List<String> changedGiven = Commit.changedFiles(givenCommit, ancestor);
        List<String> removedGiven =
                Commit.removedFiles(givenCommit, ancestor);
        List<String> removedCurrent =
                Commit.removedFiles(currentCommit, ancestor);
        for (String filename: changedGiven) {
            if (!changedCurrent.contains(filename)) {
                if (!ancestor.containsFile(filename)) {
                    if (!currentCommit.containsFile(filename)) {
                        givenCommit.checkout(filename);
                        Stage.stage(filename);
                    }
                } else {
                    currentCommit.checkout(filename);
                    Stage.stage(filename);
                }
            }
        }
        for (String filename: removedGiven) {
            if (!changedCurrent.contains(filename)
                    && !removedCurrent.contains(filename)) {
                Stage.antistage(filename);
            }
        }
        for (String filename: changedGiven) {
            if (changedCurrent.contains(filename)) {
                if (!givenCommit.fileCode(filename).equals(
                        currentCommit.fileCode(filename))) {
                    handleMergeConflict(currentCommit.fileCode(filename),
                            givenCommit.fileCode(filename), filename);
                }
            }
            if (removedCurrent.contains(filename)) {
                handleMergeConflict(currentCommit.fileCode(filename),
                        givenCommit.fileCode(filename), filename);
            }
        }
        for (String filename: changedCurrent) {
            if (removedGiven.contains(filename)) {
                handleMergeConflict(currentCommit.fileCode(filename),
                        givenCommit.fileCode(filename), filename);
            } else if (!changedGiven.contains(filename)) {
                currentCommit.checkout(filename);
                Stage.stage(filename);
            }
        }
        Commit c = Commit.create("Merged "
                        + given + " into " + current + ".",
                Instant.now().getEpochSecond(), getBranchCode(given));
        updateBranch(current, c.sha1());
    }

    /** Handles a merge conflict for FILENAME for CODECURRENT and
     * CODEGIVEN while merging. */
    public static void handleMergeConflict(String codeCurrent,
                                           String codeGiven, String filename) {
        Blob currentFile = Blob.get(codeCurrent);
        Blob givenFile = Blob.get(codeGiven);
        String newValue = "<<<<<<< HEAD\n";
        newValue += (currentFile != null && currentFile.exists())
                ? currentFile.contents() : "";
        newValue += "=======\n";
        newValue += (givenFile != null && givenFile.exists())
                ? givenFile.contents() : "";
        newValue += ">>>>>>>\n";
        File write = Utils.join("", filename);
        if (!write.exists()) {
            Repo.createFile(write);
        }
        Utils.writeContents(write, newValue);
        Stage.stage(filename);
        System.out.println("Encountered a merge conflict.");
    }


    /** Delete a branch NAME assuming that it exists. */
    public static void delete(String name) {
        File branch = Utils.join(Repo.BRANCHES,
                name + ".txt");
        assert branch.exists();
        branch.delete();
    }

    /** Return the display string of all the branches
     * as per the status request. */
    public static String display() {
        List<String> branchNames = Utils.plainFilenamesIn(
                ".gitlet/branches");
        String active = head();
        String result = "";
        for (String branch: branchNames) {
            branch = branch.substring(0, branch.length() - 4);
            if (branch.equals(active)) {
                result += "*" + branch + "\n";
            } else {
                result += branch + "\n";
            }
        }
        return result;
    }

    /** Return the name of the currently active branch. Assume that
     * the head file in the github repository exists. */
    public static String head() {
        assert Repo.HEAD.exists();
        String name = Utils.readContentsAsString(Repo.HEAD);
        return name.trim();
    }

    /** Set the head branch to be the branch NAME. */
    public static void setHeadBranch(String name) {
        assert Repo.HEAD.exists();
        Utils.writeContents(Repo.HEAD, name);
    }

    /** Checkout a given branch with name NAME. */
    public static void checkout(String name) {
        String branchCode = Branch.getBranchCode(name);
        Commit branchCommit = Commit.getCommit(branchCode);
        branchCommit.checkout();
        List<String> workingfiles = Utils.plainFilenamesIn(".");
        for (String filename: workingfiles) {
            if (!branchCommit.containsFile(filename)) {
                if (!Repo.neverConsider(filename)) {
                    Utils.restrictedDelete(filename);
                }
            }
        }
    }

    /** Check if a branch with NAME already exists,
     * and throw an error if it does. */
    public static void checkDouble(String name)
            throws GitletException {
        File branch = Utils.join(Repo.BRANCHES, name + ".txt");
        if (branch.exists()) {
            throw Utils.error("A branch with that name already exists.");
        }
    }

    /** Check if a branch NAME exists, and throw an error if it does not. */
    public static void checkExists(String name)
            throws GitletException {
        File branch = Utils.join(Repo.BRANCHES, name + ".txt");
        if (!branch.exists()) {
            throw Utils.error("No such branch exists.");
        }
    }

    /** Check if a branch NAME exists, and throw an error if it does not. */
    public static void checkExistsRm(String name)
            throws GitletException {
        File branch = Utils.join(Repo.BRANCHES, name + ".txt");
        if (!branch.exists()) {
            throw Utils.error(" A branch with that name does not exist.");
        }
    }

    /** Check if the branch NAME is currently active, and throw
     * an error if it is. */
    public static void checkIsActiveRm(String name)
            throws GitletException {
        if (head().equals(name)) {
            throw Utils.error("Cannot remove the current branch.");
        }
    }

    /** Return the head of the branch NAME from remote REPO linked by file F
     * or null if the branch does not exist. */
    public static Commit getRemoteHead(String name, File f) {
        String path = Utils.readContentsAsString(f);
        File branchFile = Utils.join(path, "branches", name + ".txt");
        if (!branchFile.exists()) {
            return null;
        } else {
            return Commit.remoteGetCommit(
                    Utils.readContentsAsString(branchFile), path);
        }
    }

    /** Set the head of the remote REPO linked by file F's BRANCHNAME
     * to String ID. */
    public static void setRemoteHead(File repo, String branchname, String iD) {
        String path = Utils.readContentsAsString(repo);
        File branchFile = Utils.join(path, "branches", branchname + ".txt");
        Utils.writeContents(branchFile, iD);
    }

    /** Check if the branch NAME is currently active, and throw an
     * error if it is. */
    public static void checkIsActive(String name) throws GitletException {
        if (head().equals(name)) {
            throw Utils.error("No need to checkout the current branch.");
        }
    }

    /** Check if there is an untracked file in the way for branch NAME. */
    public static void checkUntracked(String name) throws GitletException {
        String branchCode = Branch.getBranchCode(name);
        assert !branchCode.equals("");
        Commit branchCommit = Commit.getCommit(branchCode);
        Commit lastCommit = Commit.currCommitObj();
        Stage currStage = Stage.savedStage();
        List<String> workingfiles = Utils.plainFilenamesIn(".");
        for (String filename: workingfiles) {
            if (!Repo.neverConsider(filename)) {
                if (!lastCommit.containsFile(filename)) {
                    if (currStage == null
                            || !currStage.containsFile(filename)) {
                        if (branchCommit.containsFile(filename)) {
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

    /** Return the most recent commit SHA code associated
     * with a branch named BRANCH. */
    public static String getBranchCode(String branch) {
        assert Repo.BRANCHES.exists();
        if (branch.equals("")) {
            return "";
        }
        File file = Utils.join(Repo.BRANCHES, branch + ".txt");
        return Utils.readContentsAsString(file);
    }

    /** The name of this branch. */
    private String name;
}
