package com.compactvfs.utils;

import com.compactvfs.model.VFSDirectory;

import static java.util.stream.Collectors.joining;

public class DrawUtils {
    public static final String NEXT_LINE = System.lineSeparator();
    public static final String TAB = "    ";
    public static final String DASHES_TAB = "----";

    public static String toTreeString(VFSDirectory vfsDirectory) {
        return toTreeStringWithTabs(vfsDirectory, 0);
    }

    private static String toTreeStringWithTabs(VFSDirectory vfsDirectory, int tabsCount) {
        String dirNameIndent = tabsCount == 0 ? "" : TAB.repeat(tabsCount - 1) + DASHES_TAB;
        String nestedDirsTree = vfsDirectory.getSubDirectories()
                .stream()
                .map(subDir -> toTreeStringWithTabs(subDir, tabsCount + 2) + NEXT_LINE)
                .collect(joining());
        String subFileNames = vfsDirectory.getSubFiles()
                .stream()
                .map(subFile -> TAB.repeat(tabsCount + 1) + DASHES_TAB + subFile.getName() + NEXT_LINE)
                .collect(joining());
        return "" +
                dirNameIndent + "Directory: " + vfsDirectory.getName() + NEXT_LINE +
                TAB.repeat(tabsCount + 1) + "SubDirectories: " + NEXT_LINE +
                nestedDirsTree +
                TAB.repeat(tabsCount + 1) + "SubFiles: " + NEXT_LINE +
                subFileNames;
    }
}
