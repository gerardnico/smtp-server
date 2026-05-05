package com.combostrap.common;


import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Static function around the JVM
 */
public class Javas {

    /**
     * @param clazz - the class where the resource is collocated
     * @param name  - the name (starting with a /) example: /pipeline/email_record_run.yml
     */
    public static Optional<Path> getResourcePath(Class<?> clazz, String name) {
        URL resource = clazz.getResource(name);
        if (resource == null) {
            return Optional.empty();
        }
        return Optional.of(getFilePathFromUrl(resource));
    }

    /**
     * Same as {@link #getResourcePath(Class, String)} without exception
     */
    public static Optional<Path> getResourcePathSafe(Class<?> clazz, String name) {
        return getResourcePath(clazz, name);
    }

    /**
     * @return return the class file path or the jar if located in a jar
     */
    public static Path getFilePathFromUrl(URL sourceCodeUrl) {
        try {

            switch (sourceCodeUrl.getProtocol()) {
                case "file":
                    return Paths.get(sourceCodeUrl.toURI());

                case "jar":

                    String fileUri = sourceCodeUrl.getFile();

                    /**
                     * Path in a jar have at the end a suffix separated by an exclamation such as
                     * dir/myjar!package.myclass
                     * We delete it if we are in a jar
                     */
                    fileUri = fileUri.substring(0, fileUri.indexOf("!"));

                    /**
                     * Send back the path
                     */
                    URI pathUri = URI.create(fileUri);
                    return Paths.get(pathUri);
                default:
                    throw new RuntimeException("The protocol (" + sourceCodeUrl.getProtocol() + ") is not implemented and we can therefore return the class file path back");
            }


        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getSourceCodePath(Class<?> clazz) {
        return getFilePathFromUrl(Objects.requireNonNull(clazz.getResource(clazz.getSimpleName() + ".class")));
    }

    public static Boolean getSourceCodePathInJar(Class<?> clazz) {
        URL sourceCodeUrl = Objects.requireNonNull(clazz.getResource(clazz.getSimpleName() + ".class"));

        switch (sourceCodeUrl.getProtocol()) {
            case "file":
                return false;
            case "jar":
                return true;
            default:
                throw new RuntimeException("The protocol (" + sourceCodeUrl.getProtocol() + ") is not implemented and we can therefore return the class file path back");
        }

    }

    public static Path getModulePath(Class<?> clazz) throws NotDirectoryException {


        Path buildPath = getBuildDirectory(clazz);
        return buildPath.getParent();


    }


    /**
     * Check if there is a build directory
     * (We could also check if there is a build file (ie gradle.kts)
     * in the working directory to not throw an error when testing the software locally
     * See Tabli.hasBuildFileInRunningDirectory
     */
    public static Path getBuildDirectory(Class<?> clazz) throws NotDirectoryException {


        URL url = Objects.requireNonNull(clazz.getResource(clazz.getSimpleName() + ".class"));
        if (url.getProtocol().equals("jar")) {
            throw new NotDirectoryException("No build path can be found as the class (" + clazz + ") is in a jar (" + url + ")");
        }

        Path sourceCodePath;
        try {
            sourceCodePath = Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }


        List<String> buildPathNames = Arrays.asList(
                "build", // gradle
                "out", // idea
                "target" // maven
        );

        for (String buildPathName : buildPathNames) {
            try {
                return Fs.closest(sourceCodePath.getParent(), buildPathName);
            } catch (FileNotFoundException e) {
                // not found
            }
        }

        throw new NotDirectoryException("No build path was found from class (" + clazz + ") in the path (" + sourceCodePath + ")");

    }

    /**
     * Package in java are not hierarchical (a package does not have any parent)
     * We need then to do it
     *
     * @param clazz - the clazz
     * @return the apex package (ie the first 2 names xxx.xxxx)
     */
    public static String getApexPackage(Class<?> clazz) {
        return String.join(".", Arrays.asList(clazz.getPackage().getName().split("\\.")).subList(0, 2));
    }

    /**
     * Just a snippet that shows a way to locate the code source
     * We use know {@link #getSourceCodePath(Class)}
     */
    @SuppressWarnings("unused")
    public void getCodeSourcePathFromCodeSource() {

        URL location = Javas.class.getProtectionDomain().getCodeSource().getLocation();
        System.out.println(location.getFile());

    }
}
