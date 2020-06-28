# Classes And Data Structures

## Commit

This class stores the data associated with each commit made.

**Fields:** Log message, Time of creation, A mapping of file names to blobs associated, Parent, Second parent

## Blob

This class encapsulates a file.

**Fields:** Name of file encapsulated, File encapsulated.

## Tag

A pointer to a certain commit, with a certain name.

**Fields:** name, commit ID associated, isActive.

## Stage

An object representing the staging area of a repository.

**Fields:** A mapping of file names to associated blobs. A list of all files staged for removal.

---

# Algorithms

## Main

- `initialize()`: Get .gitlet repo, with subdirs commits, tags, staging, files
- `handleAdd()`: Check if file exists in working directory, and abort if it doesn't. Send to 
(new Stage( )).addFile(<file name>).
- `handleCommit()`: Checks for edge cases in spec. Else, commit ( ).
- `handleRemoval()`: Handles failures. stageRemoval(<file name>)

## Commit

- `commit()`: create an empty "initial commit" and save that to .gitlet/commits
- `commit(message)`: makeCommit( )
- `makeCommit()`: Deserialise most recent commit. Make the parent of this commit and this mapping the parent's mapping. Deserialise the staging object. Go through its mapping and update whichever name is necessary in current object. If the file didn't exist in this's mapping, add it. For every file in the stage object's remove list, remove it from the commit's mapping. For every file in the stage object mapping, move the file .gitlet/staging to .gitlet/files. clearStage(). Serialize this object and save it in .gitlet/commits. updateTag().
- `getCurrCommit()`: Look in active.txt in .gitlet/tags to get <name> –– get the commit ID mentioned in <name>.txt in .gitlet/tags. Get corresponding commit byte file in .gitlet/commits. Deserialize and return Commit object.
- `displayLog()`: getCurrCommit(). Display currCommit, with additional merge statement when both parents ≠ null. Recursive with curr's parent, til parent is null.
- `displayGlobalLog()`: getCurrCommit(). Display currCommit, with additional merge statement when both parents ≠ null. Recursive with curr's parents, til parent is null.
- `find()`: Use the process of global-log to go through the commits. For each commit, check if the message matches –– if it does, display the ID, and turn a local variable foundCommit to true. If foundCommit is false at the end of the recursion, throw up error message.
- `checkout(<commit id>, <file name>)`: If no such commit ID exists, abort. If the file does not exist, abort. Get the corresponding commit file from .gitlet/files. Overwrite the file in the working directory with this file. Create a new stage object. If the previous version of the given file was staged, remove it from the stage object's mapping ("unstage"). Update `stage.txt` .
- `checkout(<branch name>)`: Check if stageEmpty() –– if not, abort. Deserialize `branchlist.txt` and get a list of all the tags available. If the <branch name> is not in the tags, abort. Get current branchname from `active.txt`. If <branch name> is active branch, then abort. Else, make `active.txt`  the given branch

## Blob

- `equals()`

## Stage

- `addFile(<file name>)`: getCurrCommit( ). getCurrStage( ). UpdateMap(Curr Stage's Map). Search for <file name> is curr commit's blobmap. If not found or if found and the blobs are not equal, create a blob of <file name>, and stageAddition(<file name>, <blob>). If the blobs are equal, unstage(<file name>). Finally, updateStage().
- `updateMap(<Map>)`: Update this's map to be <Map>.
- `stageAddition(<file name>, <blob>)`: Add <file name> to map if it doesn't exist, otherwise overwrite with <blob>. StageFile(<file name>).
- `stageRemoval(<file name>)`: getCurrStage( ). updateMap(<curr map>). If the given file exists in the stage object's mapping, remove it and remove its corresponding file from .gitlet/staging. Save a serialized version of current stage object in `stage.txt`. Else, getCurrCommit( ). If the file exists in this commit, then add the file name to the list of remove files in the stage object. Serialize this object as `stage.txt` and save it in .gitlet/staging. If neither of the above are true, abort with message.
- `stageFile(<file name>)`: Add file attached to <file name> to .gitlet/staging.
- `unstage(<file name>)`: If <file name> exists in map, remove it from map. If <file name> exists in removal list, remove it from list. Else, no nothing.
- `static getCurrStage()`: If `stage.txt` exists in .gitlet/staging, deserialize and return. Else, return null.
- `updateStage()`: Rewrite/write `stage.txt` to be this object, serialized.
- `stageEmpty()`: Returns true if `stage.txt` does not exist in .gitlet/staging else false.
- `clear()`: Deletes `stage.txt`

## Tag

- `tag([commit id], active)`: create the initial tag with the name "master". If active, makeActive. Add to branchlist listBranch. Save in .gitlet/tags.
- `makeActive()`: Create a file `active.txt` in .gitlet/tags if it doesn't exist, otherwise update it to save the this.name.
- `listBranch()`: Add this tag's name to `branchlist.txt` in .gitlet/tags.
- `updateTag(<commit id>)`: Update current tag.
- `status()`: Go through `branchlist.txt` and display all branches as per branches. If branch.isActive, display with asterisk. Deserialize the object in `stage.txt`, and go through it's mapping keys. Display them as the staged files. Display its remove list as removed files. Display Mods not staged for commit and untracked files headers.

---

# Persistence

- `index.txt`: The staging area.
- `objects`: All the serialized objects, including blobs and commits, as *Sha1Code*.txt.
- `head.txt:` Saves the name of the currently active branch.
- `branches`: Directory that contains files as *branchname*.txt, with each file containing the SHA 1 code to a commit.

---