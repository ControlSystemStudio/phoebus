package org.phoebus.applications.uxanalytics.monitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {

    public static final String GIT_METADATA_DIR = ".git";
    public static final String SVN_METADATA_DIR = ".svn";
    public static final String HG_METADATA_DIR = ".hg";

    private static boolean isGitRepo(File fileObject){
        return isRepo(fileObject, GIT_METADATA_DIR);
    }

    private static boolean isSVNRepo(File fileObject){
        return isRepo(fileObject, SVN_METADATA_DIR);
    }

    private static boolean isMercurialRepo(File fileObject){
        return isRepo(fileObject, HG_METADATA_DIR);
    }

    private static boolean isRepo(File fileObject, String repoType){
       File file = new File(fileObject.getAbsolutePath() + File.separator + repoType);
         return file.exists() && file.isDirectory();
    }

    public static boolean isSourceRoot(File fileObject){
        return isGitRepo(fileObject) || isSVNRepo(fileObject) || isMercurialRepo(fileObject);
    }

    public static File findSourceRootOf(File fileObject) {
        File directory = fileObject.getParentFile();
        while (directory != null) {
            if (isSourceRoot(directory)) {
                return directory;
            }
            directory = directory.getParentFile();
        }
        return null;
    }

    //re-implement here with Java MessageDigest, so we don't need to depend on elog
    private static String getFileSHA256(File fileObject){
        try(InputStream fis = new FileInputStream(fileObject)){
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] byteBuffer = new byte[1024];
            int bytesCount = 0;

            while((bytesCount = fis.read(byteBuffer)) != -1){
                digest.update(byteBuffer, 0, bytesCount);
            }

            byte[] bytes = digest.digest();

            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++){
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();

        } catch(IOException | NoSuchAlgorithmException e){;
            throw new RuntimeException(e);
        }
    }

}
