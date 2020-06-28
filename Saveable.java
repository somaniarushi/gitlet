package gitlet;

import java.io.File;
import java.io.Serializable;

/** An abstract class for objects that are to be saved in the
 * objects directory of the gitlet repo. Can be overwritten to
 * change the location where the file is being saved.
 * @author AMK Somani
 */
public abstract class Saveable implements Serializable {

    /** Returns the shaCode associated with the object. */
    public abstract String shaCode();

    /** Saves the object to objects directory
     * with its respective shacode. */
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
}
