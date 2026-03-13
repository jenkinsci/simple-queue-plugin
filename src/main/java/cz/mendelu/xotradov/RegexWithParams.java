package cz.mendelu.xotradov;

import java.util.List;
import java.util.regex.Pattern;

public class RegexWithParams {

    final String regex;
    final String params;

    public RegexWithParams(String regex, String params) {
        this.regex = regex;
        this.params = params;
    }

    /* "Groovy/PERL style" declaration
     * This ne is always all-matching like matcher.find
     * The /params eg /i is recgnized
     */
    public static RegexWithParams groovyLikeRegexWithParams(String futureRegex) {
        String params = "";
        int lastSlash = futureRegex.lastIndexOf("/");
        params = futureRegex.substring(lastSlash + 1);
        futureRegex = futureRegex.substring(0, lastSlash).replaceFirst("~/", "");
        futureRegex = futureRegex.trim();
        futureRegex = ".*" + futureRegex + ".*";
        return new RegexWithParams(futureRegex, params);
    }

    /* "Java style"
     * Assume the whole idParam string makes sense as a java-style regex
     * (meaning it Matcher::matches() the whole item.task.getDisplayName()
     * string), and the value must be surrounded by `.*` to match from
     * start and/or to the end of string respectively (like find() above
     * does by default). The /i and other params are understood too.
     */
    public static RegexWithParams javaLikeRegexWithParams(String futureRegex) {
        String params = "";
        int lastSlash = futureRegex.lastIndexOf("/");
        if (lastSlash >= 0) {
            params = futureRegex.substring(lastSlash + 1);
            futureRegex = futureRegex.substring(0, lastSlash);
        }
        futureRegex = futureRegex.trim();
        return new RegexWithParams(futureRegex, params);
    }

    public static boolean isGroovy(String idParam) {
        return (idParam.matches("~/.*/.*"));
    }

    public static RegexWithParams exact(String regex, List<ItemTarget> itemTargets) {
        StringBuilder sb = new StringBuilder();
        for (ItemTarget itemTarget : itemTargets) {
            switch (itemTarget) {
                case DISPLAY -> {
                    sb.append("d");
                }
                case FULLDISPLAY -> {
                    sb.append("D");
                }
                case NAME -> {
                    sb.append("n");
                }
                case I -> {
                    sb.append("i");
                }
            }
        }
        return new RegexWithParams(regex, sb.toString());
    }

    public Pattern getPattern() {
        return Pattern.compile(regex, isCaseInsensitive() ? Pattern.CASE_INSENSITIVE : 0);
    }

    public boolean isCaseInsensitive() {
        return params.contains("i");
    }

    public boolean isDisplayName() {
        if (params.contains("d")) {
            return true;
        }
        return !isFullDisplayName() && !isName();
    }

    public boolean isFullDisplayName() {
        return params.contains("D");
    }

    public boolean isName() {
        return params.contains("n");
    }
}
