/*
 * Decompiled with CFR 0.152.
 */
package sakura.kooi.BridgingSkin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static String readFile(File file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        return FileUtils.readFile(fin);
    }

    public static String readFile(InputStream stream) throws IOException {
        int bytesRead;
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        while ((bytesRead = stream.read(buffer)) >= 0) {
            outStream.write(buffer, 0, bytesRead);
        }
        stream.close();
        return outStream.toString();
    }

    public static void copyFile(File from, File to) throws IOException {
        int bytesRead;
        FileInputStream inStream = new FileInputStream(from);
        FileOutputStream outStream = new FileOutputStream(to);
        byte[] buffer = new byte[4096];
        while ((bytesRead = inStream.read(buffer)) >= 0) {
            outStream.write(buffer, 0, bytesRead);
        }
        outStream.flush();
        inStream.close();
        outStream.close();
    }

    public static void writeFile(File file, String data) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(data);
        writer.flush();
        writer.close();
    }

    public static void writeFile(File configFile, InputStream stream) throws IOException {
        FileUtils.writeFile(configFile, FileUtils.readFile(stream));
    }
}

