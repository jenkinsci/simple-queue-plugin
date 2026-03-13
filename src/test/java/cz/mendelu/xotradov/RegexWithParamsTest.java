package cz.mendelu.xotradov;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RegexWithParamsTest {

    @Test
    void crateBasicJavaRegex() {
        RegexWithParams r = RegexWithParams.javaLikeRegexWithParams("a[12]");
        assertEquals("a[12]", r.regex);
        assertEquals("", r.params);
    }

    @Test
    void crateBasicJavaRegexWithParams() {
        RegexWithParams r = RegexWithParams.javaLikeRegexWithParams("a[12]/abc");
        assertEquals("a[12]", r.regex);
        assertEquals("abc", r.params);
    }

    @Test
    void trickJavaSlash() {
        RegexWithParams r = RegexWithParams.javaLikeRegexWithParams("sla/sh/");
        assertEquals("sla/sh", r.regex);
        assertEquals("", r.params);
    }

    @Test
    void crateBasicGroovyRegex() {
        RegexWithParams r = RegexWithParams.groovyLikeRegexWithParams("~/a[12]/");
        assertEquals(".*a[12].*", r.regex);
        assertEquals("", r.params);
    }

    @Test
    void crateBasicGroovyRegexWithParams() {
        RegexWithParams r = RegexWithParams.groovyLikeRegexWithParams("~/a[12]/abc");
        assertEquals(".*a[12].*", r.regex);
        assertEquals("abc", r.params);
    }

    @Test
    void trickGroovySlash() {
        RegexWithParams r = RegexWithParams.groovyLikeRegexWithParams("~/sla/sh/");
        assertEquals(".*sla/sh.*", r.regex);
        assertEquals("", r.params);
    }

    @Test
    void isGroovyTest() {
        assertTrue(RegexWithParams.isGroovy("~//"));
        assertTrue(RegexWithParams.isGroovy("~/blah/"));
        assertTrue(RegexWithParams.isGroovy("~/bl/ah/"));
        assertTrue(RegexWithParams.isGroovy("~/bl/ah/params"));
        assertTrue(RegexWithParams.isGroovy("~/blah/params"));
        assertFalse(RegexWithParams.isGroovy("/blah/params"));
        assertFalse(RegexWithParams.isGroovy("/blah"));
        assertFalse(RegexWithParams.isGroovy("~/blah"));
        assertFalse(RegexWithParams.isGroovy("bl/ah"));
        assertFalse(RegexWithParams.isGroovy("b~/l/ah"));
    }

    @Test
    void isDisplayNameTest() {
        assertTrue(RegexWithParams.groovyLikeRegexWithParams("~//i").isDisplayName());
        assertTrue(RegexWithParams.groovyLikeRegexWithParams("~//").isDisplayName());
        assertTrue(RegexWithParams.javaLikeRegexWithParams("blah/i").isDisplayName());
        assertTrue(RegexWithParams.javaLikeRegexWithParams("blah/").isDisplayName());
        assertTrue(RegexWithParams.javaLikeRegexWithParams("blah").isDisplayName());
        assertFalse(RegexWithParams.javaLikeRegexWithParams("blah/D").isDisplayName());
        assertTrue(RegexWithParams.javaLikeRegexWithParams("blah/Dd").isDisplayName());
        assertFalse(RegexWithParams.javaLikeRegexWithParams("blah/n").isDisplayName());
        assertFalse(RegexWithParams.javaLikeRegexWithParams("blah/DnN").isDisplayName());
        assertTrue(RegexWithParams.javaLikeRegexWithParams("blah/DnNd").isDisplayName());
    }
}
