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
 * @author Noxgrim
 */
public class Preprocessor8051 implements Preprocessor {

    private List<Problem> problems;

    private Path currentFile;
    private int line;

    private List<Regex> regexes;
    private int includeDepth;

    private List<String> output;
    private int outputIndex;

    private byte endState;         // 0: Running, 1: End Reached, 2: End Problem created
    private static final byte RUNNING = 0;
    private static final byte END_REACHED = 1;
    private static final byte END_PROBLEM_CREATED = 2;

    private int conditionalDepth;
    private byte conditionalState; // 0: Normal, 1: in If-block, 2: in Else-block

    private final Directive[] directives = {
            new Directive("file", 1, 2, true) {
                @Override
                public boolean perform(String[] args) {
                    final String fileString = args[0];
                    try {
                        Path newPath = Paths.get(fileString);
                        line = 0;


                        if (args.length > 1)
                            directives[1].perform(args[1], new PreprocessingProblem(currentFile, line, ""), problems);

                        currentFile = newPath;
                        return true;
                    } catch (InvalidPathException e) {
                        problems.add(new PreprocessingProblem("Invalid Path!: " + e.getMessage(), Problem.Type.ERROR,
                                currentFile, line, fileString));
                        return false;
                    }
                }
            },

            new Directive("line", true) {
                @Override
                public boolean perform(String[] args) {

                    String number = args[0];
                    try {

                        final boolean relative = number.charAt(0) == '+' || number.charAt(0) == '-';

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

                    } catch (NumberFormatException e) {
                        problems.add(new PreprocessingProblem("Illegal number format!", Problem.Type.ERROR,
                                currentFile, line, number));
                        return false;
                    }
                    return true;
                }
            },

            new Directive("end", 0, 0) {
                @Override
                public boolean perform(String[] args) {
                    if (endState == 0)
                        endState = 1;
                    return true;
                }
            },

            new Directive("include", 1, 1, new String[]{"x\"\"", "x''", "i<>"}, false) {
                @Override
                public boolean perform(String[] args) {
                    final String targetFile = args[0];
                    try {
                        Path target = null;
                        if (targetFile.codePointAt(1) == '<' && targetFile.codePointAt(targetFile.length()-1) == '>') {
                            String[] dirs = Settings.INSTANCE.getProperty(AssemblerSettings.INCLUDE_PATH)
                                    .split("(?<!(?<!\\\\)\\\\);");
                            boolean recursiveSearch = Settings.INSTANCE.getBoolProperty(
                                    AssemblerSettings.INCLUDE_RECURSIVE_SEARCH);
                            for (String dir : dirs) {

                                dir = dir.replaceAll("\\\\\\\\", "\\\\").replaceAll("\\\\;", ";");

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

                        final int maxDepth = Settings.INSTANCE.getIntProperty(
                                AssemblerSettings.INCLUDE_DEPTH, x -> x >= 0);
                        if (maxDepth == 0) {
                            problems.add(new PreprocessingProblem("Including inside includes is deactivated!",
                                    Problem.Type.ERROR, currentFile, line, target.toString()));
                            return false;
                        } else if (maxDepth <= includeDepth) {
                            problems.add(new PreprocessingProblem("Maximal inclusion depth reached!",
                                    Problem.Type.ERROR, currentFile, line, target.toString()));
                            return false;
                        } else
                            ++includeDepth;


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
                            output.add(outputIndex+=fileContent.size()+1, null);
                            output.add(outputIndex+=fileContent.size()+2, "$file \"" + target.toString() + "\" " + line+1);

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

                    return true;
                }
            },

            new Directive("code", 2, 2) {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFFFF);
                }
            },

            new Directive("xdata", 2, 2) {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFFFF);
                }
            },

