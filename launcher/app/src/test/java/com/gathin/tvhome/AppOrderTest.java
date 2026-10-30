package com.gathin.tvhome;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Plain JVM tests for laying the saved grid order over the apps installed now. */
public class AppOrderTest {

    private static List<String> keys(String... values) {
        return Arrays.asList(values);
    }

    @Test
    public void theSavedOrderComesFirstAndNewAppsGoToTheEnd() {
        List<String> out = AppOrder.merge(keys("b", "a"), keys("a", "b", "c", "d"));
        assertEquals(keys("b", "a", "c", "d"), out);
    }

    @Test
    public void appsThatAreGoneAreDropped() {
        List<String> out = AppOrder.merge(keys("gone", "b", "a"), keys("a", "b"));
        assertEquals(keys("b", "a"), out);
    }

    @Test
    public void aNewAppBetweenTwoSavedOnesStaysAfterThem() {
        List<String> out = AppOrder.merge(keys("c", "a"), keys("a", "b", "c"));
        assertEquals(keys("c", "a", "b"), out);
    }

    @Test
    public void withoutASavedOrderTheListIsUnchanged() {
        assertEquals(keys("a", "b", "c"), AppOrder.merge(null, keys("a", "b", "c")));
        assertEquals(keys("a", "b", "c"), AppOrder.merge(Collections.emptyList(), keys("a", "b", "c")));
    }

    @Test
    public void duplicatesAndBlanksInTheSavedOrderAreIgnored() {
        List<String> out = AppOrder.merge(keys("b", " b ", "", "a", "b"), keys("a", "b"));
        assertEquals(keys("b", "a"), out);
    }

    @Test
    public void noInstalledAppsGivesAnEmptyOrder() {
        assertTrue(AppOrder.merge(keys("a"), Collections.emptyList()).isEmpty());
        assertTrue(AppOrder.merge(keys("a"), null).isEmpty());
    }
}
