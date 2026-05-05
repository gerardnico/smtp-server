package com.combostrap.type;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.logging.Logger;

public class MediaTypes {

    static Logger LOGGER = Logger.getLogger(MediaTypes.class.getName());

    /**
     * @param absolutePath an absolute path
     * @return the media/content type string that is somewhat normalized
     * @throws MediaTypePathNotAbsoluteException if the path is not absolute (important to see if this is a directory media type)
     *                              We don't return an object because MediaType object are created by the type manager
     *                              because not all types are known in advance, and they are normally enum
     *                              If you want to detect your own media type, you should implement a {@link FileTypeDetector}
     */
    public static MediaType detectMediaType(Path absolutePath) throws MediaTypePathNotAbsoluteException {

        if (!absolutePath.isAbsolute()) {
            throw new MediaTypePathNotAbsoluteException("The path (" + absolutePath + ") is not absolute, we can't determine it media type");
        }

        /**
         * If this is a directory
         */
        if (Files.isDirectory(absolutePath)) {
            return MediaType.DIR;
        }

        /**
         * File System based
         * They need to implement java.nio.file.spi.FileTypeDetector
         */
        String mediaTypeString;
        try {
            /**
             * May be implemented
             */
            mediaTypeString = Files.probeContentType(absolutePath);
            // mediaTypeString may be null if not detected
            if(mediaTypeString != null) {
                return MediaType.parse(mediaTypeString);
            }
        } catch (IOException e) {
            // Log is depend on the type module unfortunately
            // LoggerType.LOGGER.fine("Error while guessing the mime type of (" + path + ") via probeContent", e.getMessage());
        }


        /**
         * Name based
         */
        Path fileName = absolutePath.getFileName();
        if (fileName == null) {
            // file system may not have any name in the path for file
            // (ie http has no directory only file, but they may have no name. Example: https://example.com)
            throw new RuntimeException("The file (" + absolutePath + ") does not have any name");
        }



        /**
         * Name based
         */
        mediaTypeString = URLConnection.guessContentTypeFromName(fileName.toString());
        if(mediaTypeString != null) {
            return MediaType.parse(mediaTypeString);
        }


        if (!Files.exists(absolutePath)) {
            return MediaType.BINARY;
        }


        /**
         * Open and guess content
         */

        /**
         * BufferedInputStream was chosen because it supports marks
         * Otherwise it does not work
         */
        try (InputStream is = new BufferedInputStream(Files.newInputStream(absolutePath))) {
            mediaTypeString = URLConnection.guessContentTypeFromStream(is);
            if (mediaTypeString != null) {
                return MediaType.parse(mediaTypeString);
            }
        } catch (Exception e) {
            /**
             *
             * We may get an error it this is a http url and there is no basic authentication property
             * yet set
             */
            LOGGER.fine(() -> "Error while guessing the mime type of (" + absolutePath + ") via content reading. Message: " + e.getMessage());

        }

        /**
         * Try to return a text (Charset to verify that this is a text file)
         */
        try {
            TextFile.builder(absolutePath).build();
            return MediaType.TEXT_PLAIN;
        } catch (TextDetectedCharsetNotSupported e) {
            // charset detected but not supported
            return MediaType.TEXT_PLAIN;
        } catch (TextCharacterSetNotDetected e) {
            //
        }


        // Unknown
        return MediaType.BINARY;

    }

}
