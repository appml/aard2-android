package itkach.aard2;

import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DictionaryFinder {

    private final static String T = "DictionaryFinder";

    private Set<String>         excludedScanDirs   = new HashSet<String>() {
        {
            add("/proc");
            add("/dev");
            add("/etc");
            add("/sys");
            add("/acct");
            add("/cache");
        }
    };

    private FilenameFilter fileFilter = new FilenameFilter() {
        public boolean accept(File dir, String filename) {
            return filename.toLowerCase().endsWith(
                    ".slob") || new File(dir, filename).isDirectory();
        }
    };

    private List<File> discover() {
        File scanRoot = new File("/");
        List<File> result = new ArrayList<File>();
        result.addAll(scanDir(scanRoot));
        return result;
    }

    private List<File> scanDir(File dir) {
        String absolutePath = dir.getAbsolutePath();
        if (excludedScanDirs.contains(absolutePath)) {
            Log.d(T, String.format("%s is excluded", absolutePath));
            return Collections.emptyList();
        }
        boolean symlink = false;
        try {
            symlink = isSymlink(dir);
        } catch (IOException e) {
            Log.e(T,
                    String.format("Failed to check if %s is symlink",
                            dir.getAbsolutePath()));
        }

        if (symlink) {
            Log.d(T, String.format("%s is a symlink", absolutePath));
            return Collections.emptyList();
        }

        if (dir.isHidden()) {
            Log.d(T, String.format("%s is hidden", absolutePath));
            return Collections.emptyList();
        }
        Log.d(T, "Scanning " + absolutePath);
        List<File> candidates = new ArrayList<File>();
        File[] files = dir.listFiles(fileFilter);
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    candidates.addAll(scanDir(file));
                } else {
                    if (!file.isHidden() && file.isFile()) {
                        candidates.add(file);
                    }
                }
            }
        }
        return candidates;
    }

    private static boolean isSymlink(File file) throws IOException {
        File fileInCanonicalDir = null;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }
        if (fileInCanonicalDir.getCanonicalFile().equals(
                fileInCanonicalDir.getAbsoluteFile())) {
            return false;
        } else {
            return true;
        }
    }

    synchronized List<SlobDescriptor> findDictionaries() {
        Log.d(T, "starting dictionary discovery");
        long t0 = System.currentTimeMillis();
        List<File> candidates = discover();
        Log.d(T, "dictionary discovery took " + (System.currentTimeMillis() - t0));
        List<SlobDescriptor> descriptors = new ArrayList<SlobDescriptor>();
        for (File f : candidates) {
            descriptors.add(SlobDescriptor.fromFile(f));
        }
        return descriptors;
    }
}
