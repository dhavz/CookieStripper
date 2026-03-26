package io.github.burpextensions.cookiestripper;

final class CookieEntry {
    private final String key;
    private String displayName;
    private boolean selected;
    private int seenCount;
    private String lastTool;

    CookieEntry(String displayName) {
        this.key = displayName.toLowerCase();
        this.displayName = displayName;
        this.lastTool = "-";
    }

    String getKey() {
        return key;
    }

    String getDisplayName() {
        return displayName;
    }

    void updateDisplayName(String candidate) {
        if (candidate != null && candidate.length() > displayName.length()) {
            this.displayName = candidate;
        }
    }

    boolean isSelected() {
        return selected;
    }

    void setSelected(boolean selected) {
        this.selected = selected;
    }

    int getSeenCount() {
        return seenCount;
    }

    String getLastTool() {
        return lastTool;
    }

    void seen(String toolName) {
        seenCount++;
        lastTool = toolName;
    }
}
