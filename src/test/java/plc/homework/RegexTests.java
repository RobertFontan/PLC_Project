package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentAccessException;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                //matching
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Numbers", "7831239@gmail.com", true),
                Arguments.of("Caps Lock", "SAMPLEDOMAIN@gmail.com", true),
                Arguments.of("Caps Domain", "thestandard@GMAIL.com", true),
                //non-matching
                Arguments.of("Number dot Com", "sample@gmail.co2", false),
                Arguments.of("Typo dot Com", "sample@gmail.comm", false),
                Arguments.of("No @", "lefraudgmail.com", false),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                //matching
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("15 Characters", "123123121233315", true),
                Arguments.of("17 Characters", "$$$$!@$#%^&(*&^", true),
                Arguments.of("19 Characters", "whyisithardtomake19", true),
                //non matching
                Arguments.of("No Character", "", false),
                Arguments.of("1 Character", "1", false),
                Arguments.of("3 Character", "123", false),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("9 Characters", "chicanery", false),
                Arguments.of("10 Characters", "password10", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("21 Character", "tomakefaketeststrin21", false)
                );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                //matching
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Mixed Spaces", "['a','b', 'c']",true),
                Arguments.of( "Symbols", "['1','^','$']",true),
                Arguments.of("Empty List", "[]", true),
                //non matching
                Arguments.of("Missing Quotes", "[a,b,c]", false),
                Arguments.of("Missing Left Quote", "[a']", false),
                Arguments.of("Missing Right Quote", "['a]", false),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("Missing Commas and Quotes", "[l o l]", false),
                Arguments.of("Wrong Bracket", "('a','b','c')", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.DECIMAL, success);
    }

    public static Stream<Arguments> testDecimalRegex() {
        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                //matching
                Arguments.of("Multiple Digit Decimal", "10100.001", true),
                Arguments.of("Single Digit Decimal", "1.0", true),
                Arguments.of("Single Zero to the Left", "0.4", true),
                Arguments.of("Negative Number", "-6.0", true),
                Arguments.of("Trailing Zero", "33.1000", true),
                Arguments.of("Large Negative Number","-123.456", true),
                //non matching
                Arguments.of("Leading Positive", "+1.0", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Leading Zeroes", "003.0", false),
                Arguments.of("No Decimal", "90", false),
                Arguments.of("Too many Decimal", "3..0", false),
                Arguments.of("Not a number", "d", false),
                Arguments.of("Not a number pt 2", "!", false)

        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        //throw new UnsupportedOperationException(); //TODO
        test(input, Regex.STRING, success);
    }

    public static Stream<Arguments> testStringRegex() {
        //throw new UnsupportedOperationException(); //TODO
        return Stream.of(
                //matching
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Hello World", "\"Hello, World!\"", true),
                Arguments.of("Escape", "\"1\\t2\"", true),
                Arguments.of("Numbers", "\"123123123\"", true),
                Arguments.of("Symbols", "\"!@#$%^\"", true),
                Arguments.of("Many quotes", "\"\"", true),
                //Arguments.of(),
                //non matching
                Arguments.of("Unterminated Right", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("No Escape Character", "\"sample\\sample\"", false),
                Arguments.of("Typo", "\"sample\"sample", false),
                Arguments.of("Unterminated Left", "sample\"", false)
        );

    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
