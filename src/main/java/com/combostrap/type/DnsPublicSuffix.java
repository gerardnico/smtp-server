package com.combostrap.type;


import com.combostrap.common.Fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DnsPublicSuffix {

    private static final Set<String> SUFFIXES = loadSuffixes();

    private static Set<String> loadSuffixes() {

        Set<String> set = new HashSet<>();
        // Note:
        // this file: https://publicsuffix.org/list/effective_tld_names.dat should be updated regularly
        // even if it does not change every day
        Path effective = Fs.getPathFromResources(DnsPublicSuffix.class, "/dns/effective_tld_names.txt");
        try (BufferedReader reader = Files.newBufferedReader(effective, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (!line.isEmpty() && !line.startsWith("//")) {
                    set.add(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Collections.unmodifiableSet(set);

    }

    public static boolean isPublicSuffix(String domain) {
        return SUFFIXES.contains(domain.toLowerCase());
    }

    public static Set<String> getPublicSuffixes() {
        return SUFFIXES;
    }
}
