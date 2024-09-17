package com.consorsbank.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class HelperTest {

    @Test
    public void testGetHash() {
        String expectedHash = "14ab2924adb4c2f0ce9d37b0afa98a9d";
        String input = "My Input";

        String hash = Helper.getHash(input);

        assert (hash.equals(expectedHash));
    }

    @Test
    public void testBankIdValid() {
        String bankId = "<WELADED1WDB> DE33478535200003845849";
        assertTrue(Helper.bankIdValid(bankId));

        bankId = "VISA 58525010 Paderborn";
        assertTrue(Helper.bankIdValid(bankId));

        bankId = "girocard";
        assertFalse(Helper.bankIdValid(bankId));
    }

    @Test
    public void testTrackingIdIsValid() {
        String trackingId = "2334920001037";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD2334920001037";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD2334920001037";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        // Check for at most 20 digits
        trackingId = "JD23349200010375785260";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD233492000103757852602";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD23349200010375785260";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD233492000103757852602";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD23349200010375785260";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD233492000103757852602";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        // Check for at least 10 digits
        trackingId = "2334920001";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "233492000";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD233492000";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JD2334920001";
        assertTrue(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD233492000";
        assertFalse(Helper.trackingIdIsValid(trackingId));

        trackingId = "JJD2334920001";
        assertTrue(Helper.trackingIdIsValid(trackingId));
    }
}
