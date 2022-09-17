package plc.homework;

import java.util.regex.Pattern;

/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regexes as needed.
 */
public class Regex {

    public static final Pattern
            EMAIL = Pattern.compile("[A-Za-z0-9._]{2,}@[A-Za-z0-9~]+\\.([A-Za-z0-9-]+\\.)*[a-z]{3}"),
            ODD_STRINGS = Pattern.compile("^((.){11}(..){0,4})$"), //TODO

            CHARACTER_LIST = Pattern.compile("\\[(\\s*('.')\\s*|\\s*)(,\\s*'.'\\s*)*\\]"), //TODO

            DECIMAL = Pattern.compile("^-?([1-9][0-9]*|0)\\.[0-9]+$"),//TODO
            STRING = Pattern.compile("\"([^\\\\]|\\\\[bnrt'\\\"\\\\])*\""); //TODO
        //".*(\\(?![^bnrt'"\\]))?.*"
        //need help with string feedback appreciated :p
}
