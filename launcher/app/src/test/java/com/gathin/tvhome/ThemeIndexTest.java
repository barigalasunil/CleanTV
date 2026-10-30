package com.gathin.tvhome;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Plain JVM tests for wallpapers.json and for the folder listing that stands in for it. */
public class ThemeIndexTest {

    private static List<String> names(String... values) {
        return Arrays.asList(values);
    }

    /** Parses an index against the files that are said to be in the folder. */
    private static List<ThemeIndex.Entry> parse(String json, String... present) {
        Set<String> files = new HashSet<>(names(present));
        return ThemeIndex.parse(json, files::contains);
    }

    @Test
    public void readsEveryField() {
        List<ThemeIndex.Entry> entries = parse(
                "[{\"file\":\"w-01.jpg\",\"thumb\":\"t-01.jpg\",\"mood\":\"Day\",\"name\":\"Desert road\"}]",
                "w-01.jpg", "t-01.jpg");
        assertEquals(1, entries.size());
        assertEquals("w-01.jpg", entries.get(0).file);
        assertEquals("t-01.jpg", entries.get(0).thumb);
        assertEquals("Day", entries.get(0).mood);
        assertEquals("Desert road", entries.get(0).name);
    }

    @Test
    public void acceptsTheWrappedForm() {
        List<ThemeIndex.Entry> entries = parse(
                "{\"wallpapers\":[{\"file\":\"w-02.jpg\",\"mood\":\"Night\"}]}", "w-02.jpg");
        assertEquals(1, entries.size());
        assertEquals("Night", entries.get(0).mood);
    }

    @Test
    public void thumbNameAndMoodAreOptional() {
        List<ThemeIndex.Entry> entries = parse("[{\"file\":\"w-02.jpg\"}]", "w-02.jpg");
        assertEquals(1, entries.size());
        assertEquals("t-02.jpg", entries.get(0).thumb);
        assertEquals("w-02", entries.get(0).name);
        assertEquals("", entries.get(0).mood);
    }

    @Test
    public void entriesKeepTheOrderOfTheFile() {
        List<ThemeIndex.Entry> entries = parse(
                "[{\"file\":\"w-03.jpg\"},{\"file\":\"w-01.jpg\"},{\"file\":\"w-02.jpg\"}]",
                "w-01.jpg", "w-02.jpg", "w-03.jpg");
        assertEquals("w-03.jpg", entries.get(0).file);
        assertEquals("w-01.jpg", entries.get(1).file);
        assertEquals("w-02.jpg", entries.get(2).file);
    }

    @Test
    public void aPictureThatIsNotThereIsSkipped() {
        List<ThemeIndex.Entry> entries = parse(
                "[{\"file\":\"w-01.jpg\"},{\"file\":\"w-02.jpg\"}]", "w-02.jpg");
        assertEquals(1, entries.size());
        assertEquals("w-02.jpg", entries.get(0).file);
    }

    @Test
    public void aMissingThumbFallsBackToTheDerivedOne() {
        List<ThemeIndex.Entry> entries = parse(
                "[{\"file\":\"w-04.jpg\",\"thumb\":\"t-99.jpg\"}]", "w-04.jpg");
        assertEquals("t-04.jpg", entries.get(0).thumb);
    }

    @Test
    public void unsafeAndDuplicateNamesAreSkipped() {
        List<ThemeIndex.Entry> entries = parse(
                "[{\"file\":\"../secret.jpg\"},{\"file\":\".hidden.jpg\"},{\"file\":\"\"},"
                        + "{\"file\":\"w-01.jpg\"},{\"file\":\"W-01.jpg\"}]",
                "w-01.jpg", "W-01.jpg", "..", "secret.jpg", ".hidden.jpg");
        assertEquals(1, entries.size());
        assertEquals("w-01.jpg", entries.get(0).file);
    }

    @Test
    public void theListIsCappedAtTwenty() {
        StringBuilder json = new StringBuilder("[");
        List<String> present = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            String file = String.format("w-%02d.jpg", i);
            present.add(file);
            if (i > 1) json.append(',');
            json.append("{\"file\":\"").append(file).append("\"}");
        }
        json.append(']');
        List<ThemeIndex.Entry> entries = parse(json.toString(), present.toArray(new String[0]));
        assertEquals(ThemeIndex.MAX, entries.size());
        assertEquals(20, ThemeIndex.MAX);
        assertEquals("w-20.jpg", entries.get(19).file);
    }

    @Test
    public void aMissingOrBrokenFileIsAnEmptyIndex() {
        assertTrue(parse(null).isEmpty());
        assertTrue(parse("").isEmpty());
        assertTrue(parse("{").isEmpty());
        assertTrue(parse("{}").isEmpty());
        assertTrue(parse("\"just a string\"").isEmpty());
        assertTrue(parse("[]").isEmpty());
    }

    // ---- the fallback: the folder itself ---------------------------------------------------------

    @Test
    public void theFolderIsSortedAndLabelledByName() {
        List<ThemeIndex.Entry> entries = ThemeIndex.scan(names("w-03.jpg", "w-01.jpg", "w-02.jpg"));
        assertEquals(3, entries.size());
        assertEquals("w-01.jpg", entries.get(0).file);
        assertEquals("w-01", entries.get(0).name);
        assertEquals("t-01.jpg", entries.get(0).thumb);
        assertEquals("", entries.get(0).mood);
        assertEquals("w-03.jpg", entries.get(2).file);
    }

    @Test
    public void theFolderFallbackSkipsUnusableNames() {
        List<ThemeIndex.Entry> entries = ThemeIndex.scan(names("../x.jpg", ".spot.jpg", "ok.jpg"));
        assertEquals(1, entries.size());
        assertEquals("ok.jpg", entries.get(0).file);
    }

    @Test
    public void theFolderFallbackIsCappedToo() {
        List<String> present = new ArrayList<>();
        for (int i = 1; i <= 25; i++) present.add(String.format("w-%02d.jpg", i));
        assertEquals(ThemeIndex.MAX, ThemeIndex.scan(present).size());
    }

    @Test
    public void anEmptyFolderGivesNoPictures() {
        assertTrue(ThemeIndex.scan(names()).isEmpty());
        assertTrue(ThemeIndex.scan(null).isEmpty());
    }

    @Test
    public void thePreviewFollowsThePictureName() {
        assertEquals("t-07.jpg", ThemeIndex.defaultThumb("w-07.jpg"));
        assertEquals("photo.jpg", ThemeIndex.defaultThumb("photo.png"));
        assertEquals("holiday-1.jpg", ThemeIndex.defaultThumb("holiday-1.jpeg"));
    }
}
