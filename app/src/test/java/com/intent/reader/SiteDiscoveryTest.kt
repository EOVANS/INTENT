package com.intent.reader

import com.intent.reader.sync.SiteDiscovery
import org.junit.Assert.*
import org.junit.Test

class SiteDiscoveryTest {
    @Test fun convertsRsshubRoute() {
        assertEquals("https://rsshub.cups.moe/anthropic/engineering?limit=5", SiteDiscovery.normalizeUrl("rsshub://anthropic/engineering?limit=5"))
    }

    @Test fun recognizesSecurityPageAndFeedContainingHtml() {
        assertTrue(SiteDiscovery.isVerificationPage("<html><title>Security Verification</title></html>"))
        assertFalse(SiteDiscovery.isVerificationPage("<rss><channel><title>News</title></channel></rss>"))
        assertTrue(SiteDiscovery.looksLikeFeed("<rss><description><![CDATA[<html>article</html>]]></description></rss>"))
    }
}
