package com.google.projectgameface;

public final class ZiyouInputState {
    private String pendingInitial;
    private String compositionPreview = "";

    public void setPendingInitial(String initial) {
        pendingInitial = initial;
    }

    public void clearPendingInitial() {
        pendingInitial = null;
    }

    public String getPendingInitial() {
        return pendingInitial;
    }

    public boolean hasComposition() {
        return !compositionPreview.trim().isEmpty();
    }

    public String getCompositionPreview() {
        return compositionPreview;
    }

    public String getTrimmedCompositionPreview() {
        return compositionPreview.trim();
    }

    public void appendSyllable(String syllable) {
        if (compositionPreview.isEmpty()) {
            compositionPreview = syllable;
            return;
        }
        compositionPreview = compositionPreview + " " + syllable;
    }

    public boolean removeLastSyllable() {
        String trimmed = compositionPreview.trim();
        if (trimmed.isEmpty()) {
            compositionPreview = "";
            return false;
        }
        int lastSpace = trimmed.lastIndexOf(' ');
        compositionPreview = lastSpace >= 0 ? trimmed.substring(0, lastSpace) : "";
        return true;
    }

    public void clearComposition() {
        compositionPreview = "";
    }

    public String getPreviewText(boolean ziyouMode, String standardPlaceholder, String ziyouPlaceholder) {
        if (ziyouMode) {
            return hasComposition() ? getTrimmedCompositionPreview() : ziyouPlaceholder;
        }
        return hasComposition() ? compositionPreview : standardPlaceholder;
    }
}