package assembler.util;

import assembler.arc8051.MC8051Library;
import assembler.util.problems.PreprocessingProblem;
import assembler.util.problems.Problem;
import misc.Settings;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Represents a <code>Regex</code> that can be used with the preprocessor
 * or other text-based parts of the assembler.<br>
 * For a detailed documentation of the features see the documentation of
 * the constructor: {@link #Regex(String, Path, int, List)}
 *
 * @author Noxgrim
 */
public class Regex {
    /** The internal Pattern.*/
    private Pattern match;
    /** The String a match will be replaced. Supports groups.*/
    private String substitution;
    /** The <code>Regex</code>'s modifier. */
    private StringBuffer modifier;
    /** The segments (all Strings that are separated by '/'. */
    private String[] segments;
    /**
     * A lowercased representation of the Regex's Pattern.<br>
     * Used for equality purposes if the Regex is case insensitive.
     */
    private String lowerCased;

    /** Whether all occurrences (<code>true</code>) or only the first will be replaced by the substitution. */
    private boolean global = true;  // Only in substitution
    /** Whether the Pattern will be case sensitive. */
    private boolean caseSensitive = false; // Only in Substitution
    /** If this variable is set the substitution will also be performed inside of '"' and '\'. */
    private boolean replaceString = false;

    /** If <code>false</code> this <code>Regex</code> shouldn't be modified. */
    private boolean modifiable = true;

    /** The file that will be used when creating new Problems. */
    private Path problemFile;
    /** The line that will be used when creating new Problems. */
    private int problemFileLine;
    /** The List newly created Problems will be added to. */
    private List<Problem> problems;


    //Valid modifiers
    /** Adds a substitute segment which will be used to substitute a match. */
    public static final char SUBSTITUTE_MODIFIER          = 's';
    /** Adds a segments that specifies a message of a ERROR that will be created if the pattern matches. */
    public static final char ERROR_ON_MATCH_MODIFIER      = 'e';
    /** Adds a segments that specifies a message of a ERROR that will be created if the pattern mismatches. */
    public static final char ERROR_ON_MISMATCH_MODIFIER   = 'E';
    /** Adds a segments that specifies a message of a WARNING that will be created if the pattern matches. */
    public static final char WARNING_ON_MATCH_MODIFIER    = 'w';
    /** Adds a segments that specifies a message of a WARNING that will be created if the pattern mismatches. */
    public static final char WARNING_ON_MISMATCH_MODIFIER = 'W';
    /** Adds a segments that specifies a message of a INFORMATION that will be created if the pattern matches. */
    public static final char INFO_ON_MATCH_MODIFIER       = 'i';
    /** Adds a segments that specifies a message of a INFORMATION that will be created if the pattern mismatches. */
    public static final char INFO_ON_MISMATCH_MODIFIER    = 'I';

    //Valid flags
    /** Makes a <code>Regex</code> replace all occurrences of the pattern with the substitution. */
    public static final char WHOLE_LINE_FLAG              = 'g';
    /** Makes a <code>Regex</code> replace only the first occurrence of the pattern with the substitution. */
    public static final char ONLY_FIRST_FLAG              = 'G';
    /** Flags a <code>Regex</code> as modifiable. */
    public static final char MODIFIABLE_FLAG              = 'm';
    /** Flags a <code>Regex</code> as not modifiable. */
    public static final char UNMODIFIABLE_FLAG            = 'M';
    /**
     * Makes a <code>Regex</code> replace occurrences of a pattern with the substitution within a String
     * (everything within single (<code>'\''</code>) and double (<code>'"'</code>) quotes).
     */
    public static final char REPLACE_IN_STRING_FLAG       = 's';
    /**
     * Makes a <code>Regex</code> replace occurrences of a pattern with the substitution in everything but within a
     * String (everything within single (<code>'\''</code>) and double (<code>'"'</code>) quotes).
     */
    public static final char DO_NOT_REPLACE_IN_STRING_FLAG = 'S';
    /**
     * Makes the <code>Regex</code>'s <code>Pattern</code> case insensitive by using the
     * {@link Pattern#CASE_INSENSITIVE} flag.
     */
    public static final char CASE_INSENSITIVE_FLAG        = 'i';
    /**
     * Makes the <code>Regex</code>'s <code>Pattern</code> case sensitive by not using the
     * {@link Pattern#CASE_INSENSITIVE} flag.
     */
    public static final char CASE_SENSITIVE_FLAG          = 'I';


    /**
     * Constructs a <code>Regex</code>.<br>
     * The <code>Regex</code> will be compiled from a format.<br>
     * <br>
     * The format is divided into different segments by a <code>'/'</code>
     * (which can be escaped with a leading backslash (<code>'\\'</code>))<br>
     * E.g.: <code>first/2nd/3\/rd/forth</code>.<br>
     * <br>
     * Explanation of the different segments:<br>
     * <pre>
     *     s/(\d+)H/~hex-number~/I
     *     1   2         3       4
     *
     *     1 Modifiers
     *     2 Pattern
     *     3 Modifier-segment(s)
     *     4 Flags
     * </pre>
     *
     * <h1>1 Modifiers:</h1>
     * The first segment is always interpreted as the modifiers segment and describes the following
     * segments and how they are interpreted. Each segment type is described by a character.<br>
     * Possible modifiers:<br>
     * <table>
     *     <tr><th>Char</th><th>Name</th><th>Function</th></tr>
     *     <tr><td><code>s</code></td><td>Substitution</td><td>
     *         Specifies a segment which text will be used to substitute a match from the
     *         Pattern. Group References are supported. If multiple Substitution modifiers
     *         are given, the content of the last referenced segment will be used.
     *     </td></tr>
     *     <tr><td><code>e</code> and <code>E</code></td><td>Error on (mis)match</td><td>
     *         Specifies a segment which text will be used as the message of a newly created
     *         <code>Problem</code> with the type <code>ERROR</code> on a match (<code>e</code>)
     *         or mismatch <code>(E)</code>. If the <code>Problem</code> is created on a match,
     *         Group references are supported. Each occurrence of this modifier will result in
     *         a corresponding <code>Problem</code> even if it has occurred before.
     *     </td></tr>
     *     <tr><td><code>w</code> and <code>W</code></td><td>Warning on (mis)match</td><td>
     *         The same as 'e and E' but with <code>WARNING</code> as type of the
     *         <code>Problem</code>.
     *     </td></tr>
     *     <tr><td><code>i</code> and <code>I</code></td><td>Information on (mis)match</td><td>
     *         The same as 'e and E' but with <code>INFORMATION</code> as type of the
     *         <code>Problem</code>.
     *     </td></tr>
     * </table>
     * Note #1: The order of the modifier characters specifies how a segment is interpreted.<br> So
     *          the corresponding segment can be calculated by adding <code>2</code> to index
     *          (beginning with <code>0</code>) of the modifier character.<br>
     * Note #2: This segment has to be specified for the <code>Regex</code> to work properly.
     *
     * <h1>2 Pattern:</h1>
     * The Pattern of the <code>Regex</code>. This uses Java's {@link Pattern} API.<br>
     * Some special sequences that will be replaced with internal the pattern for different assembler
     * and operand types can be used. To use one of this classes just type <code>\T{<i>name</i>}</code>
     * (mind the capital <code>T</code>) with <code><i>name</i></code> being one of the following values:
     * <table>
     *      <tr><th>Name</th><th>Corresponding constant in {@link MC8051Library}</th></tr>
     *      <tr><td>number</td><td>NUMBER_PATTERN</td></tr>
     *      <tr><td>address</td><td>ADDRESS_PATTERN</td></tr>
     *      <tr><td>constant</td><td>CONSTANT_PATTERN</td></tr>
     *      <tr>negated_address</td><td>NEGATED_ADDRESS_PATTERN</td></tr>
     *      <tr>address_offset</td><td>ADDRESS_OFFSET_PATTERN</td></tr>
     *      <tr>bit_addressing</td><td>BIT_ADDRESSING_PATTERN</td></tr>
     *      <tr>symbol</td><td>SYMBOL_PATTERN</td></tr>
     *      <tr>label</td><td>LABEL_PATTERN</td></tr>
     *      <tr>name</td><td>SYMBOL_PATTERN</td></tr>
     *      <tr>mnemonic_name</td><td>MNEMONIC_NAME_PATTERN</td></tr>
     *      <tr>indirect_name</td><td>SYMBOL_INDIRECT_PATTERN</td></tr>
     *      <tr>indirect_symbol</td><td>SYMBOL_INDIRECT_PATTERN</td></tr>
     * </table>
     * Note: Like the modifier-segment the second segment will always be interpreted as the pattern of the
     *       <code>Regex</code> and is required for it to work properly.
     *
     * <h1>3 Modifier-segments:</h1>
     * All segments after the pattern segment will be interpreted as modifier segments as specified by the
     * modifier segment. The <code>Regex</code> expects a segment for every valid modifier in the modifier
     * segment.<br>
     * The behavior of the segment is specified by the corresponding modifier.
     *
     * <h2>Group Reference</h2>
     * Some modifiers allow Group References. Groups can be referenced by using the group's number preceding
     * with a <code>'$'</code>, <code>"\\"</code> (backslash) or <code>"\\g"</code>, e.g: <code>$1</code> to
     * reference the first group.<br>
     * Any prefix can be escaped with a backslash (<code>'\\'</code>) like <code>'\\$10'</code> also the number
     * can be separated from a literal number by surrounding it with curly brackets (<code>${1}2</code>).
     *
     * <h1>4 Flags</h1>
     * The optional segment after the last modifier segment will be interpreted as the flag segment. This
     * segments may contain flag characters. A uppercase letter always is the negation of a lowercase one
     * (<code>'I'</code> is the negation of <code>'i'</code>). The last flag character is always the one
     * that affects the <code>Regex</code>, e.g.: in <code>"iIiIIi"</code> only <code>'i'</code> will affect
     * the <code>Regex</code>.<table>
     *     <tr><th>Char</th><th>Name</th><th>Function</th><th>Default</th></tr>
     *     <tr><td><code>i</code></td><td>Case Insensitive</td><td>
     *         The <code>Pattern</code> of will be compiled with the {@link Pattern#CASE_INSENSITIVE} flag.</td>
     *         <td>X</td></tr>
     *     <tr><td><code>I</code></td><td>Case Sensitive</td><td>
     *     The <code>Pattern</code> of wont be compiled with the {@link Pattern#CASE_INSENSITIVE} flag.</td></tr>
     *     <tr><td><code>g</code></td><td>Replace all</td><td>
     *         Every occurrence of the pattern will be replaced with the substitution.
     *     </td><td>X</td></tr>
     *     <tr><td><code>G</code></td><td>Replace first</td><td>
     *         Only the first occurrence of the pattern will be replaced with the substitution.
     *     </td></tr>
     *     <tr><td><code>s</code></td><td>Replace in Strings</td><td>
     *         Tells the <code>Regex</code> to replace everything, including Strings which are any characters surrounded
     *         by simple (<code>'\''</code>) or double (<code>'"'</code>) quotes.</td></tr>
     *     <tr><td><code>S</code></td><td>Don't replace in Strings</td><td>
     *         Tells the <code>Regex</code> to replace everything, <u>except</u> Strings which are any characters
     *         surrounded by simple (<code>'\''</code>) or double (<code>'"'</code>) quotes.</td><td>X</td></tr>
     *     <tr><td><code>m</code></td><td>Modifiable</td><td>
     *         Marks this <code>Regex</code> as modifiable.</td><td>X</td></tr>
     *     <tr><td><code>M</code></td><td>Unmodifiable</td><td>
     *         Marks this <code>Regex</code> as unmodifiable and tells the processing unit not to replace it with
     *         a <code>Regex</code> with the same pattern.</td></tr>
     * </table>
     *
     * <h1>Ignoring</h1>
     * All trailing segments as well as invalid modifiers or flags will be ignored and don't count to the required
     * segment count, but may result in a <code>Problem</code>.
     *
     * @param format
     *      the format of the <code>Regex</code>.
     * @param problemFile
     *      the <code>Path</code> that will be used as argument in newly
     *      created <code>Problem</code>s.
     * @param problemFileLine
     *      the line that will be used as argument in newly created
     *      <code>Problem</code>s.
     * @param problemList
     *      the <code>List</code> newly created <code>Problem</code>s will
     *      be added to.
     *
     * @see #compile(String[])
     */
    public Regex(String format, Path problemFile, int problemFileLine, List<Problem> problemList) {
        Objects.requireNonNull(format, "'format' cannot be 'null'!");
        setProblemReport(problemFile, problemFileLine, problemList);
        modifier = new StringBuffer();
        substitution = null;

        String[] segments = format.split("(?<!(?<!\\\\)\\\\)/");
        for (int i = 0; i < segments.length; i++)
            segments[i] = segments[i].replaceAll("\\\\/", "/");
        compile(segments);
    }

    /**
     * Sets the data of created problems and their location.
     *
     * @param problemFile
     *      the <code>Path</code> that will be used as argument in newly
     *      created <code>Problem</code>s.
     * @param problemFileLine
     *      the line that will be used as argument in newly created
     *      <code>Problem</code>s.
     * @param problemList
     *      the <code>List</code> newly created <code>Problem</code>s will
     *      be added to.
     */
    public void setProblemReport(Path problemFile, int problemFileLine, List<Problem> problemList) {
        this.problemFile = Objects.requireNonNull(problemFile, "'File' cannot be 'null'.");
        this.problemFileLine = Objects.requireNonNull(problemFileLine, "'Line' cannot be 'null'.");
        this.problems = Objects.requireNonNull(problemList, "'Problems' cannot be 'null'.");

    }

    /**
     * Prepare all segments in the <code>Regex</code>.<br>
     * A regex at least needs a modifiers segment and a Pattern.<br>
     * The first segment is always interpreted as the modifiers-segment
     * and the second one is interpreted as the Pattern.<br>
     * All other segment types will be determined by the modifiers and
     * their order in the modifiers-segment. The first segment that is not
     * determined by a modifier in the modifier-segment will be interpreted
     * as the flags of the <code>Regex</code>.<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: no modifier and Pattern segment found.</li>
     *      <li><i>Problems created by the sub-prepare-methods.</i></li>
     * </ul>
     *
     * @param unprepared An array of the segments of the <code>Regex</code>.
     *
     * @return Whether all prepare-methods returned <code>true</code>.
     *
     * @see #compileModifiers(String)
     * @see #compilePattern(String)
     * @see #compileRemainingSegments(String[])
     * @see #compileFlags(String)
     */
    private boolean compile(final String[] unprepared) {
        final int length = unprepared.length;
        boolean result = true, flags = false;
        if (length < 2) {
            problems.add(new PreprocessingProblem("At least a modifier and a pattern segment expected!",
                    Problem.Type.ERROR, problemFile, problemFileLine, Arrays.toString(unprepared)));
            return false;
        }

        if (!compileModifiers(unprepared[0])) result = false;
        if (!compileRemainingSegments(unprepared)) result = false;

        if (length >= 2 + modifier.length()) {
            flags = true;
            if (!compileFlags(unprepared[2 + modifier.length()])) result = false;
        } if (!compilePattern(unprepared[1])) result = false;
        if (length > 2 + (flags ? 1 : 0) + modifier.length())
            MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(problemFile, problemFileLine,
                            Arrays.toString(Arrays.copyOfRange(unprepared, 2 + (flags ? 1 : 0) + modifier.length(),
                            unprepared.length))),
                    AssemblerSettings.UNNECESSARY_SEGMENTS, "Too many segments!", "Unnecessary segments.", problems);

        return result;
    }

    /**
     * Prepares the given modifiers by ignoring the invalid ones and generate
     * in special cases:
     * <ul>
     *      <li>ERROR: A modifier character is unknown.</li>
     *      <li>ERROR: Multiple substitution modifier were used.<br>
     *                 Note: The modifier will still be added, and the last one will be
     *                       used for substitution.</li>
     *      <li>ERROR/WARNING: Multiple Problem generating modifiers for the same
     *                 match case (Problem on match or Problem on mismatch) were
     *                 being used.<br>
     *                 Note #1: This case will only result in a Problem if the  Setting
     *                          {@link AssemblerSettings#MULTIPLE_SAME_MATCH_CASE} isn't
     *                          set to "ignore".<br>
     *                 Note #2: The modifier will still be added, and all modifiers will
     *                          result in the corresponding Problem.</li>
     *      <li>INFORMATION: One or more multiple Problem match cases occurred.</li>
     * </ul>
     * <br>
     * Because even a modifier that results in a Problem will be added to the modifiers
     * if it is known, the later called execution method demands at least as many segments
     * as specified by the modifiers.
     *
     * @param mods the modifiers to be precessed.
     *
     * @return whether no Problem occurred.
     */
    private boolean compileModifiers(String mods) {
        boolean ret = true;
        int positiveMsgMod = 0;
        int negativeMsgMod = 0;
        boolean substitution = false;

        int validModsCount = 0;
        final String validMods = "seEwWiI";

        for (int cp : mods.codePoints().toArray()) {
            if (validMods.indexOf(cp) == -1) {
                problems.add(new PreprocessingProblem("Unknown modifier!", Problem.Type.ERROR, problemFile,
                        problemFileLine, new String(Character.toChars(cp))));
                ret = false;
                continue;
            }

            if (substitution && cp == SUBSTITUTE_MODIFIER) {
                problems.add(new PreprocessingProblem("Multiple substitution modifiers!", Problem.Type.ERROR,
                        problemFile, problemFileLine, mods));
                ret = false;
            }

            if (!substitution && cp == SUBSTITUTE_MODIFIER)
                substitution = true;

            if (cp != SUBSTITUTE_MODIFIER) { // Must be a Problem modifier
                if (Character.isUpperCase(cp)) {
                    if (++negativeMsgMod > 1) {
                        MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(problemFile, problemFileLine,
                                 new String(Character.toChars(cp))),
                                AssemblerSettings.MULTIPLE_SAME_MATCH_CASE,
                                "More than one mismatch message!", "More than one mismatch message.", problems);
                        ret = false;
                    }
                } else {
                    if (++positiveMsgMod > 1) {
                        MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(problemFile, problemFileLine,
                                 new String(Character.toChars(cp))),
                                AssemblerSettings.MULTIPLE_SAME_MATCH_CASE,
                                "More than one match message!", "More than one match message.", problems);
                        ret = false;
                    }
                }
            }

            validModsCount++;
            modifier.appendCodePoint(cp);
        }

        if (!ret)
            problems.add(new PreprocessingProblem("Even if some Problems occurred, you have to write " +
                    validModsCount + 2 + " segments.", Problem.Type.INFORMATION, problemFile, problemFileLine, null));
        return ret;
    }


    /**
     * Prepares the given pattern by replacing the type escape sequences
     * with the actual regex provided by the {@link MC8051Library}.<br>
     * A type escape sequence is initiated by a <code>\T</code> (must be
     * capital) and then the name of the type surrounded by curly brackets.<br>
     * Example: <code>\T{number}</code><br>
     * Note: Surrounding white space is not supported and will result in a
     * mismatch.<br>
     * <br>
     * Supported escapes sequences:<br>
     * <table>
     *      <tr><th>Name</th><th>Corresponding constant in MC8051Library</th></tr>
     *      <tr><td>number</td><td>NUMBER_PATTERN</td></tr>
     *      <tr><td>address</td><td>ADDRESS_PATTERN</td></tr>
     *      <tr><td>constant</td><td>CONSTANT_PATTERN</td></tr>
     *      <tr>negated_address</td><td>NEGATED_ADDRESS_PATTERN</td></tr>
     *      <tr>address_offset</td><td>ADDRESS_OFFSET_PATTERN</td></tr>
     *      <tr>bit_addressing</td><td>BIT_ADDRESSING_PATTERN</td></tr>
     *      <tr>symbol</td><td>SYMBOL_PATTERN</td></tr>
     *      <tr>label</td><td>LABEL_PATTERN</td></tr>
     *      <tr>name</td><td>SYMBOL_PATTERN</td></tr>
     *      <tr>mnemonic_name</td><td>MNEMONIC_NAME_PATTERN</td></tr>
     *      <tr>indirect_name</td><td>SYMBOL_INDIRECT_PATTERN</td></tr>
     *      <tr>indirect_symbol</td><td>SYMBOL_INDIRECT_PATTERN</td></tr>
     * </table>
     * <br>
     * After the substitution the resulting pattern String will be used to
     * compile the match Pattern of this <code>Regex</code>.<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the pattern is invalid.</li>
     *      <li>ERROR: the escaped name is unknown.</li>
     * </ul>
     *
     * @param pattern the pattern that should be used.
     *
     * @return whether the preparation was successful.
     */
    private boolean compilePattern(final String pattern) {
        String result = pattern;

        try {
            result =
                    result.replaceAll("\\\\T\\{number}", MC8051Library.NUMBER_PATTERN.toString())
                            .replaceAll("\\\\T\\{address}", MC8051Library.ADDRESS_PATTERN.toString())
                            .replaceAll("\\\\T\\{constant}", MC8051Library.CONSTANT_PATTERN.toString())
                            .replaceAll("\\\\T\\{negated_address}", MC8051Library.NEGATED_ADDRESS_PATTERN.toString())
                            .replaceAll("\\\\T\\{address_offset}", MC8051Library.ADDRESS_OFFSET_PATTERN.toString())
                            .replaceAll("\\\\T\\{bit_addressing}", MC8051Library.BIT_ADDRESSING_PATTERN.toString())
                            .replaceAll("\\\\T\\{symbol}", MC8051Library.SYMBOL_PATTERN.toString())
                            .replaceAll("\\\\T\\{label}", MC8051Library.LABEL_PATTERN.toString())
                            .replaceAll("\\\\T\\{name}", MC8051Library.SYMBOL_PATTERN.toString())
                            .replaceAll("\\\\T\\{mnemonic_name}", MC8051Library.MNEMONIC_NAME_PATTERN.toString())
                            .replaceAll("\\\\T\\{indirect_name}", MC8051Library.SYMBOL_INDIRECT_PATTERN.toString())
                            .replaceAll("\\\\T\\{indirect_symbol}", MC8051Library.SYMBOL_INDIRECT_PATTERN.toString());

            Matcher m = Pattern.compile("\\\\T\\{(.*)}").matcher(result);
            while (m.find())
                problems.add(new PreprocessingProblem(m.group(1).trim().isEmpty() ? "No type name!" :
                        "Unknown type name!", Problem.Type.ERROR, problemFile, problemFileLine, m.group(1)));

            this.match = Pattern.compile(result, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            this.lowerCased = Regex.patternToLowercase(result);
            return true;

        } catch (PatternSyntaxException e) {
            problems.add(new PreprocessingProblem("Wrong regular expression syntax: " + e.getMessage(),
                    Problem.Type.ERROR, problemFile, problemFileLine, result));
        }
        return false;
    }

    /**
     * Prepares the given flags by ignoring invalid ones and adding them to the flags
     * field.<br>
     * All <code>MODIFIABLE-flags</code> except the last also will be ignored.
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: a flag character is unknown.</li>
     * </ul>
     *
     * @param flags the flags to be processed.
     *
     * @return <code>false</code> if one or more flags were unknown,
     *
     * <code>true</code> otherwise
     */
    private boolean compileFlags(String flags) {
        {
            final String defaults = Settings.INSTANCE.getProperty(AssemblerSettings.DEFAULT_FLAGS);
            if (defaults != null)
                flags = defaults + flags;
        }
        boolean result = true;

        for (int cp : flags.codePoints().toArray()) {

            switch (cp) {
                case ONLY_FIRST_FLAG:
                    global = false;
                    break;
                case WHOLE_LINE_FLAG:
                    global = true;
                    break;
                case CASE_SENSITIVE_FLAG:
                    caseSensitive = false;
                    break;
                case CASE_INSENSITIVE_FLAG:
                    caseSensitive = true;
                    break;
                case REPLACE_IN_STRING_FLAG:
                    replaceString = true;
                    break;
                case DO_NOT_REPLACE_IN_STRING_FLAG:
                    replaceString = false;
                    break;
                case MODIFIABLE_FLAG:
                    modifiable = true;
                    break;
                case UNMODIFIABLE_FLAG:
                    modifiable = false;
                    break;

                default:
                    problems.add(new PreprocessingProblem("Unknown flag!", Problem.Type.ERROR, problemFile,
                            problemFileLine, new String(Character.toChars(cp))));
                    result = false;
            }
        }

        return result;
    }

    /**
     * Prepares all segments that are not modifiers or the Pattern.<br>
     * The last substitution segment (indicated by the position of the substitution modifier
     * in the modifiers) will be set to the current substitution.
     * All other segments will be added to the <code>segments</code> field.<br>
     * Any remaining segments will be ignored.<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the segments have not enough elements for a modifier.<br>
     *                 Note: The invalid modifier(s) will be deleted.</li>
     * </ul>
     *
     * @param segments the segments to be used.<br>
     *                 The first two segments will be ignored because it is assumed
     *                 that they are the modifiers and the Pattern.
     *
     * @return whether all modifiers had been satisfied. with a segment
     */
    private boolean compileRemainingSegments(String[] segments) {
        List<String> segs = new LinkedList<>();

        final int[] mods = modifier.codePoints().toArray();

        for (int i = 0; i < mods.length; ++i) {
            if (2 + i > segments.length) {
                for (int j = i; j < mods.length; ++j)
                    problems.add(new PreprocessingProblem(new StringBuilder("Missing segment for modifier '")
                            .appendCodePoint(mods[j]).append("'!").toString(), Problem.Type.ERROR, problemFile,
                            problemFileLine, new String(Character.toChars(mods[j]))));
                modifier.setLength(2 + i);
                return false;
            }

            if (mods[i] == SUBSTITUTE_MODIFIER) {
                substitution = segments[2 + i];
                segs.add("");
            } else
                segs.add(segments[2 + i]);
        }

        // modifier = new StringBuffer(modifier.toString().replaceAll(String.valueOf(SUBSTITUTE_MODIFIER), ""));

        this.segments = segs.toArray(new String[segs.size()]);

        return true;
    }

    /**
     * Performs this regex on a target String.<br>
     * This may replaces substrings with the possibly specified substitution and
     * creates desired problems.<br>
     * Special options (defined via the flags) will also be taken care of.<br>
     * <br>
     * If the <code>DO_NOT_REPLACE_IN_STRING_FLAG</code> is used the string first will
     * be spilt and the not-string (quoted with <code>'"'</code> or <code>'\'</code>)
     * each of the line fragments will be used for processing. Because each substring
     * is threaded like a single target, for each of it a separated Problem will be
     * created.<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the matcher has not as many groups as desired by
     *                 the given value if substitution of group references is used.</li>
     * </ul>
     *
     * @param target the target String.
     *
     * @return the target String after processing.
     *
     * @see #replaceGroups(Matcher, String)
     */
    public String perform(String target) {
        if (match == null) return target;

        Objects.requireNonNull(target, "'Target' String cannot be 'null'!");
        Pattern p = MC8051Library.STRING_PATTERN;
        boolean matched = false;
        String[] substrings = replaceString ? new String[]{target} : p.split(target);

        for (int i = 0; i < substrings.length; ++i) {
            Matcher m = this.match.matcher(substrings[i]);
            if (m.find()) {
                if (global || !global && !matched) {
                    final int[] cps = this.modifier.codePoints().toArray();
                    for (int j = 0; j < cps.length; ++j)
                        switch (cps[j]) {
                            case ERROR_ON_MATCH_MODIFIER:
                                createProblem(m, Problem.Type.ERROR, segments[j], target);
                                break;
                            case WARNING_ON_MATCH_MODIFIER:
                                createProblem(m, Problem.Type.WARNING, segments[j], target);
                                break;
                            case INFO_ON_MATCH_MODIFIER:
                                createProblem(m, Problem.Type.INFORMATION, segments[j], target);
                                break;
                        }
                }
                if (substitution != null)
                    if (global)
                        substrings[i] = m.replaceAll(replaceGroups(m, substitution)
                                .replaceAll("\\$", "\\\\$")).replaceAll("\\\\$", "\\$");
                                // Prevent native group replacement of Matcher
                    else if (!matched)
                        substrings[i] = m.replaceFirst(replaceGroups(m, substitution)
                                .replaceAll("\\$", "\\\\$")).replaceAll("\\\\$", "\\$");

                matched = true;
            }
        }
        if (!matched) {
            final int[] cps = this.modifier.codePoints().toArray();
            for (int j = 0; j < cps.length; ++j)
                switch (cps[j]) {
                    case ERROR_ON_MISMATCH_MODIFIER:
                        createProblem(null, Problem.Type.ERROR, segments[j], target);
                        break;
                    case WARNING_ON_MISMATCH_MODIFIER:
                        createProblem(null, Problem.Type.WARNING, segments[j], target);
                        break;
                    case INFO_ON_MISMATCH_MODIFIER:
                        createProblem(null, Problem.Type.INFORMATION, segments[j], target);
                        break;
                }
        }

        Matcher m = p.matcher(target);
        StringBuilder result = new StringBuilder(target.length());
        boolean found = m.find();
        if (found && !replaceString) {
            final Iterator<String> betw = Arrays.asList(substrings).listIterator();
            do {
                result.append(betw.next()).append(m.group());
                found = m.find();
            } while (found);
            if (betw.hasNext())
                result.append(betw.next());
        } else
            result.append(substrings[0]);

        return result.toString();
    }

    /**
     * Performs this regex on a target String.<br>
     * This may replaces substrings with the possibly specified substitution and
     * creates desired problems.<br>
     * Special options (defined via the flags) will also be taken care of.<br>
     * <br>
     * If the <code>DO_NOT_REPLACE_IN_STRING_FLAG</code> is used the string first will
     * be spilt and the not-string (quoted with <code>'"'</code> or <code>'\'</code>)
     * each of the line fragments will be used for processing. Because each substring
     * is threaded like a single target, for each of it a separated Problem will be
     * created.<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the matcher has not as many groups as desired by
     *                 the given value if substitution of group references is used.</li>
     * </ul>
     *
     * @param target the target String.
     * @param problemFile
     *      the <code>Path</code> that will be used as argument in newly
     *      created <code>Problem</code>s.
     * @param problemFileLine
     *      the line that will be used as argument in newly created
     *      <code>Problem</code>s.
     * @param problemList
     *      the <code>List</code> newly created <code>Problem</code>s will
     *      be added to.
     *
     * @return the target String after processing.
     *
     * @see #replaceGroups(Matcher, String)
     */
    public String perform(String target, Path problemFile, int problemFileLine, List<Problem> problemList) {
        setProblemReport(problemFile, problemFileLine, problemList);
        return perform(target);
    }

    /**
     * Creates a new Problem from a given Type and message and adds it to the
     * Problem List of the Preprocessor.<br>
     * Group notation as specified in {@link #replaceGroups(Matcher, String)}
     * can be used to insert groups of the matcher at a specific location.<br>
     * For a detailed documentation {@link #replaceGroups(Matcher, String)}.<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the matcher has not as many groups as desired by
     *                 the given value.</li>
     * </ul>
     *
     * @param matcher the Matcher (with the input String) that should be used to get
     *                the desired groups.<br>
     *                If this value is <code>null</code>, the message wont be altered.
     * @param type    the desired type of the created Problem.
     * @param message the message of the Problem.
     * @param line    the line of a file that was matched
     *
     * @see #replaceGroups(Matcher, String)
     */
    private void createProblem(Matcher matcher, final Problem.Type type, final String message, final
    String line) {
        String result;
        if (matcher == null)
            result = message;
        else
            result = replaceGroups(matcher, message);

        problems.add(new PreprocessingProblem(result, type, problemFile, this.problemFileLine,
                matcher != null ? matcher.group() : line));
    }

    /**
     * Replaces all specified group references in an with the corresponding
     * group of a <code>Matcher</code>.<br>
     * <br>
     * It is possible to specify groups of the matched String that will be
     * inserted at the desired locations.
     * This is possible by use the group notation of Java and Pearl, i.e. using
     * <code>'$'</code>, <code>'\'</code> or <code>'\g'</code> as a prefix and
     * a trailing number to specify the group.<br>
     * Example:
     * <pre>
     *     Matcher regex:     ".*?(\d+?).*"
     *     Matcher input:     "It's 42, baby!"
     *     Message:           "The answer is $1!"
     *     Resulting message: "The answer is 42!"
     * </pre>
     * <br>
     * The group number can be separated from a directly following literal number
     * by surrounding it with curly brackets.<br>
     * Example: <code>"${4}2"</code><br>
     * Also the group specifier can be escaped with a leading <code>'\'</code>.<br>
     * Example: <code>"Cost \$42."</code>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the matcher has not as many groups as desired by
     *                 the given value.</li>
     * </ul>
     *
     * @param matcher the Matcher (with the input String) that should be used to get
     *                the desired groups.
     * @param string  the string to processed.
     */
    private String replaceGroups(Matcher matcher, String string) {
        Objects.requireNonNull(matcher, "'Matcher' cannot be 'null'!");
        Objects.requireNonNull(string, "'String' cannot be 'null'!");

        final Pattern p = Pattern.compile("(?<!\\\\)(?:\\$|\\\\g?)(?:(\\d+)|\\{(\\d+)})");
        final Matcher m = p.matcher(string);
        StringBuilder result = new StringBuilder(string.length());

        // Slight variation of the 'replaceAll()' method in 'Matcher'
        boolean found = m.find();
        if (found) {
            int group;
            final Iterator<String> betw = Arrays.asList(p.split(string)).listIterator();

            do {
                if (m.group(1) != null)
                    group = Integer.parseInt(m.group(1));
                else
                    group = Integer.parseInt(m.group(2));
                if (group > matcher.groupCount()) {
                    problems.add(new PreprocessingProblem("Value of desired group bigger than group count! " +
                            "(" + group + " > " + matcher.groupCount() + ")", Problem.Type.ERROR, problemFile,
                            problemFileLine, m.group()));
                    result.append(betw.next());
                    // m.appendReplacement(result, "");
                } else
                    result.append(betw.next()).append(matcher.group(group));
                    // m.appendReplacement(result, matcher.group(group));
                found = m.find();
            } while (found);
            if (betw.hasNext())
                result.append(betw.next());
            // m.appendTail(result);
        } else
            result.append(string);
        return result.toString().replaceAll("\\\\(\\$|\\\\g?(?:\\d+|\\{\\d+}))", "$1");
    }

    /**
     * @return
     *      whether this <code>Regex</code> is modifiable or not.<br>
     *      Use {@link #MODIFIABLE_FLAG} to set and {@link #UNMODIFIABLE_FLAG}
     *      to unset.
     */
    public boolean isModifiable() {
        return modifiable;
    }

    /**
     * @return
     *      whether the internal pattern isn't <code>null</code> and
     *      the <code>Regex</code> has at least one modifier segment.
     */
    public boolean isValid() {
        return match != null && segments.length > 0;
    }

    /**
     * @param obj
     *      the other Object to be tested.
     * @return
     *      <code>true</code> if <code>obj</code> is a <code>Regex</code> and the
     *      internal <code>Pattern</code>s use the same pattern.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Regex)) return false;

        Regex other = (Regex) obj;

        if (this.match == null || other.match == null) return false;

        if (this.caseSensitive && other.caseSensitive)
            return match.pattern().equals(other.match.pattern());
        else
            return lowerCased.equals(other.lowerCased);
            // If one or both of them are case insensitive
            // they will cover the same cases
    }

    /**
     * Lowercases all characters in a pattern that aren't needed for
     * a character class or similar.<br>
     *
     * @param pattern
     *      the pattern as a String to be lowercased.
     * @return
     *      the lowercased pattern.
     */
    private static String patternToLowercase(final String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int last = 0, beforeLast = 0;
        boolean escape = false;
        for (int cp : pattern.codePoints().toArray()) {
            if (beforeLast == '\\' && last == 'p') // POSIX character class names
                escape = true;                     // are case sensitive so they
            else if (escape && cp == '}')          // have to stay untouched.
                escape = false;

            if (last == '\\' || escape)
                sb.appendCodePoint(cp);
            else
                sb.appendCodePoint(Character.toLowerCase(cp));

            beforeLast = last; last = cp;
        }
        return sb.toString();
    }
}
