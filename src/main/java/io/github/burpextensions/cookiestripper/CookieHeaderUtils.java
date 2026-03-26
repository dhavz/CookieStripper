package io.github.burpextensions.cookiestripper;

import burp.IExtensionHelpers;
import burp.IRequestInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class CookieHeaderUtils {
    private CookieHeaderUtils() {
    }

    static List<CookieToken> parseCookieHeaderValue(String headerValue) {
        List<CookieToken> tokens = new ArrayList<CookieToken>();
        if (headerValue == null || headerValue.trim().isEmpty()) {
            return tokens;
        }

        String[] parts = headerValue.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex <= 0) {
                tokens.add(new CookieToken(trimmed, trimmed));
                continue;
            }
            String name = trimmed.substring(0, equalsIndex).trim();
            if (name.isEmpty()) {
                continue;
            }
            tokens.add(new CookieToken(name, trimmed));
        }
        return tokens;
    }

    static List<CookieToken> extractCookiesFromRequestHeaders(List<String> headers) {
        List<CookieToken> cookies = new ArrayList<CookieToken>();
        for (int i = 1; i < headers.size(); i++) {
            String header = headers.get(i);
            if (startsWithHeaderName(header, "Cookie")) {
                cookies.addAll(parseCookieHeaderValue(header.substring(header.indexOf(':') + 1).trim()));
            }
        }
        return cookies;
    }

    static byte[] rewriteRequestCookies(byte[] request, IExtensionHelpers helpers, CookieInventoryTableModel tableModel) {
        IRequestInfo requestInfo = helpers.analyzeRequest(request);
        List<String> headers = requestInfo.getHeaders();
        List<CookieToken> allCookies = extractCookiesFromRequestHeaders(headers);
        if (allCookies.isEmpty()) {
            return null;
        }

        List<CookieToken> remainingCookies = new ArrayList<CookieToken>();
        boolean changed = false;
        for (CookieToken cookie : allCookies) {
            if (tableModel.shouldStrip(cookie.getName())) {
                changed = true;
                continue;
            }
            remainingCookies.add(cookie);
        }

        if (!changed) {
            return null;
        }

        List<String> rewrittenHeaders = new ArrayList<String>();
        rewrittenHeaders.add(headers.get(0));
        boolean insertedCookieHeader = false;
        for (int i = 1; i < headers.size(); i++) {
            String header = headers.get(i);
            if (startsWithHeaderName(header, "Cookie")) {
                if (!insertedCookieHeader && !remainingCookies.isEmpty()) {
                    rewrittenHeaders.add("Cookie: " + buildCookieHeaderValue(remainingCookies));
                    insertedCookieHeader = true;
                }
                continue;
            }
            rewrittenHeaders.add(header);
        }

        byte[] body = Arrays.copyOfRange(request, requestInfo.getBodyOffset(), request.length);
        return helpers.buildHttpMessage(rewrittenHeaders, body);
    }

    static String buildCookieHeaderValue(List<CookieToken> cookies) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cookies.size(); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(cookies.get(i).getRawToken());
        }
        return builder.toString();
    }

    static boolean startsWithHeaderName(String headerLine, String expectedName) {
        if (headerLine == null) {
            return false;
        }
        int colonIndex = headerLine.indexOf(':');
        if (colonIndex <= 0) {
            return false;
        }
        String name = headerLine.substring(0, colonIndex).trim();
        return name.toLowerCase(Locale.ROOT).equals(expectedName.toLowerCase(Locale.ROOT));
    }

    static final class CookieToken {
        private final String name;
        private final String rawToken;

        CookieToken(String name, String rawToken) {
            this.name = name;
            this.rawToken = rawToken;
        }

        String getName() {
            return name;
        }

        String getRawToken() {
            return rawToken;
        }
    }
}
