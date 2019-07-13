package org.iota.utility;

import java.net.MalformedURLException;
import java.net.URL;

public class ArgValidator {

    public static boolean isUrl(String arg) {
        try {
            new URL(arg);
            return true;
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}
