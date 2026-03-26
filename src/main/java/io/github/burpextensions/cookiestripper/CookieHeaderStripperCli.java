package io.github.burpextensions.cookiestripper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CookieHeaderStripperCli {
    private CookieHeaderStripperCli() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: CookieHeaderStripperCli <path-to-cookie-header-file> [cookieNameToStrip ...]");
            System.exit(1);
        }

        String input = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8).trim();
        String headerValue = input;
        if (CookieHeaderUtils.startsWithHeaderName(input, "Cookie")) {
            headerValue = input.substring(input.indexOf(':') + 1).trim();
        }

        List<CookieHeaderUtils.CookieToken> cookies = CookieHeaderUtils.parseCookieHeaderValue(headerValue);
        if (args.length == 1) {
            for (CookieHeaderUtils.CookieToken cookie : cookies) {
                System.out.println(cookie.getName());
            }
            return;
        }

        Set<String> stripNames = new HashSet<String>();
        for (int i = 1; i < args.length; i++) {
            stripNames.add(args[i].toLowerCase(Locale.ROOT));
        }

        List<CookieHeaderUtils.CookieToken> remaining = new ArrayList<CookieHeaderUtils.CookieToken>();
        for (CookieHeaderUtils.CookieToken cookie : cookies) {
            if (!stripNames.contains(cookie.getName().toLowerCase(Locale.ROOT))) {
                remaining.add(cookie);
            }
        }

        if (remaining.isEmpty()) {
            System.out.println("<Cookie header removed>");
            return;
        }

        System.out.println("Cookie: " + CookieHeaderUtils.buildCookieHeaderValue(remaining));
    }
}
