package assembler.arc8051;

import assembler.Preprocessor;
import assembler.util.AssemblerSettings;
import assembler.util.Regex;
import assembler.util.assembling.Directive;
import assembler.util.problems.ExceptionProblem;
import assembler.util.problems.PreprocessingProblem;
import assembler.util.problems.Problem;
import misc.Settings;
import simplemath.SimpleMath;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

/**
 * Preprocesses input for assembly language written for the
 * 8051 family.
 *
 * @author Jannik
 */
public class Preprocessor8051 implements Preprocessor {

    private List<Problem> problems;

    private Path currentFile;
    private int line;

    private List<Regex> regexes;
    private HashMap<Path, Integer> included;

    private List<String> output;
    private int outputIndex;

    private byte endState; // 1: Running, 0: End Reached, -1: End Problem created

    private final Directive[] directives = {
            new Directive("file", true) {
                @Override
                public boolean perform(String... args) {
                    if (args.length < 1) {
                        problems.add(new PreprocessingProblem("Expected at least one argument for 'file' directive!",
                                Problem.Type.ERROR, currentFile, line, null));
                        return false;
                    } else {
                        final String fileString = args[0];
                        try {
                            Path newPath = Paths.get(fileString);
                            line = 0;

                            if (args.length > 2)
                                problems.add(new PreprocessingProblem("Too many arguments for 'file' directive!",
                                        Problem.Type.WARNING, currentFile, line,
                                        Arrays.toString(Arrays.copyOfRange(args, 2, args.length))));

                            if (args.length > 1)
                                directives[1].perform(args[1]);

                            currentFile = newPath;
                            return true;
                        } catch (InvalidPathException e) {
                            problems.add(new PreprocessingProblem("Invalid Path!: " + e.getMessage(), Problem.Type.ERROR,
                                    currentFile, line, fileString));
                            return false;
                        }
                    }
                }
            },

            new Directive("line", true) {
                @Override
                public boolean perform(String... args) {
                    if (args.length < 1) {
                        problems.add(new PreprocessingProblem("Expected at least one argument for 'line' directive!",
                                Problem.Type.ERROR, currentFile, line, null));
                        return false;
                    } else {
                        String number = args[0];
                        try {

                            final boolean relative = number.startsWith("~");

                            if (relative) number = number.substring(1);

                            int newLine = Integer.parseInt(number);

                            if (relative) {
                                if (line - newLine < 1) {
                                    line = 0;
                                    problems.add(new PreprocessingProblem("Resulting line number cannot be smaller than 1!",
                                            Problem.Type.ERROR, currentFile, line, number));
                                    return false;
                                } else
                                    line += --newLine;
                            } else {
                                if (newLine < 1) {
                                    problems.add(new PreprocessingProblem("New line number cannot be smaller than 1!",
                                            Problem.Type.ERROR, currentFile, line, number));
                                    return false;
                                } else
                                    line = --newLine;
                            }
                            if (args.length > 1)
                                problems.add(new PreprocessingProblem("Too many arguments for 'line' directive!",
                                        Problem.Type.WARNING, currentFile, line,
                                        Arrays.toString(Arrays.copyOfRange(args, 1, args.length))));
                            return true;

                        } catch (NumberFormatException e) {
                            problems.add(new PreprocessingProblem("Illegal number format!", Problem.Type.ERROR,
                                    currentFile, line, number));
                            return false;
                        }
                    }
                }
            },

            new Directive("end") {
                @Override
                public boolean perform(String... args) {
                    if (endState > 0)
                        endState = 0;
                    if (args.length > 0)
                        problems.add(new PreprocessingProblem("Too many arguments for 'end' directive!",
                                Problem.Type.WARNING, currentFile, line,
                                Arrays.toString(args)));
                    return true;
                }
            },

            new Directive("include") {
                @Override
                public boolean perform(String... args) {
                    if (args.length < 1) {
                        problems.add(new PreprocessingProblem("Expected at least one argument for 'include' directive!",
                                Problem.Type.ERROR, currentFile, line, null));
                        return false;
                    }

                    final String targetFile = args[0];
                    try {
                        Path target = null;
                        if (targetFile.codePointAt(1) == '<' && targetFile.codePointAt(targetFile.length()-1) == '>') {
                            String[] dirs = Settings.INSTANCE.getProperty(AssemblerSettings.INCLUDE_PATH)
                                    .split("(?<!\\\\);");
                            boolean recursiveSearch = Settings.INSTANCE.getBoolProperty(
                                    AssemblerSettings.INCLUDE_RECURSIVE_SEARCH);
                            for (String dir : dirs) {
                                dir = dir.replaceAll("\\\\;", ";");

                                if (recursiveSearch) {
                                    Path dirPath = Paths.get(dir);
                                    if (!Files.exists(dirPath)) {
                                        problems.add(new PreprocessingProblem("Include path does not exist!",
                                                Problem.Type.ERROR, currentFile, line, dir));
                                        continue;
                                    }

                                    target = findFile(dirPath, targetFile);
                                    if (target != null)
                                        break;
                                } else
                                    target = Paths.get(dir, targetFile);
                            }
                            if (target == null || Files.exists(target)) {
                                problems.add(new PreprocessingProblem("No matching file found!", Problem.Type.ERROR,
                                        currentFile, line, targetFile));
                                return false;
                            }
                        } else {
                            target = Paths.get(args[0]);
                            if (!Files.exists(target)) {
                                problems.add(new PreprocessingProblem("File to include does not exist!",
                                        Problem.Type.ERROR, currentFile, line, target.toString()));
                                return false;
                            }
                        }

                        try {  // Check for recursive inclusion.
                            if (Files.isSameFile(target, currentFile)) {
                                final int maxRecursive = Settings.INSTANCE.getIntProperty(
                                        AssemblerSettings.INCLUDE_RECURSIVE_DEPTH, x -> x >= 0);
                                if (maxRecursive == 0) {
                                    problems.add(new PreprocessingProblem("Recursive including is deactivated!",
                                            Problem.Type.ERROR, currentFile, line, target.toString()));
                                    return false;
                                }
                                for (Path p : included.keySet())
                                    if (Files.isSameFile(p, target)) {
                                        final int times = included.get(p);
                                        if (maxRecursive <= times) {
                                            problems.add(new PreprocessingProblem("Maximal recursion depth reached!",
                                                    Problem.Type.ERROR, currentFile, line, target.toString()));
                                            return false;
                                        } else
                                            included.put(p, times+1);
                                        break;
                                    } else
                                        included.put(currentFile, 1); // File has will be included the second time.
                            } else
                                for (Path p : included.keySet())
                                    if (Files.isSameFile(p, currentFile)) {
                                        included.put(p, 0); // Reset recursive value.
                                        break;
                                    }
                        } catch (IOException | SecurityException e) {
                            problems.add(new ExceptionProblem("Could not check for recursive inclusion!",
                                    Problem.Type.ERROR, currentFile, line, e));
                        }

                        List<String> fileContent = readFile(target);

                        if (fileContent == null)
                            return false;
                        else {
                            boolean fileEmpty = true;
                            for (String l : fileContent)
                                if (!l.trim().isEmpty()) fileEmpty = false;
                            if (fileEmpty)
                                problems.add(new PreprocessingProblem("Included file is empty.", Problem.Type.WARNING,
                                        currentFile, line, target.toString()));

                            output.add(++outputIndex, "$file \"" + target.toString() + "\"");
                            output.addAll(++outputIndex, fileContent);
                            output.add(outputIndex+=fileContent.size()+1, "$file \"" + target.toString() + "\" " + line+1);


                            if (args.length > 1)
                                problems.add(new PreprocessingProblem("Too many arguments for 'include' directive!",
                                        Problem.Type.WARNING, currentFile, line,
                                        Arrays.toString(Arrays.copyOfRange(args, 1, args.length))));
                            return true;
                        }
                    } catch (InvalidPathException e) {
                        problems.add(new PreprocessingProblem("Invalid Path!: " + e.getMessage(), Problem.Type.ERROR,
                                currentFile, line, targetFile));
                        return false;
                    }
                }
            },

            new Directive("org", true) {
                @Override
                public boolean perform(String... args) {
                    if (args.length < 1) {
                        problems.add(new PreprocessingProblem("Expected at least 1 argument for 'org' directive!",
                                Problem.Type.ERROR, currentFile, line, Arrays.toString(args)));
                        return false;
                    }

                    if (!MC8051Library.NUMBER_PATTERN.matcher(args[1]).matches()) {
                        problems.add(new PreprocessingProblem("First argument of 'org' is not a valid number!",
                                Problem.Type.ERROR, currentFile, line, args[1]));
                        return false;
                    } else {
                        try {
                            int value = Integer.parseInt(args[1]);
                            if (value > 0xFFFF) {
                                problems.add(new PreprocessingProblem("Number too big! Value cannot be bigger than " +
                                        "0FFFFh!",
                                        Problem.Type.ERROR, currentFile, line, args[1]));
                                return false;
                            }
                        } catch (NumberFormatException e) {
                            problems.add(new PreprocessingProblem("Second argument of 'org' is not a valid number!",
                                    Problem.Type.ERROR, currentFile, line, args[1]));
                            return false;
                        }
                    }

                    if (args.length > 1)
                        problems.add(new PreprocessingProblem("Too many arguments for 'org' directive!",
                                Problem.Type.WARNING, currentFile, line,
                                Arrays.toString(Arrays.copyOfRange(args, 1, args.length))));
                    return true;
                }
            },

            new Directive("code") {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFFFF);
                }
            },

            new Directive("xdata") {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFFFF);
                }
            },

            new Directive("bit") {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFF);
                }
            },

            new Directive("data") {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFF);
                }
            },

            new Directive("idata") {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFF);
                }
            },

            new Directive("equ") {
                @Override
                public boolean perform(String... args) {
                    boolean result = true;
                    if (args.length < 2) {
                        problems.add(new PreprocessingProblem("Expected at least 2 arguments for 'equ' directive!",
                                Problem.Type.ERROR, currentFile, line, Arrays.toString(args)));
                        return false;
                    }

                    if (!MC8051Library.SYMBOL_PATTERN.matcher(args[0]).matches()) {
                        problems.add(new PreprocessingProblem("First argument of 'equ' is not a valid symbol!",
                                Problem.Type.ERROR, currentFile, line, args[0]));
                        result = false;
                    }

                    if (!MC8051Library.NUMBER_PATTERN.matcher(args[1]).matches() &&
                        !MC8051Library.BIT_ADDRESSING_PATTERN.matcher(args[1]).matches()) {
                        problems.add(new PreprocessingProblem("Expected a valid number or bit addressing as second " +
                                "argument of 'equ'!", Problem.Type.ERROR, currentFile, line, args[1]));
                        return false;
                    }

                    if (!result) return false;

                    if (!regexFromSymbol(args[0].toLowerCase(), args[1].toLowerCase(),
                            false, true)) return false;
                    return true;
                }
            },

            new Directive("set") {
                @Override
                public boolean perform(String... args) {
                    boolean result = true;
                    if (args.length < 2) {
                        problems.add(new PreprocessingProblem("Expected at least 2 arguments for 'set' directive!",
                                Problem.Type.ERROR, currentFile, line, Arrays.toString(args)));
                        return false;
                    }

                    if (!MC8051Library.SYMBOL_PATTERN.matcher(args[0]).matches()) {
                        problems.add(new PreprocessingProblem("First argument of 'set' is not a valid symbol!",
                                Problem.Type.ERROR, currentFile, line, args[0]));
                        result = false;
                    }

                    if (!MC8051Library.NUMBER_PATTERN.matcher(args[1]).matches() &&
                            !MC8051Library.BIT_ADDRESSING_PATTERN.matcher(args[1]).matches()) {
                        problems.add(new PreprocessingProblem("Expected a valid number or bit addressing as second " +
                                "argument of 'set'!", Problem.Type.ERROR, currentFile, line, args[1]));
                        return false;
                    }

                    if (!result) return false;

                    if (!regexFromSymbol(args[0].toLowerCase(), args[1].toLowerCase(),
                            true, true)) return false;
                    return true;
                }
            },
    };

    public Preprocessor8051() {
        problems = new LinkedList<>();
        output = new ArrayList<>(50);
        regexes = new LinkedList<>();
        included = new HashMap<>(8);
    }

    @Override
    public List<? extends Problem> preprocess(Path file, List<String> output) {
        problems.clear();
        regexes.clear();
        this.output.clear();
        currentFile = null;
        try {
            {
                List<String> tmp = readFile(file);
                if (tmp == null)
                    return problems;
                else {
                    this.output = tmp;
                    currentFile = file;
                }
            }

            line = 0;
            endState = 1;

            String lineString = null, original;
            output.add("$file \"" + currentFile.toString() + "\""); // Add directive for main source file
            // for Tokenizer and Assembler
            for (outputIndex = 1; outputIndex < output.size(); ++outputIndex) {
                this.line++;
                lineString = original = output.get(outputIndex);

                if (endState > 0) {

                    for (Regex regex : regexes)
                        lineString = regex.perform(lineString,    // Perform all registered regular expressions
                                currentFile, line, problems);     // on the current line.

                    lineString = cutComment(lineString);          // Cut comments

                    lineString = convertNumbers(lineString);      // Convert any numbers into the decimal system

                    // TODO: Evaluate mathematical expressions with SimpleMath

                    if (MC8051Library.DIRECTIVE_PATTERN.matcher(lineString).matches())
                        lineString = handleDirective(lineString); // Line is a directive: handle it
                    else
                        lineString = lowerCase(lineString);       // Only convert lines to lowercase if they
                    // are not a directive because fallthrough
                    // directives may be case sensitive.

                    output.add(lineString);
                } else if (!lineString.split(";", 2)[0].trim().isEmpty() && endState == 0) {
                    // If the line contains more than just comments or white space
                    // and no Problem has been created yet
                    MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(currentFile, this.line, lineString),
                            AssemblerSettings.END_CODE_AFTER, "No code allowed after use of 'end' directive!",
                            "All code after an 'end' directive will be ignored.", problems);
                    endState = -1;
                }

            }

            if (endState > 0)
                MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(currentFile, this.line, lineString),
                        AssemblerSettings.END_MISSING, "'end' directive not found!", "Missing 'end' directive!",
                        problems);


            output.addAll(this.output);
            this.output.clear();
        } catch (OutOfMemoryError e) {
            output.clear();
            System.gc(); // Invoke Garbage Collector to get enough memory for the new Problem (hopefully...).
            problems.add(new PreprocessingProblem("Out of memory!", Problem.Type.ERROR, currentFile, line, null));
        }
        return problems;
    }

    private String handleDirective(final String line) {
        Matcher m = MC8051Library.DIRECTIVE_PATTERN.matcher(line);
        if (m.matches()) {

            return "";
        } else
            return line;
    }


    private List<String> readFile(Path file) {
        Objects.requireNonNull(file, "'file' cannot be 'null'!");

        if (Files.isRegularFile(file)) {
           problems.add(new PreprocessingProblem("Given Path is not a regular file.", Problem.Type.ERROR, currentFile,
                   currentFile == null ? -1 : line, file.toString()));
            return null;
        }
        if (Files.isReadable(file)) {
            problems.add(new PreprocessingProblem("File is not readable.", Problem.Type.ERROR, currentFile,
                    currentFile == null ? -1 : line, file.toString()));
            return null;
        }

        try (Stream<String> stream = Files.lines(file)){
            return Arrays.asList((String[]) stream.toArray());
        } catch (IOException e) {
            problems.add(new ExceptionProblem("Unable to read file: \"" + file + "\"", Problem.Type.ERROR, currentFile,
                    currentFile == null ? -1 : line, e));
            return null;
        }
    }

    /**
     * Finds a specific file by name in a start Path and searches through
     * its sub-directories.<br>
     *
     * @param start
     *      the path that is used as the start point for the search.
     * @param file
     *      the desired file by name as a String.<br>
     *      If the file name does not start with the file system's
     *      default path separator (<code>'/'</code> for unix systems and
     *      <code>'\\'</code> for windows systems) it will be added.
     * @return
     *      the desired file as Path if it was found or <code>null</code>
     *      if not.
     */
    private Path findFile(Path start, String file) {
        Objects.requireNonNull(start, "Start Path cannot be 'null'!");
        Objects.requireNonNull(file, "File String cannot be 'null'!");

        Path[] result = new Path[1];
        try {
            final String fileName = !file.startsWith(FileSystems.getDefault().getSeparator()) ?
                FileSystems.getDefault().getSeparator().concat(file) : file;


            Files.walkFileTree(start, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile())
                        if (file.toString().endsWith(fileName)) {
                            result[0] = file;
                            return FileVisitResult.TERMINATE;
                        }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            return null;
        }
        return result[0];
    }


    private String evaluate() {
        SimpleMath.evaluate("");
        return null;
    }

    /**
     * Coverts every occurrence of a valid number in a String to
     * the decimal system.
     *
     * @param source
     *      the String to be used.
     *
     * @return
     *      the modified source String.
     *
     * @see #getNumber(String)
     */
    private String convertNumbers(final String source) {
        StringBuffer sb = new StringBuffer(source.length());

        Matcher m = MC8051Library.NUMBER_PATTERN.matcher(source);

        // Variation of 'replaceAll()' in Matcher
        boolean found;
        if (found = m.find()) {
            do {
                final String number = getNumber(m.group());
                m.appendReplacement(sb, getNumber(number == null ? "0" : number));
            } while (found = m.find());
            return m.appendTail(sb).toString();
        }

        return source;
    }

    /**
     * Coverts a number from a given match to a decimal number and
     * returns it as a String (so it can be of the value <code>null</code>).<br>
     * <br>
     * Possible sources of Problems:
     * <ul>
     *      <li>ERROR: the number uses digits that are not used in the number
     *                 system specified.</li>
     * </ul>
     * @param number
     *      the number that should be converted.<br>
     *      The number system will be obtained from the number's suffix if it
     *      has any.<br>
     *      Possible number systems:<br>
     *      <table>
     *          <tr><th>Suffix</th><th>Name</th><th>Base</th><th>Valid Digits</th></tr>
     *          <tr><td>h</td><td>Hexadecimal</td><td>16</td><td>0-9a-f</td></tr>
     *          <tr><td>d or none</td><td>Decimal</td><td>10</td><td>0-9</td></tr>
     *          <tr><td>o or q</td><td>Octal</td><td>8</td><td>0-8</td></tr>
     *          <tr><td>b</td><td>Binary</td><td>2</td><td>0-1</td></tr>
     *
     *          <tr><th>Prefix</th><th>Name</th><th>Base</th><th>Valid Digits</th></tr>
     *          <tr><td>0x</td><td>Hexadecimal</td><td>16</td><td>0-9a-f</td></tr>
     *      </table>
     *      <br>
     *      If the number contains digits that are illegal for its number
     *      system the number is considered invalid.
     * @return
     *      the value of the String as a decimal String or <code>null</code>
     *      if the String is invalid.
     *
     * @see MC8051Library#NUMBER_PATTERN
     */
    private String getNumber(final String number) {
        String result = null;
        String numberSystem = "MISSINGNO-SYSTEM";      // Should be overwritten. (Did you get the joke?)

        try {
            if (number.startsWith("0x")) {
                numberSystem = "HEXADECIMAL";

                result = Integer.toString(Integer.parseInt(number.substring(2), 16));
            } else {
                final char postfix = number.charAt(number.length()-1);
                // All postfixes are taken from Asem-51
                // Reference: http://plit.de/asem-51/constant.htm
                switch (postfix) {
                    case 'B':
                    case 'b': {   // In Asem-51 'b' is used to indicate binary numbers
                        numberSystem = "BINARY";
                        final int val = Integer.parseInt(number.substring(0, number.length()-1), 2);
                        result = Integer.toString(val);
                        break;
                    }
                    case 'Q':
                    case 'q':
                    case 'O':
                    case 'o': {   // In Asem-51 'o' or 'q' are used to indicate octal numbers
                        numberSystem = "OCTAL";
                        final int val = Integer.parseInt(number.substring(0, number.length()-1), 8);
                        result = Integer.toString(val);
                        break;
                    }
                    case 'H':
                    case 'h': {   // In Asem-51 'h' is used to indicate hexadecimal numbers
                        numberSystem = "HEXADECIMAL";
                        final int val = Integer.parseInt(number.substring(0, number.length()-1), 16);
                        result = Integer.toString(val);
                        break;
                    }
                    case 'D':
                    case 'd': {   // In Asem-51 'd' or missing prefixes is used to indicate decimal numbers
                        numberSystem = "DECIMAL";
                        final int val = Integer.parseInt(number.substring(0, number.length()-1), 10);
                        result = Integer.toString(val);
                        break;
                    }
                    default: {   // No known postfix recognised, assuming decimal
                        numberSystem = "DECIMAL";
                        final int val = Integer.parseInt(number, 10);
                        result = Integer.toString(val);
                    }
                }
            }
        } catch (NumberFormatException e) {
            problems.add(new PreprocessingProblem("Illegal digits in " + numberSystem + " number.",
                    Problem.Type.ERROR, currentFile, line, number));
        }
        return result;
    }

    private String cutComment(String line) {

        StringBuilder result = new StringBuilder(line.length());
        boolean simpQuoted = false, doubQuoted = false;

        int last = 0;
        for (int cp : line.codePoints().toArray()) {
            if (last == '\\') {
                if (cp == ';')
                    result.setLength(result.length()-1); // Cut '\\' else keep it
                result.appendCodePoint(cp);
            } else {
                if (cp == '"' && !simpQuoted)
                    doubQuoted = !doubQuoted;
                else if (cp == '\'' && !doubQuoted)
                    simpQuoted = !simpQuoted;

                if (!(simpQuoted || doubQuoted) && cp == ';')
                    return result.toString();
                else
                    result.appendCodePoint(cp);
            }
            last = cp;
        }
        return result.toString();
    }
    /**
     * Turns every character into its lowercase representation if possible.<br>
     * The character wont be lowercased ignored if it's quoted in <code>'"'</code> or
     * <code>'\''</code>. A quoted character can be escaped with a <code>'\'</code>.<br>
     * <br>
     * Possible sources of Problems:<br>
     * <ul>
     *      <li>WARNING: a quote is not closed if the end of the line or
     *                   a comment is reached.</li>
     * </ul>
     *
     * @param line the line that should be lowercased.
     *
     * @return
     *      a String that is a lowercased representation of the given one
     *      with possible comments removed.
     */
    private String lowerCase(String line) {

        StringBuilder result = new StringBuilder(line.length());
        boolean simpQuoted = false, doubQuoted = false;

        int last = 0;
        for (int cp : line.codePoints().toArray()) {
            if (last == '\\')
                result.appendCodePoint(cp);
            else if (cp != '\\') {
                if (cp == '"' && !simpQuoted)
                    doubQuoted = !doubQuoted;
                else if (cp == '\'' && !doubQuoted)
                    simpQuoted = !simpQuoted;

                result.appendCodePoint(!(doubQuoted || simpQuoted) ?
                        Character.toLowerCase(cp) : cp);
            }
            last = cp;
        }
        if (doubQuoted || simpQuoted)
            problems.add(new PreprocessingProblem("Unclosed quote!",
                    Problem.Type.WARNING, currentFile, this.line, line));
        return result.toString();
    }

    private boolean mcuCompatibilityDirective(final String[] args, final String directiveName, final int maxValue) {
        // Implemented for compatibility with Asem-51's MCU files
        // Does not have a particular segment type.
        boolean result = true;
        if (args.length < 2) {
            problems.add(new PreprocessingProblem("Expected at least 2 arguments for '"+directiveName+"' directive!",
                    Problem.Type.ERROR, currentFile, line, Arrays.toString(args)));
            return false;
        }

        if (!MC8051Library.SYMBOL_PATTERN.matcher(args[0]).matches()) {
            problems.add(new PreprocessingProblem("First argument of '"+directiveName+"' is not a valid symbol!",
                    Problem.Type.ERROR, currentFile, line, args[0]));
            result = false;
        }
        if (!MC8051Library.NUMBER_PATTERN.matcher(args[1]).matches()) {
            problems.add(new PreprocessingProblem("Second argument of '"+directiveName+"' is not a valid number!",
                    Problem.Type.ERROR, currentFile, line, args[1]));
            return false;
        } else {
            try {
                int value = Integer.parseInt(args[1]);
                if (value > maxValue) {
                    problems.add(new PreprocessingProblem("Number too big! Value cannot be bigger than "+maxValue+"!",
                            Problem.Type.ERROR, currentFile, line, args[1]));
                    return false;
                }
            } catch (NumberFormatException e) {
                problems.add(new PreprocessingProblem("Second argument of '"+directiveName+"' is not a valid number!",
                        Problem.Type.ERROR, currentFile, line, args[1]));
                return false;
            }
        }

        if (!result) return false;
        else {
            if (!regexFromSymbol(args[0].toLowerCase(), args[1].toLowerCase(),
                    false, false)) return false;
        }
        if (args.length > 2)
            problems.add(new PreprocessingProblem("Too many arguments for '"+directiveName+"' directive!",
                    Problem.Type.WARNING, currentFile, line,
                    Arrays.toString(Arrays.copyOfRange(args, 2, args.length))));
        return true;

    }

    private boolean regexFromSymbol(final String symbol, final String replacement,
                                    final boolean modifiable, final boolean replacing) {

        Regex regex = new Regex(new StringBuilder("s/\\b").append(symbol).append("\\b/").append(replacement).append("/")

                .append(Regex.CASE_INSENSITIVE_FLAG).append(Regex.WHOLE_LINE_FLAG)
                .append(Regex.DO_NOT_REPLACE_IN_STRING_FLAG)
                .append(modifiable ? Regex.MODIFIABLE_FLAG : Regex.UNMODIFIABLE_FLAG).toString(),

                currentFile, line, problems);
        if (!regex.isValid())
            return false;

        for (String reserved : MC8051Library.RESERVED_NAMES)
            if (symbol.equalsIgnoreCase(reserved)) {
                problems.add(new PreprocessingProblem("Symbol is a reserved name!",
                        Problem.Type.ERROR, currentFile, line, symbol));
                return false;
            }

        if (!replacing) {
            for (Regex r : regexes)
                if (regex.equals(r)) {
                    problems.add(new PreprocessingProblem("Symbol already defined!",
                            Problem.Type.ERROR, currentFile, line, symbol));
                    return false;
                }
        } else {
            Regex[] regexArray = regexes.toArray(new Regex[regexes.size()]);
            for (int i = 0; i < regexArray.length; ++i) {
                Regex r = regexArray[i];
                if (r.equals(regex)) {
                    if (r.isModifiable()) {
                        regexes.set(i, regex);
                        return true;
                    } else {
                        problems.add(new PreprocessingProblem("Symbol already defined!",
                                Problem.Type.ERROR, currentFile, line, symbol));
                        return false;
                    }
                }
            }
        }
        regexes.add(regex);
        return true;

    }

}
