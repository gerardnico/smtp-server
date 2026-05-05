package com.combostrap.common;



import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@SuppressWarnings("UnusedReturnValue")
public class Fs {


    /**
     * A safe method even if the string is not a path
     *
     * @param s - a string to test if s is a path to a valid file
     * @return true if this a regular file
     */
    public static boolean isFile(String s) {
        return isFile(Paths.get(s));
    }

    public static boolean isFile(Path s) {
        try {
            return Files.isRegularFile(s);
        } catch (InvalidPathException e) {
            return false;
        }
    }


    /**
     * Safe is directory method even if the string is not a path
     *
     * @param s - a string path
     * @return true if the string path represents a directory
     */
    public static boolean isDirectory(String s) {
        try {
            return Files.isDirectory(Paths.get(s));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /**
     * An alias to the function {@link Files#write(Path, byte[], OpenOption...)}
     * without any option.
     *
     * @param s    - the string to write
     * @param path - the path to the file to write
     */
    public static void toFile(String s, Path path) {
        try {
            Files.write(path, s.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Same as Files.walk(Paths.get(path))
     *
     * @param path - the start path to scan
     * @return - the children of a directory or the file if it's a file
     */
    public static List<Path> getDescendantFiles(Path path) {

        // Path to return
        List<Path> pathsCollector = new ArrayList<>();
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {

                for (Path childPath : dirStream) {
                    if (Files.isRegularFile(childPath)) {
                        pathsCollector.add(childPath);
                    } else {
                        addChildFiles(pathsCollector, childPath);
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            pathsCollector.add(path);
        }
        return pathsCollector;

    }

    /**
     * Recursive function usd by {@link #getDescendantFiles(Path)}
     *
     * @param path           - a directory or file path where to start from
     * @param pathsCollector - the object that collects the path
     */
    static void addChildFiles(List<Path> pathsCollector, Path path) {

        if (Files.isDirectory(path)) {

            try (DirectoryStream<Path> childrenFiles = Files.newDirectoryStream(path)) {

                for (Path childPath : childrenFiles) {
                    if (Files.isRegularFile(childPath)) {
                        pathsCollector.add(childPath);
                    } else {
                        addChildFiles(pathsCollector, childPath);
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            pathsCollector.add(path);
        }

    }

    /**
     * @param content - the file content
     * @return a path to a txt file with the string as content
     */
    public static Path createTempFileWithContent(String content) {
        return createTempFileWithContent(content, ".txt");
    }

    /**
     * @param content -  the content of the file
     * @param suffix  - the file suffix. Example: ".txt"
     * @return a temp file in the temp directory
     */
    public static Path createTempFileWithContent(String content, String suffix) {

        try {

            Path tempFile = createTempFile(suffix);
            Files.write(tempFile, content.getBytes());

            return tempFile;

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    public static Path createTempFile(String suffix) {
        Path temp = createTempDirectory(null);

        try {
            return Files.createTempFile(temp, null, suffix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @param prefix - a prefix to generate the directory name (may be null)
     * @return a temp directory
     */
    public static Path createTempDirectory(String prefix) {

        try {

            return Files.createTempDirectory(prefix);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }





    public static Path getUserHome() {
        final String home = System.getProperty("user.home");
        return Paths.get(home);
    }

    @SuppressWarnings("unused")
    private static String getPathSeparator() {
        return FileSystems.getDefault().getSeparator();
    }

    /**
     * @return the system (process) encoding
     * ie the value of the system property file.encoding
     */
    @SuppressWarnings("unused")
    private static String getSystemEncoding() {
        return Charset.defaultCharset().displayName();
    }

    @SuppressWarnings("unused")
    private static Path getTempDir() {
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }


    /**
     * Create a file and all subdirectories if needed
     * It fails if the file exists
     *
     * @param path the file to create
     */
    public static void createEmptyFile(Path path) {
        try {
            Path parent = path.getParent();
            Files.createDirectories(parent);
            Files.createFile(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create an empty file if not exists
     *
     * @param path the file to create
     */
    public static void createEmptyFileIfNotExist(Path path) {

        if (Files.exists(path)) {
            return;
        }
        Fs.createEmptyFile(path);

    }

    /**
     * Wrapper around {@link Files#write(Path, byte[], OpenOption...)}
     * to write a string to a file in UTF8
     * without exception handling
     * and with parent directory creation if not exists
     *
     * @param path - the path
     * @param s    - the content to add to the path
     */
    public static void write(Path path, String s) {
        try {
            Fs.createDirectoryIfNotExists(path.getParent());
            Files.write(path, s.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A wrapper around path.relativize(base)
     *
     * @param path - the path to relativize
     * @param base - the base path
     * @return a relative path
     */
    public static Path relativize(Path path, Path base) {
        return base.relativize(path);
    }

    public static void overwrite(Path source, Path target) {
        try {
            Files.write(target, Files.readAllBytes(source), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param path     - a regular file or a directory
     * @param basePath - a base directory
     * @return a list of name between the path and the base path
     * <p>
     * Example
     * * basePath = /tmp
     * * path = /tmp/foo/bar/blue
     * You will get { 'foo', 'bar'}
     */
    public static List<String> getDirectoryNamesInBetween(Path path, Path basePath) {

        if (basePath.getNameCount() > path.getNameCount()) {
            throw new RuntimeException("The base path should have less levels than the path");
        }
        List<String> names = new ArrayList<>();
        // -1 in the end limit because the last name if the file name, we don't return it
        for (int i = 0; i < path.getNameCount() - 1; i++) {
            String name = path.getName(i).toString();
            if (i <= basePath.getNameCount() - 1) {
                String baseName = basePath.getName(i).toString();
                if (!baseName.equals(name)) {
                    throw new RuntimeException("The path doesn't share a common branch with the base path. At the level (" + (i + 1) + ", the name is different. We got (" + name + ") for the path and (" + baseName + ") for the base path");
                }
            } else {
                names.add(name);
            }
        }
        return names;
    }


    /**
     * path.isAbsolute just tell you that the object path is absolute, not the path
     * <p>
     * This method check if there is a root
     * If this is the case, the path is absolute otherwise not.
     *
     * @param path the path
     * @return true if the path has a root (is absolute)
     */
    public static boolean isAbsolute(Path path) {
        return path.getRoot() != null;
    }


    /**
     * @param path - the directory path
     * @return the children path of a directory
     */
    public static List<Path> getChildrenFiles(Path path) {

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
            List<Path> childrenPaths = new ArrayList<>();
            for (Path childPath : paths) {
                childrenPaths.add(childPath);
            }
            return childrenPaths;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param parts - the parts of the path name
     * @return a local pah from an array string
     */
    public static Path getPath(String[] parts) {
        String[] more = Arrays.copyOfRange(parts, 1, parts.length);
        return Paths.get(parts[0], more);
    }

    /**
     * @param path the path
     * @return the file name without the extension
     */
    public static String getFileNameWithoutExtension(Path path) {
        final String fileName = path.getFileName().toString();
        final int endIndex = fileName.indexOf(".");
        if (endIndex == -1) {
            return fileName;
        } else {
            return fileName.substring(0, endIndex);
        }
    }

    /**
     * @param path - a file or a directory
     */
    public static List<Path> deleteIfExists(Path path) {
        return deleteIfExists(path, false);
    }

    /**
     * @param path - a file or a directory
     */
    public static List<Path> deleteIfExists(Path path, boolean cascade) {
        if (Files.exists(path)) {
            return Fs.delete(path, cascade);
        }
        return new ArrayList<>();
    }

    public static List<Path> delete(Path path) {
        return delete(path, false);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isDirectoryEmpty(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.findFirst().isEmpty();
        }
    }

    /**
     * Delete a file or a directory (with all its content)
     *
     * @param path    the path to delete
     * @param cascade if true and the path is a directory delete cascade all files recursively
     * @return all deleted path
     */
    public static List<Path> delete(Path path, boolean cascade) {
        try {

            List<Path> deletedPaths = new ArrayList<>();

            if (Files.isDirectory(path)) {
                if (!isDirectoryEmpty(path) && !cascade) {
                    throw new IllegalArgumentException("The path (" + path + ") is a non-empty directory, set recursive/cascade to true if you want to delete the directory recursively");
                }
                try (Stream<Path> walk = Files.walk(path)) {
                    walk
                            .map(Path::toFile)
                            .sorted(Comparator.reverseOrder())
                            .forEach(file -> {
                                boolean result = file.delete();
                                if (result) {
                                    deletedPaths.add(file.toPath());
                                } else {
                                    throw new RuntimeException("The path (" + file + ") was not successfully deleted. No reason was given.");
                                }
                            });
                    return deletedPaths;
                }
            }

            Files.delete(path);
            deletedPaths.add(path);
            return deletedPaths;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return a temporary file path.
     * <p>
     * This function is a wrapper around the function {@link Files#createTempFile(String, String, FileAttribute[])}
     * but delete the file to return only a unique path
     * <p>
     * If you want to create it, use a create function such as {@link #createEmptyFile(Path)}
     *
     * @param prefix the beginning of the file name
     * @param suffix the extension (example .txt) when null '.tmp'
     * @return a temporary file path
     * <p>
     * Example:
     * Path path = Fs.getTempFilePath("test",".csv");
     */
    public static Path getTempFilePath(String prefix, String suffix) {

        try {
            Path path = Files.createTempFile(prefix, suffix);
            Files.deleteIfExists(path);
            return path;
        } catch (AccessDeniedException e) {
            // the temp directory may be C:\windows
            // when there is no variable set
            // that is not accessible
            throw new RuntimeException("The access to the temporary directory was denied with the following message (" + e.getMessage() + "). \n The root cause may be that your environment does not have any TEMP / TMP variables. \n As a workaround, you can add the following option `-Djava.io.tmpdir=/temp/path` to a writable directory in your script.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static boolean isEmpty(Path path) {
        try {
            return Files.size(path) == 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the temporary directory
     * <p>
     * Environment variable that have an influence:
     * * TEMP for windows by default: "C:\\Users\\user\\AppData\\Local\\Temp"
     */
    public static Path getTempDirectory() {
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }


    public static String getExtension(Path path) {
        return getExtension(path.getFileName().toString());
    }

    public static String getExtension(String fullFileName) {
        int i = fullFileName.lastIndexOf('.');
        if (i == -1) {
            return null;
        } else {
            return fullFileName.substring(i + 1);
        }
    }

    /**
     * Return the part of the file without its extension
     *
     * @param fullFileName a full file name string
     * @return the extension if any
     */
    public static String getFileNameWithoutExtension(String fullFileName) {
        return fullFileName.substring(0, fullFileName.lastIndexOf('.'));
    }

    /**
     * Create the directory and all its children if not exist
     *
     * @param path a directory path
     */
    public static void createDirectoryIfNotExists(Path path) {

        try {
            if (Files.notExists(path)) {
                /**
                 * Validation check all parts
                 * but as it happens only if it does not exist
                 * that's cool
                 */
                Fs.validateDirectoryPath(path);
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * A directory path should not have any file in it.
     * Otherwise, we get: `Not a directory` error
     * Example:
     * /tmp/zip/my-file
     * is not a valid directory path if
     * /tmp/zip is a file
     */
    public static void validateDirectoryPath(Path directoryPath) {
        Path currentPath = directoryPath;

        while (currentPath != null) {
            if (Files.exists(currentPath)) {
                if (Files.isRegularFile(currentPath)) {
                    throw new IllegalArgumentException(
                            String.format("The directory path '%s' is not valid because the child path '%s' is an existing file. Delete the file or change the directory path.",
                                    directoryPath, currentPath)
                    );
                }
            }
            currentPath = currentPath.getParent();
        }
    }


    /**
     * @return the first root of the current file system
     * This is a utility function mostly for test purpose
     * in order to create a string with a root in it
     */
    public static String getFirstRoot() {

        return StreamSupport
                .stream(Paths.get(".").getFileSystem().getRootDirectories().spliterator(), false)
                .map(Path::toString)
                .findFirst()
                .orElse(null);

    }

    public static String getSeparator() {
        return Paths.get(".").getFileSystem().getSeparator();
    }


    /**
     * A wrapper around {@link Files#move(Path, Path, CopyOption...) move} that makes sure
     * that the target directory is already created
     *
     * @param source the source to move
     * @param target the location of the target
     */
    public static void move(Path source, Path target, CopyOption... copyOptions) {
        try {
            Fs.createDirectoryIfNotExists(target.getParent());
            Files.move(source, target, copyOptions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param path - an absolute or relative path
     * @param name - a name
     * @return the path until a certain name was found (name not included)
     * This is used to get the root/project directory based on a file or a directory found in it
     * @deprecated use {@link #closest(Path, String)} followed {@link Path#getParent()} instead
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public static Path getPathUntilName(Path path, String name) throws FileNotFoundException {

        if (!path.isAbsolute()) {
            // mandatory because getParent is a meta operation
            // parent of the current directory '.' is null
            path = path.toAbsolutePath();
        }
        Path pathUntil = path.resolve(name);
        if (Files.exists(pathUntil)) {
            return pathUntil;
        }
        Path parent = path.getParent();
        while (parent != null) {
            pathUntil = parent.resolve(name);
            if (Files.exists(pathUntil)) {
                return parent;
            }
            parent = parent.getParent();
        }
        throw new FileNotFoundException();

    }


    /**
     * @param path - the path
     * @return the string of a text file
     * <p>
     */
    public static String getFileContent(Path path, Charset charset) throws NoSuchFileException {

        try {
            return Files.readString(path, charset);
        } catch (NoSuchFileException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static String getFileContent(Path path) throws NoSuchFileException {
        return getFileContent(path, Charset.defaultCharset());
    }



    public static boolean isRoot(Path path) {
        return path.getRoot().equals(path);
    }

    /**
     * @param name - the name of the closest path
     * @return the closest path
     */
    public static Path closest(Path path, String name) throws FileNotFoundException {

        Path resolved;
        Path actual = path;
        if (!Files.isDirectory(path)) {
            actual = path.getParent();
        }
        // toAbsolute is needed otherwise the loop
        // will not stop at the root of the file system
        // but at the root of the relative path
        actual = actual.toAbsolutePath();
        while (actual != null) {
            resolved = actual.resolve(name);
            if (Files.exists(resolved)) {
                return resolved;
            }
            actual = actual.getParent();
        }
        throw new FileNotFoundException("No closest file was found");

    }


    public static Path getUserDesktop() {
        // Mac OS X: /Users/username/Desktop.
        // Windows: C:/Users/username/Desktop.
        // Linux: /home/username/Desktop.
        return Fs.getUserHome()
                .resolve("Desktop");
    }

    public static String getInputStreamContent(InputStream inputStream) {
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void setUserAttribute(Path path, String key, String value) throws IOException {


        if (!Files.exists(path)) {
            return;
        }

        FileStore store = Files.getFileStore(path);
        if (store.supportsFileAttributeView(UserDefinedFileAttributeView.class)) {
            UserDefinedFileAttributeView view = Files
                    .getFileAttributeView(path, UserDefinedFileAttributeView.class);
            ByteBuffer valueByteBuffer = Charset.defaultCharset().encode(value);
            view.write(key, valueByteBuffer);
        }


    }

    public static Instant getCreationTime(Path path) {
        BasicFileAttributes attributes;
        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return attributes.creationTime().toInstant();

    }

    /**
     * Truncate the file (no data anymore)
     * Truncate file to 0 bytes (empty the file)
     */
    public static void truncate(Path filePath) {
        truncate(filePath, 0, false);
    }

    /**
     * Truncate the file to a specific size
     * Truncate file to x bytes
     *
     * @return
     */
    public static List<Path> truncate(Path path, long size, boolean cascade) {
        try {

            List<Path> truncatedPath = new ArrayList<>();

            if (Files.isDirectory(path)) {
                if (!isDirectoryEmpty(path) && !cascade) {
                    throw new IllegalArgumentException("The path (" + path + ") is a directory, set recursive/cascade to true if you want to truncate all files in the directory recursively");
                }
                try (Stream<Path> walk = Files.walk(path)) {
                    walk
                            .sorted(Comparator.reverseOrder())
                            .forEach(file -> {
                                truncateUtility(file, size);
                                truncatedPath.add(file);
                            });
                    return truncatedPath;
                }
            }

            truncateUtility(path, size);
            truncatedPath.add(path);
            return truncatedPath;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private static void truncateUtility(Path filePath, long size) {
        try (SeekableByteChannel channel = Files.newByteChannel(filePath,
                StandardOpenOption.WRITE)) {
            channel.truncate(size);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Utility class to return a file from the resource directory
     *
     * @param clazz - the clazz
     * @param path  - the path (called name with a root separator generally)
     * @return the path
     */
    public static Path getPathFromResources(Class<?> clazz, String path) {
        try {
            URL clazzResource = clazz.getResource(path);
            if (clazzResource == null) {
                // verify that they are in the resource directory
                // that in your IDE, the path is marked as resource root
                throw new RuntimeException("Resource not found: " + path);
            }
            return Paths.get(clazzResource.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param s - the resource name starting with a /
     * @return the content as a string
     */
    public static String readStringFromResource(Class<?> clazz, String s) {
        try {
            URI uri = Objects.requireNonNull(clazz.getResource(s)).toURI();
            return Files.readString(Paths.get(uri), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The {@link Files#readString(Path)} without IOException
     *
     * @param path - the path
     * @return the string
     */
    public static String readString(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading the path (" + path + "). Error: " + e.getMessage(), e);
        }
    }
}
