/*
 * Copyright 2015-2016 USEF Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package energy.usef.environment.tool.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class FileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    public static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private FileUtil() {
        // private constructor
    }

    public static String getCurrentFolder() {
        return System.getProperty("user.dir");
    }

    /**
     * Creates all folders
     *
     * @param folderName
     */
    public static void createFolders(String folderName) {
        LOGGER.debug("Creating folder: " + folderName);
        File dir = new File(folderName);
        dir.mkdirs();
    }

    public static void removeFolders(String folderName) throws IOException {
        if (folderName == null || folderName.isEmpty() || folderName.trim().equals("\\") || folderName.trim().equals("/")) {
            LOGGER.warn("Prevent to delete folders recursively from the root location.");
        } else {
            Path directory = Paths.get(folderName);
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    LOGGER.debug("Removing folder " + dir);
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    public static void removeFile(String fileName, boolean failOnNotExists) {
        try {
            Files.delete(Paths.get(fileName));
        } catch (IOException e) {
            if (failOnNotExists) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    public static boolean isFolderExists(String folderName) {
        File f = new File(folderName);
        return f.exists() && f.isDirectory();
    }

    public static boolean isFileExists(String fileName) {
        File f = new File(fileName);
        return f.exists() && !f.isDirectory();
    }

    public static void writeTextToFile(String filename, String data) throws IOException {
        FileUtils.writeStringToFile(new File(filename), data, DEFAULT_CHARSET);
    }

    public static void appendTextToFile(String filename, String data) throws IOException {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))) {
            out.println(data);
        }
    }

    public static void appendNonDelimitingTextToFile(String filename, String data) throws IOException {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))) {
            out.print(data);
        }
    }

    public static List<String> readLines(String filename) throws IOException {
        String line;
        List<String> list = new ArrayList<>();
        try (FileInputStream input = new FileInputStream(filename);
                BufferedReader br = new BufferedReader(new InputStreamReader(input, DEFAULT_CHARSET))) {
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        }
        return list;
    }

    public static String readFile(String filename) throws IOException {
        File file = new File(filename);
        return org.apache.commons.io.FileUtils.readFileToString(file);
    }

    public static void copyFile(String fromFilename, String toFilename) throws IOException {
        if (!isFileExists(fromFilename)) {
            LOGGER.error("File {} can not be found and therefor can not be copied to {}.", fromFilename, toFilename);
            throw new RuntimeException("File " + fromFilename + " can not be found and therefor can not be copied to "
                    + toFilename + ".");
        }
        LOGGER.info("Copy file from {} to {}.", fromFilename, toFilename);
        File fromFile = new File(fromFilename);
        File toFile = new File(toFilename);
        FileUtils.copyFile(fromFile, toFile);
    }

    public static void copyFolder(String fromFolder, String toFolder) throws IOException {
        if (!isFolderExists(fromFolder)) {
            LOGGER.error("Folder {} can not be found and therefor can not be copied to {}.", fromFolder, toFolder);
            throw new RuntimeException("Folder " + fromFolder + " can not be found and therefor can not be copied to "
                    + toFolder + ".");
        }

        LOGGER.info("Copy folder from {} to {}.", fromFolder, toFolder);
        File src = new File(fromFolder);
        File dest = new File(toFolder);
        FileUtils.copyDirectory(src, dest);
    }

    public static Properties readProperties(String configFilename) throws IOException {
        LOGGER.info("Reading properties file: {}", configFilename);
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(configFilename);) {
            properties.load(input);

            StringBuilder sb = new StringBuilder("Properties read from " + configFilename + ":\n");
            for (Entry<Object, Object> entry : properties.entrySet()) {
                sb.append(entry.getKey() + "=" + entry.getValue() + "\n");
            }
            LOGGER.debug(sb.toString());
        }
        return properties;
    }

    public static Properties writeProperties(String configFilename, String comment, Properties properties) throws IOException {
        LOGGER.info("Writing properties file: {}", configFilename);
        try (FileOutputStream output = new FileOutputStream(configFilename);) {
            properties.store(output, comment);
        }
        return properties;
    }

    @SuppressWarnings("rawtypes")
    public static Map loadYaml(String configFilename) throws FileNotFoundException, IOException {
        LOGGER.info("Reading YAML configuration from: " + configFilename);
        Map map = null;
        try (FileInputStream input = new FileInputStream(configFilename);) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);

            map = (Map) yaml.load(input);
            LOGGER.debug("\n--------------------------------------------------------------------" +
                    "\nConfiguration from file: {}:\n{}--------------------------------------------------------------------",
                    configFilename, yaml.dump(map));
        }
        return map;
    }
}
