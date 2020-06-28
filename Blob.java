package gitlet;

import java.io.Serializable;
import java.io.File;

/** A file representing the blob that wraps a file in gitlet.
 * @author AMK Somani
 * */
public class Blob extends Saveable implements Serializable {

    /** Represents a blob object wrapping around a file named STR. */
    private Blob(String str) {
        this.filename = str;
        this.file = Utils.join(str);
    }

    /** Return, after creating and saving, a file FILENAME. */
    public static Blob create(String filename) {
        Blob b = new Blob(filename);
        b.serializeFile();
        b.saveFile();
        return b;
    }

    /** Return after creating a blob of file FILENAME
     *  without saving it to the gitlet repo,
     * for comparison purposes. */
    public static Blob checker(String filename) {
        Blob b = new Blob(filename);
        b.serializeFile();
        return b;
    }

    /** Serializing a file and storing it in local variable serial. */
    void serializeFile() {
        serial = Utils.readContents(file);
        contents = Utils.readContentsAsString(file);
    }

    /** Returns true if the file contained in this blob exists
     * in the git repo. */
    public boolean exists() {
        return file.exists();
    }

    /** Return the blob associated in the given sha1 SHACODE in the
     * gitlet objects repo. */
    public static Blob get(String shaCode) {
        File file = Utils.join(Repo.OBJECTS, shaCode + ".txt");
        if (!file.exists()) {
            return null;
        }
        Blob b = Utils.readObject(file, Blob.class);
        return b;
    }

    /** Returns the unique SHA1 String code for the blob object. */
    public String shaCode() {
        return Utils.sha1(serial, filename);
    }


    /** Saves the object to objects directory with its respective shacode. */
    public void saveFile() {
        saveCode(shaCode());
    }

    /** Save a file of the name CODE in the objects directory
     * of the gitlet repo.
     * Assumes that the object directory exists.
     */
    public void saveCode(String code) {
        assert Repo.OBJECTS.exists();
        File obj = Utils.join(Repo.OBJECTS, code + ".txt");
        if (!obj.exists()) {
            Repo.createFile(obj);
        }
        Utils.writeObject(obj, this);
    }

    /** Returns the file stored in the blob. */
    public File file() {
        return file;
    }

    /** Returns the filename associated with this blob. */
    public String filename() {
        return filename;
    }

    /** Returns the contents of the file stored in the blob. */
    public String contents() {
        return contents;
    }

    /** The name of the blob's file name. */
    private String filename;

    /** File object associated with this blob. */
    private File file;

    /** Serialized representation of the file. */
    private byte[] serial;

    /** The contents of a file. */
    private String contents;
}
