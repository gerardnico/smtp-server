package com.combostrap.type;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A class that represents a possible text file
 * If it's not a text file, it will fail at build time
 * because it could not detect the mandatory character set
 * <p>
 * It wraps common text operation such as:
 * * text detection
 * * character set detection
 * * and buffer creation
 */
@SuppressWarnings("unused")
public class TextFile {

    private static final Logger log = Logger.getLogger(TextFile.class.getName());

    private final Charset charset;
    private final Path path;


    public TextFile(FsTextFileBuilder builder) {

        charset = builder.charset;
        path = builder.path;

    }


    public static FsTextFileBuilder builder(Path path) {
        return new FsTextFileBuilder(path);
    }

    public BufferedReader getBufferedReader() {
        try {
            return Files.newBufferedReader(path, this.getCharset());
        } catch (IOException e) {
            throw new RuntimeException("We were unable to open the file (" + path + "). Error: " + e.getMessage(), e);
        }
    }

    /**
     * @return a charset or null
     */
    public Charset getCharset() {

        return charset;

    }


    public static class FsTextFileBuilder {
        private final Path path;


        public FsTextFileBuilder setCharset(Charset charset) {
            this.charset = charset;
            return this;
        }

        private Charset charset;
        /**
         * The detected charset name (maybe not supported)
         * and the Charset cannot be created
         * That's why we have it here
         */
        private String charsetName;

        public FsTextFileBuilder(Path path) {
            this.path = path;
        }

        public TextFile build() throws TextDetectedCharsetNotSupported, TextCharacterSetNotDetected {

            if (this.charset == null) {
                if (!Files.exists(path)) {
                    throw new TextCharacterSetNotDetected("The character set is null and the file does not exist, we can't detect any character set");
                }
                this.charset = detectCharacterSet();
                if (this.charset == null) {
                    throw new TextCharacterSetNotDetected("No character set has been detected");
                }
            }

            return new TextFile(this);
        }

        /**
         * <a href="https://unicode-org.github.io/icu/userguide/conversion/detection.html">...</a>
         *
         * @return an encoding value or null if this is not possible
         * See possible values at
         * <a href="http://userguide.icu-project.org/conversion/detection#TOC-Detected-Encodings">...</a>
         * @throws TextDetectedCharsetNotSupported if the charset detected is not supported on the os
         */
        public Charset detectCharacterSet() throws TextDetectedCharsetNotSupported {


            /*
             * Buffered reader is important because
             * the detector make use of the mark/reset
             */
            String charsetName;
            try (InputStream bis = new BufferedInputStream(Files.newInputStream(path))) {
                CharsetDetector charsetDetector = new CharsetDetector();
                charsetDetector.setText(bis);
                CharsetMatch match = charsetDetector.detect();
                if (match == null) {
                    return null;
                }
                charsetName = match.getName();
            } catch (Exception e) {
                /*
                 * If the file is used, we can get a java.nio.file.FileSystemException exception
                 * such as `The process cannot access the file`
                 * Example on windows with `C:/Users/userName/NTUSER.DAT`
                 * <p>
                 * We can also get a problem when basic authentication is mandatory
                 * for http path
                 * <p>
                 * For now, we return null (the user will know that we can't detect it and that it should set it)
                 */
                log.fine(() -> "Error while detecting the character set of the file (" + path + ")" + e.getMessage());
                return null;
            }
            if (charsetName == null) {
                return null;
            }
            this.charsetName = charsetName;
            if (!Charset.isSupported(charsetName)) {
                throw new TextDetectedCharsetNotSupported("The character set value (" + charsetName + ") is not supported. You may set the character set to one of this values: " + String.join(", ", Charset.availableCharsets().keySet()));
            }
            return Charset.forName(charsetName);

        }


        /**
         * @return the character set name or empty string
         */
        public String getCharsetName() {
            if (this.charsetName != null) {
                return this.charsetName;
            }
            if (this.charset != null) {
                return this.charset.name();
            }
            return "";
        }

    }

    /**
     * Reads and parses the shebang line from a script file
     *
     * @return The shebang interpreter path and arguments or null if none found
     */
    public String getShebang() {

        try (BufferedReader reader = Files.newBufferedReader(path, this.getCharset())) {
            String firstLine = reader.readLine();

            if (firstLine == null) {
                return null;
            }

            firstLine = firstLine.trim();

            if (!firstLine.startsWith("#!")) {
                return null;
            }

            // Remove the #! prefix
            return firstLine.substring(2).trim();

        } catch (IOException e) {
            throw new RuntimeException("IO Error while trying to read the file (" + this.path + "). Error: " + e.getMessage(), e);
        }
    }

    public List<String> getShebangArgs() {
        String shebang = getShebang();
        if (shebang == null) {
            return new ArrayList<>();
        }
        // Parse shebang - it might contain interpreter arguments
        return Arrays.stream(shebang.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());


    }


}