            new Directive("bit", 2, 2) {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFF);
                }
            },

            new Directive("data", 2, 2) {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFF);
                }
            },

            new Directive("idata", 2, 2) {
                @Override
                public boolean perform(String... args) {
                    // Implemented for compatibility with Asem-51's MCU files
                    // Does not have a particular segment type.
                    return mcuCompatibilityDirective(args, this.getName(), 0xFF);
                }
            },

            new Directive("equ", 2, 2) {
                @Override
                public boolean perform(String... args) {
                    boolean result = true;

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

                    if (!regexFromSymbol(args[0].toLowerCase(), args[1].toLowerCase(), false, true)) return false;
                    return true;
                }
            },

            new Directive("set", 2, 2) {
                @Override
                public boolean perform(String... args) {
                    boolean result = true;

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

                    if (!regexFromSymbol(args[0].toLowerCase(), args[1].toLowerCase(), true, true)) return false;
                    return true;
                }

            },

            new Directive("regex") {
                @Override
                protected boolean perform(String[] args) {

                    Regex r = new Regex(args[0], currentFile, line, problems);

                    if (r.isValid())  {
                        regexes.add(r);
                        return true;
                    } else
                        return false;
                }
            },

            new Directive("db", 1, Integer.MAX_VALUE, new String[]{"i\"\"", "i''"}, true) {
                @Override
                protected boolean perform(String[] args) {

                    StringBuilder replacement = new StringBuilder(output.get(outputIndex).length());
                    replacement.append("$db");

                    for (String arg : args)
                        if (MC8051Library.NUMBER_PATTERN.matcher(arg).matches())
                            try {
                                final int value = Integer.parseInt(arg);
                                if (value > 0xFF)
                                    problems.add(new PreprocessingProblem("Value of number is bigger than a byte!",
                                            Problem.Type.ERROR, currentFile, line, arg));
                                else
                                    replacement.append(" ").append(arg);
                            } catch (NumberFormatException e) {
                                problems.add(new PreprocessingProblem("Illegal number!", Problem.Type.ERROR,
                                        currentFile, line, arg));
                            }
                        else if (arg.charAt(0) == '"' && arg.charAt(arg.length()-1) == '"' ||
                                arg.charAt(0) == '\'' && arg.charAt(arg.length()-1) == '\'') {
                            for (int cp : arg.substring(1, arg.length()-1).codePoints().toArray()) {
                                StringBuilder numbers = new StringBuilder();
                                for (int i = 0; (cp >> i & 0xFF) != 0 && i < Integer.SIZE; i+=8)
                                    numbers.insert(0, cp >> i & 0xFF).insert(0, " ");
                                replacement.append(numbers);
                            }
                        } else
                            problems.add(new PreprocessingProblem("Whether a valid number nor a valid string.",
                                    Problem.Type.ERROR, currentFile, line, arg));
                    output.set(outputIndex, replacement.toString());
                    return replacement.length() > 3; // Always return 'true', errors will be cut out
                                                     // only return 'false' if all arguments were invalid
                }
            },

            new Directive("dw", 1, Integer.MAX_VALUE, new String[]{"i\"\"", "i''"}, false) {
                @Override
                protected boolean perform(String[] args) {
                    StringBuilder replacement = new StringBuilder(output.get(outputIndex).length());
                    replacement.append("$db"); // Also use 'db'

                    for (String arg : args)
                        if (MC8051Library.NUMBER_PATTERN.matcher(arg).matches())
                            try {
                                final int value = Integer.parseInt(arg);
                                if (value > 0xFFFF)
                                    problems.add(new PreprocessingProblem("Value of number is bigger than a byte word (2 bytes)!",
                                            Problem.Type.ERROR, currentFile, line, arg));
                                else
                                    replacement.append(" ").append(value >> 8 & 0xFF).append(" ").append(value & 0xFF);
                            } catch (NumberFormatException e) {
                                problems.add(new PreprocessingProblem("Illegal number!", Problem.Type.ERROR,
                                        currentFile, line, arg));
                            }
                        else if (arg.charAt(0) == '"' && arg.charAt(arg.length()-1) == '"' ||
                                arg.charAt(0) == '\'' && arg.charAt(arg.length()-1) == '\'') {
                            for (int cp : arg.substring(1, arg.length()-1).codePoints().toArray()) {
                                StringBuilder numbers = new StringBuilder();
                                for (int i = 0; (cp >> i & 0xFFFF) != 0 && i < Integer.SIZE; i+=16)
                                    numbers.insert(0, cp >> i & 0xFF).insert(0, " ")
                                            .insert(0, cp >> i + 8 & 0xFF).insert(0, " ");
                                replacement.append(numbers);
                            }
                        } else
                            problems.add(new PreprocessingProblem("Whether a valid number nor a valid string.",
                                    Problem.Type.ERROR, currentFile, line, arg));
                    output.set(outputIndex, replacement.toString());
                    return replacement.length() > 3; // Always return 'true', errors will be cut out
                                                     // only return 'false' if all arguments were invalid
                }
            },

            new Directive("ds", 1, 2, new String[]{"i\"\"", "i''"}, false) {
                @Override
                protected boolean perform(String[] args) {

                    StringBuilder replacement = new StringBuilder();
                    replacement.append("$db");

                    int factor = 1;
                    StringBuilder repeat = new StringBuilder("0"); // Default to 0
                    try {
                        factor = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        problems.add(new PreprocessingProblem("Illegal number format for 'ds' multiplier!",
                                Problem.Type.ERROR, currentFile, line, args[0]));
                        return false;
                    }
                    if (args.length > 1) {
                        if (MC8051Library.NUMBER_PATTERN.matcher(args[1]).matches())
                            try {
                                final int value = Integer.parseInt(args[1]);
                                if (value > 0xFF)
                                    problems.add(new PreprocessingProblem("Value of number is bigger than a byte!",
                                            Problem.Type.ERROR, currentFile, line, args[1]));
                                else {
                                    repeat = new StringBuilder(" ");
                                    repeat.append(value & 0xFF);
                                }
                            } catch (NumberFormatException e) {
                                problems.add(new PreprocessingProblem("Illegal number!", Problem.Type.ERROR,
                                        currentFile, line, args[1]));
                            }
                        else if (args[1].charAt(0) == '"' && args[1].charAt(args[1].length()-1) == '"' ||
                                args[1].charAt(0) == '\'' && args[1].charAt(args[1].length()-1) == '\'') {
                            for (int cp : args[1].substring(1, args[1].length()-1).codePoints().toArray()) {
                                StringBuilder numbers = new StringBuilder();
                                for (int i = 0; (cp >> i & 0xFF) != 0 && i < Integer.SIZE; i+=8)
                                    numbers.insert(0, cp >> i & 0xFF).insert(0, " ");
                                repeat = new StringBuilder(numbers);
                            }
                        } else
                            problems.add(new PreprocessingProblem("Whether a valid number nor a valid string.",
                                    Problem.Type.ERROR, currentFile, line, args[1]));
                    }

                    if (repeat.toString().trim().isEmpty())
                        return false;
                    else
                        for (int i = factor; i > 0; --i)
                            replacement.append(repeat);

                    output.set(outputIndex, replacement.toString());
                    return replacement.length() > 3;
                }
            },

    };

    public Preprocessor8051() {
        problems = new LinkedList<>();
        output = new ArrayList<>(50);
        regexes = new LinkedList<>();
    }

    @Override
    public List<? extends Problem> preprocess(Path file, List<String> output) {
        problems.clear();
        regexes.clear();
        this.output.clear();
        currentFile = null;

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
        endState = RUNNING;
        conditionalState = 0;
        includeDepth = 0;

        String lineString = null, original;
        output.add("$file \"" + currentFile.toString() + "\""); // Add directive for main source file
        // for Tokenizer and Assembler
        for (outputIndex = 1; outputIndex < output.size(); ++outputIndex) {
            this.line++;
            lineString = original = output.get(outputIndex);

            if (lineString == null) {
                if (includeDepth > 0) --includeDepth;
            } else if (endState == RUNNING) {

                for (Regex regex : regexes)
                    lineString = regex.perform(lineString,    // Perform all registered regular expressions
                            currentFile, line, problems);     // on the current line.

                lineString = cutComment(lineString);          // Cut comments

                lineString = convertNumbers(lineString);      // Convert any numbers into the decimal system

                lineString = evaluate(lineString);            // Evaluate all mathematical expressions

                lineString = validateLabels(lineString);      // Test for name duplicates in labels and reserve symbol

                if (MC8051Library.DIRECTIVE_PATTERN.matcher(lineString).matches())
                    lineString = handleDirective(lineString); // Line is a directive: handle it
                else
                    lineString = lowerCase(lineString);       // Only convert lines to lowercase if they
                                                              // are not a directive because fallthrough
                                                              // directives may be case sensitive.

                output.add(lineString);
            } else if (!lineString.split(";", 2)[0].trim().isEmpty() && endState == END_REACHED) {
                // If the line contains more than just comments or white space
                // and no Problem has been created yet
                MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(currentFile, this.line, lineString),
                        AssemblerSettings.END_CODE_AFTER, "No code allowed after use of 'end' directive!",
                        "All code after an 'end' directive will be ignored.", problems);
                endState = END_PROBLEM_CREATED;
            }

        }

        if (endState == RUNNING)
            MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(currentFile, this.line, lineString),
                    AssemblerSettings.END_MISSING, "'end' directive not found!", "Missing 'end' directive!",
                    problems);


        output.addAll(this.output);
        this.output.clear();

        return problems;
    }

    private String handleDirective(final String line) {
        Matcher m = MC8051Library.DIRECTIVE_PATTERN.matcher(line);
        if (m.matches()) {
            String name = m.group(1).toLowerCase();
            for (Directive d : directives)
                if (d.getName().equals(name)) {
                    boolean result = d.perform(m.group(2),
                            new PreprocessingProblem(currentFile, this.line, line), problems);
                    if (result && d.isFallthrough())
                        return output.get(outputIndex); // Return the line of the directive in the output
                                                        // if the directive modified its own line.
                    else
                        return "";                      // else clear line.
                }
            problems.add(new PreprocessingProblem("Unknown directive!", Problem.Type.ERROR, currentFile, this.line, name));
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


    private String validateLabels(String lineString) {
        final Matcher l = MC8051Library.LABEL_PATTERN.matcher(lineString);
        StringBuilder result = new StringBuilder(lineString);

        if (l.find()) {
            final String name = l.group(1);

            Regex regex = new Regex(new StringBuilder("/(?<!^)(\\s*)\\b").append(name).append("\\b(!:)/")

                    .append(Regex.CASE_INSENSITIVE_FLAG).append(Regex.UNMODIFIABLE_FLAG).toString(),

                    currentFile, line, problems);

            for (String reserved : MC8051Library.RESERVED_NAMES)
                if (name.equalsIgnoreCase(reserved)) {
                    problems.add(new PreprocessingProblem("Label name is a reserved name!",
                            Problem.Type.ERROR, currentFile, line, name));

                    return  result.delete(0, l.end()).toString();
                }


            for (Regex r : regexes)
                if (regex.equals(r)) {
                    problems.add(new PreprocessingProblem("Symbol already defined!",
                            Problem.Type.ERROR, currentFile, line, name));

                    return result.delete(0, l.end()).toString();
                }

            regexes.add(regex);

        }

        return lineString;

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


    private String evaluate(final String line) {
        // Find possible mathematical expressions
        // TODO: Only evaluate outside of Strings
        int parenthesisCount = 0;
        StringBuilder sb = new StringBuilder(line);
        StringBuilder result = new StringBuilder(line.length());
        StringBuilder expression = new StringBuilder();
        for (int cp : sb.codePoints().toArray()) {
            if (parenthesisCount == 0 && cp != '(')
                result.appendCodePoint(cp);
            else if (cp == '(') {
                ++parenthesisCount;
                if (parenthesisCount > 0)
                    expression.appendCodePoint(cp);
            } else if (parenthesisCount > 0 && cp == ')') {
                if (0 == --parenthesisCount) {
                    try {

                        result.append(SimpleMath.evaluate(expression.toString()));

                    } catch (NumberFormatException e) {
                        problems.add(new PreprocessingProblem("Number format invalid: " + e.getMessage(),
                                Problem.Type.ERROR, currentFile, this.line, expression.toString()));
                        result.append("0");
                    } catch (IllegalArgumentException e) {
                        problems.add(new PreprocessingProblem("Mathematical expression invalid: " + e.getMessage(),
                                Problem.Type.ERROR, currentFile, this.line, expression.toString()));
                        result.append("0");
                    } catch (RuntimeException e) {
                        problems.add(new PreprocessingProblem(e.toString(),
                                Problem.Type.ERROR, currentFile, this.line, expression.toString()));
                        result.append("0");
                    }
                    expression.setLength(0);
                } else
                    expression.appendCodePoint(cp);
            }
        }

        return result.toString();
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

        // TODO: Only covert outside of Strings

        Matcher n = MC8051Library.NUMBER_PATTERN.matcher(source);

        // Variation of 'replaceAll()' in Matcher
        boolean found;
        if (found = n.find()) {
            do {
                final String number = getNumber(n.group());
                n.appendReplacement(sb, getNumber(number == null ? "0" : number));
            } while (found = n.find());
            return n.appendTail(sb).toString();
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
            if (!regexFromSymbol(args[0].toLowerCase(), args[1].toLowerCase(), false, false)) return false;
        }

        return true;

    }

    private boolean regexFromSymbol(final String symbol, final String replacement,
                                    final boolean modifiable, final boolean replacing) {

        Regex regex = new Regex(new StringBuilder("s/(?<!^)(\\s*)\\b").append(symbol).append("\\b(!:)/").append("$1")
                .append(replacement).append("/")

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
