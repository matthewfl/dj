package testcase.demo;

import edu.berkeley.dj.internal.DJIO;
import edu.berkeley.dj.internal.DJIOTargetMachineArgPosition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by matthewfl
 */
@DJIO
public final class FileWrapper {

    private File file;

    @DJIOTargetMachineArgPosition(1)
    public FileWrapper(int machine, String path) {
        file = new File(path);
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }

    public String[] list() {
        return file.list();
    }

    public String getPath() {
        return file.getPath();
    }

    public byte[] getByteContents() {
        try {
            return Files.readAllBytes(file.toPath());
        } catch(IOException e) {
            return null;
        }
    }

    public String getStringContents() {
        try {

            return new String(Files.readAllBytes(file.toPath()));
        } catch(IOException e) {
            return null;
        }
    }

}
