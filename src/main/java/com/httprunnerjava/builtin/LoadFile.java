package com.httprunnerjava.builtin;

import com.httprunnerjava.HttpRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class LoadFile {

    /**
     * 加载
     * @param filePath 文件在resources目录下的相对路径
     * @return
     * @throws IOException
     */
    public static String loadResourcesFileAsString(String filePath) throws IOException {
        StringBuffer fileData = new StringBuffer("");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Objects.requireNonNull(HttpRunner.class.getResourceAsStream(filePath)), StandardCharsets.UTF_8
                ));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }
}
