package assembler.arc8051;

import assembler.Preprocessor;
import assembler.util.AssemblerSettings;
import assembler.util.Regex;
import assembler.util.assembling.Directive;
import assembler.util.assembling.Mnemonic;
import assembler.util.problems.ExceptionProblem;
import assembler.util.problems.PreprocessingProblem;
import assembler.util.problems.Problem;
import misc.Logger;
import misc.Settings;
import simplemath.SimpleMath;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Preprocesses input for assembly language written for the
 * 8051 family.
 *
 * @author Noxgrim
 */
public class Preprocessor8051 implements Preprocessor {

    private List<Problem<?>> problems;
    private Path directory;

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

    private boolean conditionInIf; // Push this on the stack
    private byte conditionState;

    private static byte COND_WILL_BE_ACTIVE = 0;
    private static byte COND_IS_ACTIVE = 1;
    private static byte COND_WAS_ACTIVE = 2;

    private static final String[] CONDITION_RESISTANT = {"if", "elif", "else", "endif", "file", "line"};

    private Stack<Boolean> conditionStack;

    private final Directive[] directives = {
            new Directive("file", 1, 3, true) {
                @Override
                public boolean perform(String[] args) {
                    final String fileString = args.length == 1 ? args[0] : args[1];

                    boolean conditionAffected = true;

                    if (args.length > 1) {
                        if (args[0].equals("-a") || args[0].equalsIgnoreCase("--always"))
                            conditionAffected = false;
                        else
                            problems.add(new PreprocessingProblem("Unknown option for 'file' directive.",
                                    Problem.Type.ERROR, currentFile, line, args[0]));
                    }

                    if (!conditionStack.isEmpty() && conditionState != COND_IS_ACTIVE && conditionAffected)
                        return true;

                    try {
                        Path newPath = Paths.get(fileString);
                        line = 0;

                        if (args.length >= 3) {
                            output.set(outputIndex, "$file \"" + args[1] + "\" " + args[2]); // Remove option for
                                                                                                // Tokenizer
                            directives[1].perform(args[2], new PreprocessingProblem(currentFile, line, ""),
                                    problems);
                        } else if (args.length == 2)
                            if (!args[0].startsWith("-"))
                                directives[1].perform(args[1], new PreprocessingProblem(currentFile, line, ""),
                                        problems);
                            else
                                output.set(outputIndex, "$file \"" + args[1] + "\"");

                        currentFile = newPath;
                        return true;
                    } catch (InvalidPathException e) {
                        problems.add(new PreprocessingProblem("Invalid Path!: " + e.getMessage(), Problem.Type.ERROR,
                                currentFile, line, fileString));
                        return false;
                    }
                }
            },

            new Directive("line", 1, 2, true) {
                @Override
                public boolean perform(String[] args) {

                    String number = args.length == 1 ? args[0] : args[1];

                    boolean conditionAffected = true;

                    if (args.length == 2) {
                        if (args[0].equals("-a") || args[0].equalsIgnoreCase("--always"))
                            conditionAffected = false;
                        else
                            problems.add(new PreprocessingProblem("Unknown option for 'line' directive.",
                                    Problem.Type.ERROR, currentFile, line, args[0]));
                        output.set(outputIndex, "$line " + args[1]);
                    }

                    if (!conditionStack.isEmpty() && conditionState != COND_IS_ACTIVE && conditionAffected)
                        return true;

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
                    endState = END_REACHED;
                    return true;
                }
            },

            new Directive("include", 1, 2, new String[]{"x\"\"", "x''", "i<>"}, false) {
                @Override
                public boolean perform(String[] args) {
                    String targetFile = args.length == 1 ? args[0] : args[1];
                    boolean internal = false;
                    if (args.length == 2) {
                        if (args[0].equals("-i") || args[0].equalsIgnoreCase("--internal"))
                            internal = true;
                        else
                            problems.add(new PreprocessingProblem("Unknown option for 'include' directive.",
                                    Problem.Type.ERROR, currentFile, line, args[0]));
                    }
                    FileSystem zipFS = null; // File system here to be able to close it everywhere
                    try {
                        Path target = null;
                        if (targetFile.charAt(0) == '<' && targetFile.charAt(targetFile.length()-1) == '>') {
                            targetFile = targetFile.substring(1, targetFile.length()-1);
                            String[] dirs = Settings.INSTANCE.getProperty(AssemblerSettings.INCLUDE_PATH)
                                    .split("(?<!(?<!\\\\)\\\\);");
                            if (!internal)
                                for (String dir : dirs) {

                                    dir = dir.replaceAll("\\\\\\\\", "\\\\").replaceAll("\\\\;", ";");

                                    if (!Files.exists(Paths.get(dir))) {
                                        final String message = "Include path does not exist!", finalDir = dir;
                                        if (!problems.stream().anyMatch(p -> p.getMessage().equals(message) &&
                                                p.getCause().equals(finalDir)))
                                            problems.add(new PreprocessingProblem(message,
                                                    Problem.Type.ERROR, currentFile, -1, dir));
                                        continue;
                                    }

                                    target = Paths.get(dir, targetFile);
                                    if (!Files.exists(target))
                                        target = null;
                                    else
                                        break;
                                }
                            // Try to include from Jar
                            if (target == null) {
                                try {
                                    // Zip-FileSystem handler:
                                    // http://stackoverflow.com/questions/25032716/
                                    // getting-filesystemnotfoundexception-from-zipfilesystemprovider-when-creating-a-p
                                    URL url = Preprocessor.class.getResource("include/"+targetFile);
                                    if (url == null) {
                                        problems.add(new PreprocessingProblem("File '" + targetFile + "' could not be" +
                                                " included! It " + (!internal? "exits neither in the path nor" :
                                                "does not exist") + " in the Jar file.",
                                                Problem.Type.ERROR, currentFile, line, targetFile));
                                        return false;
                                    }

                                    URI uri = url.toURI();
                                    Map<String, String> env = new HashMap<>(1);
                                    env.put("create", "true");
                                    try {

                                        zipFS = FileSystems.newFileSystem(uri, env);

                                        target = Paths.get(uri);

                                    } catch (IOException ioe) {
                                        if (zipFS != null) // Close FS
                                            zipFS.close();
                                        Logger.logThrowable(ioe, Preprocessor.class, Logger.LogLevel.DEBUG);
                                        problems.add(new PreprocessingProblem("Could not include from Jar: " +
                                                ioe.getMessage(), Problem.Type.ERROR, currentFile, line, targetFile));
                                        return false;
                                    } catch (IllegalArgumentException e) {
                                        // Try standard file System (Program is probably not in a Jar.)
                                        if (zipFS != null) // Close FS
                                            zipFS.close();
                                        target = Paths.get(Preprocessor.class.getResource("include/"+targetFile).toURI());
                                    }
                                } catch (NullPointerException | URISyntaxException e) {
                                    Logger.logThrowable(e, Preprocessor.class, Logger.LogLevel.DEBUG);
                                    problems.add(new PreprocessingProblem("Could not include from Jar: " + e.getMessage(),
                                            Problem.Type.ERROR, currentFile, line, targetFile));
                                    if (zipFS != null) // Close FS
                                        zipFS.close();
                                    return false;
                                }
                            }

                            if (target == null || !Files.exists(target)) {
                                problems.add(new PreprocessingProblem("File to include does not exist!",
                                        Problem.Type.ERROR, currentFile, line, targetFile));
                                return false;
                            }

                        } else {
                            if (internal)
                                problems.add(new PreprocessingProblem("The 'internal' option can only be used with " +
                                        "path includes.", Problem.Type.ERROR, currentFile, line, args[0]));

                            boolean recursiveSearch = Settings.INSTANCE.getBoolProperty(
                                    AssemblerSettings.INCLUDE_RECURSIVE_SEARCH);
                            if (recursiveSearch) {
                                target = findFile(directory, targetFile);

                                if (target == null) {
                                    problems.add(new PreprocessingProblem("File to include not found in directory!",
                                            Problem.Type.ERROR, currentFile, line, targetFile));
                                    return false;
                                }
                            }
                            else
                                target = Paths.get(directory.toString(), targetFile);
                            if (!Files.exists(target)) {
                                problems.add(new PreprocessingProblem("File to include does not exist!",
                                        Problem.Type.ERROR, currentFile, line, targetFile));
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

                            output.add(outputIndex + 1, "$file \"" + target.toString() + "\"");
                            output.addAll(outputIndex+2, fileContent);
                            output.add(outputIndex+fileContent.size()+2, null);
                            output.add(outputIndex+fileContent.size()+3, "$file --always \"" + currentFile.toString() +
                                    "\" " + (line+1)); // --always: force switching files to keep current file
                                                       //           correct even if file contains an unclosed 'if'

                            if (zipFS != null) // Close FS
                                zipFS.close();
                            return true;
                        }
                    } catch (InvalidPathException e) {
                        problems.add(new PreprocessingProblem("Invalid Path!: " + e.getMessage(), Problem.Type.ERROR,
                                currentFile, line, targetFile));
                        if (zipFS != null) // Close FS
                            try {
                                zipFS.close();
                            } catch (IOException e1) {
                                Logger.logThrowable(e1, Preprocessor.class, Logger.LogLevel.ERROR);
                            }
                        return false;
                    } catch (IOException e) {
                        Logger.logThrowable(e, Preprocessor.class, Logger.LogLevel.ERROR);
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

            new Directive("regex", 1, 2) {
                @Override
                protected boolean perform(String[] args) {
                    boolean force = false;
                    Regex regex ;
                    if (args.length > 1) {
                        if (args[0].equals("-f") || args[0].equalsIgnoreCase("--force"))
                            force = true;
                        else
                            problems.add(new PreprocessingProblem("Unknown option for 'regex' directive.",
                                    Problem.Type.ERROR, currentFile, line, args[0]));
                        regex = new Regex(args[1], currentFile, line, problems);
                    } else
                        regex = new Regex(args[0], currentFile, line, problems);

                    if (regex.isValid())  {
                        for (int i = 0; i < regexes.size(); ++i) {
                            Regex r = regexes.get(i);
                            if (r.equals(regex)) {
                                if (r.isModifiable() || force) {
                                    regexes.set(i, regex);
                                    return true;
                                } else {
                                    problems.add(new PreprocessingProblem(
                                            "A similar regex that isn't modifiable already defined!",
                                            Problem.Type.ERROR, currentFile, line, args[0]));
                                    return false;
                                }
                            }
                        }
                        regexes.add(regex);
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

            new Directive("dw", 1, Integer.MAX_VALUE, new String[]{"i\"\"", "i''"}, true) {
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

            new Directive("ds", 1, 2, new String[]{"i\"\"", "i''"}, true) {
                @Override
                protected boolean perform(String[] args) {

                    StringBuilder replacement = new StringBuilder();
                    replacement.append("$db");

                    int factor = 1;
                    StringBuilder repeat = new StringBuilder(" 0"); // Default to 0
                    try {
                        factor = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        problems.add(new PreprocessingProblem("Illegal number format for 'ds' multiplier!",
                                Problem.Type.ERROR, currentFile, line, args[0]));
                        return false;
                    }
                    if (args.length > 1) {
                        final String toRepeat = args[1];
                        if (MC8051Library.NUMBER_PATTERN.matcher(toRepeat).matches())
                            try {
                                final int value = Integer.parseInt(toRepeat);
                                if (value > 0xFF)
                                    problems.add(new PreprocessingProblem("Value of number is bigger than a byte!",
                                            Problem.Type.ERROR, currentFile, line, toRepeat));
                                else {
                                    repeat = new StringBuilder(" ");
                                    repeat.append(value & 0xFF);
                                }
                            } catch (NumberFormatException e) {
                                problems.add(new PreprocessingProblem("Illegal number!", Problem.Type.ERROR,
                                        currentFile, line, toRepeat));
                            }
                        else if (toRepeat.charAt(0) == '"' && toRepeat.charAt(toRepeat.length()-1) == '"' ||
                                toRepeat.charAt(0) == '\'' && toRepeat.charAt(toRepeat.length()-1) == '\'') {
                            repeat = new StringBuilder((toRepeat.length()-2)*2);
                            for (int cp : toRepeat.substring(1, toRepeat.length() - 1).codePoints().toArray()) {
                                StringBuilder numbers = new StringBuilder();
                                for (int i = 0; (cp >> i & 0xFF) != 0 && i < Integer.SIZE; i+=8)
                                    numbers.insert(0, cp >> i & 0xFF).insert(0, " ");
                                repeat.append(numbers);
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

            new Directive("if", 1) {
                @Override
                protected boolean perform(String[] args) {

                    Boolean result = evaluateCondition(args);
                    if (result == null)
                        return false;

                    if (conditionStack.isEmpty())
                        conditionStack.push(null);
                    else
                        conditionStack.push(conditionInIf);

                    conditionInIf = true;

                    if (result)
                        conditionState = COND_IS_ACTIVE;
                    else
                        conditionState = COND_WILL_BE_ACTIVE;

                    return false;
                }
            },

            new Directive("elif", 1) {
                @Override
                protected boolean perform(String[] args) {
                    if (conditionStack.isEmpty())
                        problems.add(new PreprocessingProblem("'elif' without leading 'if' directive!",
                                Problem.Type.ERROR, currentFile, line, "elif"));
                    else if (conditionInIf) {
                        if (conditionState == COND_WILL_BE_ACTIVE) {

                            Boolean result = evaluateCondition(args);

                            if (result == null)
                                return false;
                            else if (result)
                                conditionState = COND_IS_ACTIVE;

                        } else if (conditionState == COND_IS_ACTIVE)
                            conditionState = COND_WAS_ACTIVE;
                        return true;
                    } else
                        problems.add(new PreprocessingProblem("'elif' directive cannot be used after a 'then' " +
                                "directive!", Problem.Type.ERROR, currentFile, line, "elif"));
                    return false;
                }
            },

            new Directive("else", 0, 0) {
                @Override
                protected boolean perform(String[] args) {
                    if (conditionStack.isEmpty())
                        problems.add(new PreprocessingProblem("'else' without leading 'if' directive!",
                                Problem.Type.ERROR, currentFile, line, "else"));
                    else if (conditionInIf) {
                        conditionInIf = false;

                        if (conditionState == COND_WILL_BE_ACTIVE)
                            conditionState = COND_IS_ACTIVE;
                        else if (conditionState == COND_IS_ACTIVE)
                            conditionState = COND_WAS_ACTIVE;

                        return  true;
                    } else
                        problems.add(new PreprocessingProblem("'else' directive cannot be after another 'then' " +
                                "directive!", Problem.Type.ERROR, currentFile, line, "else"));
                    return false;
                }
            },

            new Directive("endif", 0, 0) {
                @Override
                protected boolean perform(String[] args) {
                    if (conditionStack.isEmpty())
                        problems.add(new PreprocessingProblem("No 'if' to be closed!",
                                Problem.Type.ERROR, currentFile, line, "endif"));
                    else {
                        Boolean old = conditionStack.pop();
                        if (conditionStack.isEmpty()) {
                            conditionInIf = false;
                        } else {
                            conditionInIf = old;
                            conditionState = COND_IS_ACTIVE;
                        }
                    }

                    return false;
                }
            },
    };

    public Preprocessor8051() {
        problems = new LinkedList<>();
        output = new ArrayList<>(50);
        regexes = new ArrayList<>(50);
        conditionStack = new Stack<>();
    }

    @Override
    public List<Problem<?>> preprocess(Path workingDirectory, Path file, List<String> output) {
        Logger.log("Start preprocessing…", Preprocessor.class, Logger.LogLevel.INFO);
        problems.clear();
        regexes.clear();
        this.output.clear();
        directory = workingDirectory;
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

        if (Settings.INSTANCE.getBoolProperty(AssemblerSettings.SKIP_PREPROCESSING)) {

            output.addAll(this.output);
            this.output.clear();
            problems.add(new PreprocessingProblem("Skipped preprocessing.", Problem.Type.INFORMATION, currentFile,
                    line, AssemblerSettings.SKIP_PREPROCESSING + "=true"));
            return problems;
        }

        line = -1;
        endState = RUNNING;
        conditionState = COND_WILL_BE_ACTIVE;
        conditionStack.clear();
        includeDepth = 0;

        String lineString = null;
        output.add(0, "$file \"" + currentFile.toString() + "\""); // Add directive for main source file
                                                                   // for Tokenizer and Assembler
        this.output.add(includeDefaults(), "$line 1");

        for (outputIndex = 0; outputIndex < this.output.size(); ++outputIndex) {
            lineString = this.output.get(outputIndex);

            if (lineString == null) {
                if (includeDepth > 0) --includeDepth;
            } else if (endState == RUNNING) {
                this.line++;

                for (Regex regex : regexes)
                    lineString = regex.perform(lineString,    // Perform all registered regular expressions
                            currentFile, line, problems);     // on the current line.

                lineString = cutComment(lineString);          // Cut comments

                if (!MC8051Library.DIRECTIVE_PATTERN.matcher(lineString).matches())
                    lineString = resolveStrings(lineString);  // Convert any String into numbers.

                lineString = convertNumbers(lineString);      // Convert any numbers into the decimal system

                lineString = evaluate(lineString);            // Evaluate all mathematical expressions

                lineString = validateLabels(lineString);      // Test for name duplicates in labels and reserve symbol

                if (MC8051Library.DIRECTIVE_PATTERN.matcher(lineString).matches())
                    lineString = handleDirective(lineString); // Line is a directive: handle it
                else if (conditionStack.isEmpty() || conditionState == COND_IS_ACTIVE) // Only output if no condition
                                                                                       // active or in positive
                                                                                       // condition
                    lineString = lineString.toLowerCase();    // Only convert lines to lowercase if they
                                                              // are not a directive because fallthrough
                                                              // directives may be case sensitive.
                else
                    lineString = "";
                output.add(lineString);
            } else if (!lineString.split(";", 2)[0].trim().isEmpty() && endState == END_REACHED) {
                this.line++;
                // If the line contains more than just comments or white space
                // and no Problem has been created yet
                MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(currentFile, this.line, lineString),
                        AssemblerSettings.END_CODE_AFTER, "No code allowed after use of 'end' directive!",
                        "All code after an 'end' directive will be ignored.", "Found code after 'end' directive.",
                        problems);
                endState = END_PROBLEM_CREATED;
            }

        }
        if (!conditionStack.isEmpty())
            MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(currentFile, this.line, lineString),
                    AssemblerSettings.UNCLOSED_IF,
                    conditionStack.size() + " unclosed 'if' block" + (conditionStack.size() == 1 ? " " : "s ") +
                            "must be closed!",
                    conditionStack.size() + " unclosed 'if' block" + (conditionStack.size() == 1 ? " " : "s ") +
                            "should be closed.",
                    "Found " +conditionStack.size() + " unclosed 'if' block" + (conditionStack.size() == 1 ? "." : "s."),
                    problems);
        if (endState == RUNNING)
            MC8051Library.getGeneralErrorSetting(new PreprocessingProblem(currentFile, this.line, lineString),
                    AssemblerSettings.END_MISSING, "'end' directive not found!", "Missing 'end' directive!",
                    "Found no 'end' directive.",
                    problems);

        this.output.clear();

        Logger.log("Preprocessing finished.", Preprocessor.class, Logger.LogLevel.INFO);
        if (Logger.getLevel() == Logger.LogLevel.DEBUG) {
            Logger.log("Defined Regexes:", Preprocessor.class, Logger.LogLevel.DEBUG);
            for (Regex regex : regexes)
                Logger.log(regex.toString(), Preprocessor.class, Logger.LogLevel.DEBUG);
        }
        return problems;
    }

    /**
     * Tries to handle a directive on a line, by searching directive
     * by searching for a Directive with a similar name and performing it.
     *
     * @param line
     *      the String to be used.
     *
     * @return
     *      the (maybe modified) line if the matching Directive was a 'fallthrough' Directive,
     *      else an empty String.
     */
    private String handleDirective(final String line) {
        Matcher m = MC8051Library.DIRECTIVE_PATTERN.matcher(line);
        if (m.matches()) {
            String name = m.group(1).toLowerCase();
            if (conditionStack.isEmpty() || conditionState == COND_IS_ACTIVE) {
                for (Directive d : directives)
                    if (d.getName().equals(name)) {
                        boolean result = d.perform(m.group(2) == null ? "" : m.group(2),
                                new PreprocessingProblem(currentFile, this.line, line), problems);
                        if (result && d.isFallthrough())
                            return output.get(outputIndex); // Return the line of the directive in the output
                                                            // if the directive modified its own line.
                        else
                            return "";                      // else clear line.
                    }
                problems.add(new PreprocessingProblem("Unknown directive!", Problem.Type.ERROR,
                        currentFile, this.line, name));
            } else {
                    for (Directive d : directives)
                        if (d.getName().equals(name)) {
                            for (String posName : CONDITION_RESISTANT) {
                                if (!posName.equals(name))
                                    continue;
                                boolean result = d.perform(m.group(2) == null ? "" : m.group(2),
                                        new PreprocessingProblem(currentFile, this.line, line), problems);
                                if (result && d.isFallthrough())
                                    return output.get(outputIndex); // Return the line of the directive in the output
                                                                    // if the directive modified its own line.
                                else
                                    return "";                      // else clear line.
                            }
                            return "";      // Directive is not condition resistive so it will be cleared
                        }
                    problems.add(new PreprocessingProblem("Unknown directive!", Problem.Type.ERROR,
                            currentFile, this.line, name));
            }
            return "";
        } else
            return line;
    }

    /**
     * Reads the whole content of a file and returns it as a List by making use of the
     * {@link Files#lines(Path)}.
     *
     * @param file
     *      the file that should be read.
     *
     * @return
     *      the content of the file as a List, <code>null</code> if the file could not be read.
     */
    private List<String> readFile(Path file) {
        Objects.requireNonNull(file, "'file' cannot be 'null'!");

        if (!Files.exists(file)) {
            problems.add(new PreprocessingProblem("Given Path does not exist!", Problem.Type.ERROR, currentFile,
                    currentFile == null ? -1 : line, file.toString()));
            return null;
        }
        if (!Files.isRegularFile(file)) {
           problems.add(new PreprocessingProblem("Given Path is not a regular file.", Problem.Type.ERROR, currentFile,
                   currentFile == null ? -1 : line, file.toString()));
            return null;
        }
        if (!Files.isReadable(file)) {
            problems.add(new PreprocessingProblem("File is not readable.", Problem.Type.ERROR, currentFile,
                    currentFile == null ? -1 : line, file.toString()));
            return null;
        }

        try (Stream<String> stream = Files.lines(file)){
            List<String> result = new ArrayList<>(50);
            stream.forEach(result::add);
            return result;
        } catch (IOException e) {
            problems.add(new ExceptionProblem("Unable to read file: \"" + file + "\"", Problem.Type.ERROR, currentFile,
                    currentFile == null ? -1 : line, e));
            Logger.log("Unable to read file: \"" + file + "\"", Preprocessor.class, Logger.LogLevel.DEBUG);
            Logger.logThrowable(e, Preprocessor.class, Logger.LogLevel.DEBUG);
            return null;
        }
    }

    /**
     * Searches for labels and looks for already defined (resulting in a Problem)
     * and makes sure that the name is defined from now one.
     *
     * @param source
     *      the String to be used.
     *
     * @return
     *      the modified source String.
     */
    private String validateLabels(String source) {
        final Matcher l = MC8051Library.LABEL_PATTERN.matcher(source);
        StringBuilder result = new StringBuilder(source);

        int end = 0;
        outer:
        while (l.find(end)) {
            if (!source.substring(end, l.start()).trim().isEmpty())
                break;
            end = l.end();
            final String name = l.group(1);

            Regex regex = new Regex("c/(?<=[\\w,\\(])(\\s*)\\b"+name+"\\b/^(?!\\T{directive}).*?$/"+
                    Regex.CASE_INSENSITIVE_FLAG+Regex.UNMODIFIABLE_FLAG,

                    currentFile, line, problems);

            for (String reserved : MC8051Library.RESERVED_NAMES)
                if (name.equalsIgnoreCase(reserved)) {
                    problems.add(new PreprocessingProblem("Label name is a reserved name!",
                            Problem.Type.ERROR, currentFile, line, name));

                    result.delete(0, l.end());
                    continue outer;
                }


            for (Regex r : regexes)
                if (regex.equals(r)) {
                    problems.add(new PreprocessingProblem("Symbol already defined!",
                            Problem.Type.ERROR, currentFile, line, name));

                    result.delete(0, l.end());
                    continue outer;
                }

            regexes.add(regex);

        }

        return result.toString();

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


    /**
     * Tries to evaluate every mathematical expression in the
     * String and replaces it with the (with {@link SimpleMath})
     * evaluated result.<br>
     * A expression must be surrounded by parentheses in order to
     * be detected by this algorithm and cannot consist of unresolved
     * symbols (like labels).
     *
     * @param source
     *      the String to be used.
     *
     * @return
     *      the modified source String.
     *
     * @see SimpleMath#evaluate(String)
     */
    private String evaluate(final String source) {
        // Find possible mathematical expressions
        final Pattern p = MC8051Library.STRING_PATTERN;
        final Matcher m = p.matcher(source);
        String[] outside = p.split(source, -1);
        StringBuilder result = new StringBuilder(source.length());

        for (final String os : outside) {
            int parenthesisCount = 0;
            StringBuilder expression = new StringBuilder();
            for (int cp : os.codePoints().toArray()) {
                if (parenthesisCount == 0 && cp != '(')
                    result.appendCodePoint(cp);
                else if (cp == '(') {
                    if (parenthesisCount > 0)
                        expression.appendCodePoint(cp);
                    ++parenthesisCount;
                } else if (parenthesisCount > 0 && cp == ')') {
                    if (0 == --parenthesisCount) {
                        try {

                            final double number = SimpleMath.evaluate(expression.toString());

                            if (number < 0) {
                                problems.add(new PreprocessingProblem("Evaluated expression cannot be negative!",
                                        Problem.Type.ERROR, currentFile, this.line,
                                        expression.toString() + " = " + number));
                                result.append("0");
                            } else if (number != number) {
                                problems.add(new PreprocessingProblem("Evaluated expression is NaN!",
                                        Problem.Type.ERROR, currentFile, line, expression.toString() + " = NaN"));
                                result.append("0");
                            } else {
                                if (number == Double.POSITIVE_INFINITY)
                                    problems.add(new PreprocessingProblem("Evaluated expression is INFINITY!",
                                            Problem.Type.WARNING, currentFile, line,
                                            expression.toString() + " = INFINITY"));
                                    else if ((int) number != number)
                                        problems.add(new PreprocessingProblem("Any decimal places will be cut off!",
                                                Problem.Type.INFORMATION, currentFile, this.line,
                                                expression.toString() + " = " + number));

                                    result.append((int) number);
                            }

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
                } else if (parenthesisCount > 0)
                    expression.appendCodePoint(cp);
            }

            if (parenthesisCount > 0) {
                problems.add(new PreprocessingProblem("Unbalanced brackets!", Problem.Type.ERROR, currentFile, this.line,
                        expression.toString()));
                result.append("0");
            }

            if (m.find())
                result.append(m.group());
        }

        return result.toString();
    }

    /**
     * Coverts any String in a given source String into numbers representing
     * the numerical value of the String<br>
     * Only values up to 2 bytes (<code>0xFFFF</code>) are supported. Any
     * additional bytes will be ignored and a matching Problem will be created.
     *
     * @param source
     *      the String to be used.
     *
     * @return
     *      the modified source String.
     */
    private String resolveStrings(final String source) {
        final Pattern p = MC8051Library.STRING_PATTERN;
        final Matcher m = p.matcher(source);
        String[] outside = p.split(source, -1);

        StringBuilder result = new StringBuilder(source.length());

        for (final String os : outside) {
            result.append(os);
            if (m.find()) {
                String str = m.group(1) == null ? m.group(2) : m.group(1);

                List<Byte> bytes = new ArrayList<>(2);
                int lastChar = '\0';
                for (int cp : str.codePoints().toArray()) {
                    if (lastChar != '\\' && cp == '\\') {
                        lastChar = cp;
                        continue;
                    }
                    for (int i = 0; (cp >> i & 0xFF) != 0 && i < Integer.SIZE; i+=8)
                        bytes.add((byte)(cp >> i & 0xFF));
                    if (bytes.size() > 1)
                        break;
                    lastChar = cp;
                }

                if (bytes.size() == 0) {
                    problems.add(new PreprocessingProblem("A empty String does not contain any data to process!",
                            Problem.Type.ERROR, currentFile, line, m.group()));
                    result.append("0");
                } else {
                    for (int i = 0; i < 2 && i < bytes.size(); ++i)
                        result.append(String.format("%02x", (byte) bytes.get(i)));
                    result.append("h");

                    if (bytes.size() > 2)
                        problems.add(new PreprocessingProblem("Resolved value of the String is too big!",
                                Problem.Type.ERROR, currentFile, line, m.group()));
                }
            }
        }

        if (m.find()) {
            String str = m.group(1) == null ? m.group(2) : m.group(1);

            List<Byte> bytes = new ArrayList<>(2);
            int lastChar = '\0';
            for (int cp : str.codePoints().toArray()) {
                if (lastChar != '\\' && cp == '\\') {
                    lastChar = cp;
                    continue;
                }
                for (int i = 0; (cp >> i & 0xFF) != 0 && i < Integer.SIZE; i+=8)
                    bytes.add((byte)(cp >> i & 0xFF));
                if (bytes.size() > 1)
                    break;
                lastChar = cp;
            }

            if (bytes.size() == 0) {
                problems.add(new PreprocessingProblem("A empty String does not contain any data to process!",
                        Problem.Type.ERROR, currentFile, line, m.group()));
                result.append("0");
            } else {
                for (int i = 0; i < 2 && i < bytes.size(); ++i)
                    result.append(String.format("%02x", (byte) bytes.get(i)));
                result.append("h");

                if (bytes.size() > 2)
                    problems.add(new PreprocessingProblem("Resolved value of the String is too big!",
                            Problem.Type.ERROR, currentFile, line, m.group()));
            }
        }

        return result.toString();
    }

    /**
     * Coverts every occurrence of a valid number in a String to
     * the decimal system.
     *
     *
     * @see #getNumber(String)
     */
    private String convertNumbers(final String source) {
        final Pattern p = MC8051Library.STRING_PATTERN;
        final Matcher m = p.matcher(source);
        String[] outside = p.split(source, -1);

        StringBuilder result = new StringBuilder(source.length());

        for (final String os : outside) {
            Matcher n = MC8051Library.NUMBER_PATTERN.matcher(os);
            StringBuffer temp = new StringBuffer(os.length());

            // Variation of 'replaceAll()' in Matcher
            boolean found;
            if (found = n.find()) {
                do {
                    final String number = getNumber(n.group());
                    n.appendReplacement(temp, number == null ? "0" : number);
                } while (found = n.find());
                n.appendTail(temp);
            } else
                temp.append(os);

            result.append(temp);

            if (m.find())
                result.append(m.group());
        }

        return result.toString();
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

    /**
     * Removes everything after a ';', except if it is inside a String
     * or escaped with a '\\' (always possible, the backslash will be
     * removed.
     *
     * @param source
     *      the String to be used.
     *
     * @return
     *      the modified source String.
     */
    private String cutComment(String source) {

        StringBuilder result = new StringBuilder(source.length());
        boolean simpQuoted = false, doubQuoted = false;

        int last = 0;
        for (int cp : source.codePoints().toArray()) {
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
     * Adds "include" directives for all of the following default setting-sensitive
     * files to the output:<br>
     * <ul>
     *     <li>default.asm</li>
     *     <li>util.obvious-operands.*.asm</li>
     * </ul>
     *
     * @return
     *      the number of files included.
     */
    private int includeDefaults() {
        int included = 0;
        final Settings settings = Settings.INSTANCE;

        if (settings.getBoolProperty(AssemblerSettings.INCLUDE_DEFAULT_FILE))
            this.output.add(included++, "$include <default.asm>");

        this.output.add(included++, "$include <util.obvious-operands.asm>");
        {
            final String file = settings.getProperty(AssemblerSettings.MCU_FILE);
            if (!file.isEmpty())
                this.output.add(included++, "$include <"+file+">");
        }
        {
            final String files = settings.getProperty(AssemblerSettings.AUTO_INCLUDES);
            if (!files.isEmpty())
                for (String file : files.split("(?<!(?<!\\\\)\\\\);")) {
                    if (!file.isEmpty())
                        this.output.add(included++, "$include <"+
                                file.replaceAll("\\\\\\\\", "\\\\").replaceAll("\\\\;", ";")+">");
                }
        }
        return included;
    }

    /**
     * Creates a Regex for every MCU compatibility directives (BIT, CODE, … etc.).<br>
     * The logic is extracted because all directives basically use the same logic.
     *
     * @param args
     *      the arguments of the directive.
     * @param directiveName
     *      the name of the directive that called this method.
     * @param maxValue
     *      the maximum allowed value for this directive.
     *
     * @return
     *      whether the method was successful.
     */
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

    /**
     * Generates a Regex, to replace a specified symbol with a value.<br>
     * The resulting Regex has always the following format:
     * <pre>
     *     "cs/(?<=[\w,\(])(\s*)\b<i>symbol</i>\b/^(?!\T{directive}).*?$/${1}<i>replacement</i>/"
     *
     *      Flags:
     *        - Case insensitive
     *        - Whole line
     *        - Do not replace in Strings
     *
     *        - Modifiable   (if 'modifiable' is true)
     *        - Unmodifiable (else.)
     * </pre>
     *
     * The Regex cannot have the name of a mnemonic, directive or reserved
     * assembler name (like <code>'a'</code>).
     *
     * @param symbol
     *      the symbol that should be replaced with the value.
     * @param replacement
     *      the String the symbol should be replaced with.
     * @param modifiable
     *      whether the resulting Regex has the modifiable flag set.
     * @param replacing
     *      whether the Regex should be able to replace other Regexes.
     *
     * @return
     *      whether the method was successful.
     */
    private boolean regexFromSymbol(final String symbol, final String replacement,
                                    final boolean modifiable, final boolean replacing) {

        Regex regex = new Regex("cs/(?<=[\\w,\\(])(\\s*)\\b"+symbol+"\\b/^(?!\\T{directive}).*?$/${1}"+replacement+"/"

                +Regex.CASE_INSENSITIVE_FLAG+Regex.WHOLE_LINE_FLAG+Regex.DO_NOT_REPLACE_IN_STRING_FLAG
                +(modifiable ? Regex.MODIFIABLE_FLAG : Regex.UNMODIFIABLE_FLAG),

                currentFile, line, problems);
        if (!regex.isValid())
            return false;

        for (String reserved : MC8051Library.RESERVED_NAMES)
            if (symbol.equalsIgnoreCase(reserved)) {
                problems.add(new PreprocessingProblem("Symbol is a reserved name!",
                        Problem.Type.ERROR, currentFile, line, symbol));
                return false;
            }
        for (Mnemonic reserved : MC8051Library.mnemonics)
            if (symbol.equalsIgnoreCase(reserved.getName())) {
                problems.add(new PreprocessingProblem("Symbol is a mnemonic name!",
                        Problem.Type.ERROR, currentFile, line, symbol));
                return false;
            }
        for (Directive reserved : this.directives)
            if (symbol.equalsIgnoreCase(reserved.getName())) {
                problems.add(new PreprocessingProblem("Symbol is a directive name!",
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
            for (int i = 0; i < regexes.size(); ++i) {
                Regex r = regexes.get(i);
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

    /**
     * Evaluate a boolean condition, for example of the 'if' directive.<br>
     * <br>
     * This method supports querying a setting by using the <code>'--setting'</code>
     * or <code>'-s'</code> option in front of the left operand of a operation.<br>
     * All non setting operands must be valid doubles.<br>
     * <br>
     * Supported evaluation operators:
     * <table>
     *     <tr><th>Operator</th><th>Description</th></tr>
     *     <tr>
     *         <td><code>'=', '=='</code></td>
     *         <td>Equals <code>true</code> if both operands are equal.</td>
     *     </tr>
     *     <tr>
     *         <td><code>'&lt;&gt;', '!='</code></td>
     *         <td>Equals <code>true</code> if both operands are <i>not</i> equal.</td>
     *     </tr>
     *     <tr>
     *         <td><code>'&lt;'</code></td>
     *         <td>Equals <code>true</code> if the left operand is less than the right one.<br>
     *             Tries to convert the right operand onto a number if the left operand is
     *             a setting.</td>
     *     </tr>
     *     <tr>
     *         <td><code>'&lt;='</code></td>
     *         <td>Equals <code>true</code> if the left operand is less than or equal to the
     *             right one.<br>
     *             Tries to convert the right operand onto a number if the left operand is
     *             a setting.</td>
     *     </tr>
     *     <tr>
     *         <td><code>'&gt;'</code></td>
     *         <td>Equals <code>true</code> if the left operand is grater than the right one.<br>
     *             Tries to convert the right operand onto a number if the left operand is
     *             a setting.</td>
     *     </tr>
     *     <tr>
     *         <td><code>'&gt;='</code></td>
     *         <td>Equals <code>true</code> if the left operand is grater than or equal to the
     *             right one.<br>
     *             Tries to convert the right operand onto a number if the left operand is
     *             a setting.</td>
     *     </tr>
     *     <tr>
     *         <td><code><i>No operator</i></code></td>
     *         <td>Equals <code>true</code> if the operand is <i>unequal</i> to <code>0</code>.<br>
     *             If the operand is a setting the value equals <code>true</code> if the value
     *             of the setting equals <code>"true"</code>, <code>false</code> otherwise.</td>
     *     </tr>
     * </table>
     * Boolean operations can be logically connected with these conjunctions:
     * <ul>
     *     <li><code>'AND', '&&', '&'</code> behave like a logical AND gate.</li>
     *     <li><code>'OR', '||', '|'</code> behave like a logical OR gate.</li>
     *     <li><code>'NOT', '!'</code> behave like a logical NOT gate.</li>
     *     <li><code>'[', ']'</code> can be used to change the weight of a conjunction.</li>
     * </ul>
     *
     * @param args
     *      the arguments of the condition. Each argument must be either a valid operand, operator
     *      or conjunction. It is not possible to combine them into one argument!
     * @return
     *      the boolean result of the evaluation.<br>
     *      {@code null} if the expression could not be evaluated due to wrong syntax or other
     *      problems.
     */
    private Boolean evaluateCondition(String[] args) {
        StringBuilder expression = new StringBuilder(42);

        final boolean[] sett = new boolean[3];
        // 0: Setting
        // 1: Ignore errors
        // 2: Errors default
        boolean lastNot = false;

        String left = null, right = null;

        final Settings s = Settings.INSTANCE;
        BiPredicate<String, String> operator = null;
        // Operators
        final Predicate<String> simple = (l) -> {
            try {
                if (sett[0]) return s.getBoolProperty(l);
                else return Double.parseDouble(l) != 0;
            } catch (NumberFormatException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                            Problem.Type.ERROR, currentFile, line, l));
                return sett[2];
            }
        };
        final BiPredicate<String, String> equals = (l, r) -> {
            if (sett[0])
                return s.getProperty(l).equals(r);
            else {
                double lNum = 0, rNum = 0;
                try {
                    lNum = Double.parseDouble(l);
                } catch (NumberFormatException e) {
                    if (!sett[1])
                        problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                                Problem.Type.ERROR, currentFile, line, l));
                    return sett[2];
                }
                try {
                    rNum = Double.parseDouble(r);
                } catch (NumberFormatException e) {
                    if (!sett[1])
                        problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                                Problem.Type.ERROR, currentFile, line, r));
                    return sett[2];
                }
                return lNum == rNum;
            }
        };
        final BiPredicate<String, String> unequals = (l, r) -> {
            if (sett[0])
                return !s.getProperty(l).equals(r);
            else {
                double lNum = 0, rNum = 0;
                try {
                    lNum = Double.parseDouble(l);
                } catch (NumberFormatException e) {
                    if (!sett[1])
                        problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                                Problem.Type.ERROR, currentFile, line, l));
                    return sett[2];
                }
                try {
                    rNum = Double.parseDouble(r);
                } catch (NumberFormatException e) {
                    if (!sett[1])
                        problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                                Problem.Type.ERROR, currentFile, line, r));
                    return sett[2];
                }
                return lNum != rNum;
            }
        };
        final BiPredicate<String, String> lessThan = (l, r) -> {
            double lNum = 0, rNum = 0;
            try {
                if (sett[0]) lNum = Double.parseDouble(s.getProperty(l));
                else lNum = Double.parseDouble(l);
            } catch (NumberFormatException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                            Problem.Type.ERROR, currentFile, line, l));
                return sett[2];
            } catch (NullPointerException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Setting not defined!",
                            Problem.Type.WARNING, currentFile, line, l));
                lNum = Double.NaN;
            }
            try {
                rNum = Double.parseDouble(r);
            } catch (NumberFormatException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                            Problem.Type.ERROR, currentFile, line, r));
                return sett[2];
            }
            return lNum < rNum;
        };
        final BiPredicate<String, String> lessThanEqual = (l, r) -> {
            double lNum = 0, rNum = 0;
            try {
                if (sett[0]) lNum = Double.parseDouble(s.getProperty(l));
                else lNum = Double.parseDouble(l);
            } catch (NumberFormatException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                            Problem.Type.ERROR, currentFile, line, l));
                return sett[2];
            } catch (NullPointerException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Setting not defined!",
                            Problem.Type.WARNING, currentFile, line, l));
                lNum = Double.NaN;
            }
            try {
                rNum = Double.parseDouble(r);
            } catch (NumberFormatException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                            Problem.Type.ERROR, currentFile, line, r));
                return sett[2];
            }
            return lNum <= rNum;
        };
        final BiPredicate<String, String> moreThan = (l, r) -> {
            double lNum = 0, rNum = 0;
            try {
                if (sett[0]) lNum = Double.parseDouble(s.getProperty(l));
                else lNum = Double.parseDouble(l);
            } catch (NumberFormatException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                            Problem.Type.ERROR, currentFile, line, l));
                return sett[2];
            } catch (NullPointerException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Setting not defined!",
                            Problem.Type.WARNING, currentFile, line, l));
                lNum = Double.NaN;
            }
            try {
                rNum = Double.parseDouble(r);
            } catch (NumberFormatException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                            Problem.Type.ERROR, currentFile, line, r));
                return sett[2];
            }
            return lNum > rNum;
        };
        final BiPredicate<String, String> moreThanEqual = (l, r) -> {
            double lNum = 0, rNum = 0;
            try {
                if (sett[0]) lNum = Double.parseDouble(s.getProperty(l));
                else lNum = Double.parseDouble(l);
            } catch (NumberFormatException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                            Problem.Type.ERROR, currentFile, line, l));
                return sett[2];
            } catch (NullPointerException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Setting not defined!",
                            Problem.Type.WARNING, currentFile, line, l));
                lNum = Double.NaN;
            }
            try {
                rNum = Double.parseDouble(r);
            } catch (NumberFormatException e) {
                if (!sett[1])
                    problems.add(new PreprocessingProblem("Value could not be converted to a number!",
                            Problem.Type.ERROR, currentFile, line, r));
                return sett[2];
            }
            return lNum >= rNum;
        };

        // Evaluate
        for (String arg : args) {
            switch (arg) {
                case "-s":
                case "--settings":
                {
                    if (left == null)
                        if (sett[0])
                            problems.add(new PreprocessingProblem("Duplicate option!", Problem.Type.ERROR,
                                    currentFile, line, arg));
                        else
                            sett[0] = true;
                    else
                        problems.add(new PreprocessingProblem("Expect option before the first argument of a statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    continue;
                }
                case "-i":
                case "--ignore-errors":
                {
                    if (left == null)
                        if (sett[1])
                            problems.add(new PreprocessingProblem("Duplicate or incompatible option!",
                                    Problem.Type.ERROR, currentFile, line, arg));
                        else {
                            sett[1] = true;
                            sett[2] = false;
                        }
                    else
                        problems.add(new PreprocessingProblem("Expect option before the first argument of a statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    continue;
                }
                case "-I":
                case "--ignore-errors-default-true":
                {
                    if (left == null)
                        if (sett[1])
                            problems.add(new PreprocessingProblem("Duplicate or incompatible option!",
                                    Problem.Type.ERROR, currentFile, line, arg));
                        else {
                            sett[1] = true;
                            sett[2] = true;
                        }
                    else
                        problems.add(new PreprocessingProblem("Expect option before the first argument of a statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    continue;
                }
            }
            switch (arg.toLowerCase()) {
                case "=":
                case "==":
                {
                    if (left == null)
                        problems.add(new PreprocessingProblem("Expected left side of the boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    if (operator == null)
                        operator = equals;
                    else if (right == null)
                        problems.add(new PreprocessingProblem("Operator already defined!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    else
                        problems.add(new PreprocessingProblem("Expected NOT, a CONJUNCTION or BRACKET!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    break;
                }
                case "!=":
                case "<>":
                {
                    if (left == null)
                        problems.add(new PreprocessingProblem("Expected left side of the boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    if (operator == null)
                        operator = unequals;
                    else if (right == null)
                        problems.add(new PreprocessingProblem("Operator already defined!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    else
                        problems.add(new PreprocessingProblem("Expected NOT, a CONJUNCTION or BRACKET!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    break;
                }
                case "<":
                {
                    if (left == null)
                        problems.add(new PreprocessingProblem("Expected left side of the boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    if (operator == null)
                        operator = lessThan;
                    else if (right == null)
                        problems.add(new PreprocessingProblem("Operator already defined!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    else
                        problems.add(new PreprocessingProblem("Expected NOT, a CONJUNCTION or BRACKET!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    break;
                }
                case "<=":
                {
                    if (left == null)
                        problems.add(new PreprocessingProblem("Expected left side of the boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    if (operator == null)
                        operator = lessThanEqual;
                    else if (right == null)
                        problems.add(new PreprocessingProblem("Operator already defined!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    else
                        problems.add(new PreprocessingProblem("Expected NOT, a CONJUNCTION or BRACKET!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    break;
                }
                case ">":
                {
                    if (left == null)
                        problems.add(new PreprocessingProblem("Expected left side of the boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    if (operator == null)
                        operator = moreThan;
                    else if (right == null)
                        problems.add(new PreprocessingProblem("Operator already defined!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    else
                        problems.add(new PreprocessingProblem("Expected NOT, a CONJUNCTION or BRACKET!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    break;
                }
                case ">=":
                {
                    if (left == null)
                        problems.add(new PreprocessingProblem("Expected left side of the boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    if (operator == null)
                        operator = moreThanEqual;
                    else if (right == null)
                        problems.add(new PreprocessingProblem("Operator already defined!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    else
                        problems.add(new PreprocessingProblem("Expected NOT, a CONJUNCTION or BRACKET!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    break;
                }
                case "[":
                {
                    if (left == null)
                        expression.append(" (");
                    else
                        problems.add(new PreprocessingProblem("Cannot use BRACKET inside a boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    break;
                }
                case "]":
                {
                    if (left == null)
                        expression.append(" )");
                    else
                        problems.add(new PreprocessingProblem("Cannot use BRACKET inside a boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    break;
                }
                case "&":
                case "&&":
                case "and":
                {
                    if (left == null)
                        if (sett[0])
                            problems.add(new PreprocessingProblem("Expected key of a setting!",
                                    Problem.Type.ERROR, currentFile, line, arg));
                        else
                            expression.append(" &");
                    if (left != null && operator == null) {
                        if (lastNot) {
                            if (simple.test(left))
                                expression.append(" 1 ) &");
                            else
                                expression.append(" 0 ) &");
                            lastNot = false;
                        } else {
                            if (simple.test(left))
                                expression.append(" 1 &");
                            else
                                expression.append(" 0 &");
                        }
                    } else if (left != null && right == null)
                        problems.add(new PreprocessingProblem("Expected right side of the boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    else {
                        if (lastNot) {
                            if (operator.test(left, right))
                                expression.append(" 1 ) |");
                            else
                                expression.append(" 0 ) |");
                            lastNot = false;
                        } else {
                            if (operator.test(left, right))
                                expression.append(" 1 |");
                            else
                                expression.append(" 0 |");
                        }
                    }
                    left = right = null;
                    operator = null;
                    sett[0] = false;
                    sett[1] = false;
                    sett[2] = false;
                    break;
                }
                case "|":
                case "||":
                case "or":
                {
                    if (left == null)
                        if (sett[0])
                            problems.add(new PreprocessingProblem("Expected key of a setting!",
                                    Problem.Type.ERROR, currentFile, line, arg));
                        else
                            expression.append(" |");
                    if (left != null && operator == null) {
                        if (lastNot) {
                            if (simple.test(left))
                                expression.append(" 1 ) |");
                            else
                                expression.append(" 0 ) |");
                            lastNot = false;
                        } else {
                            if (simple.test(left))
                                expression.append(" 1 |");
                            else
                                expression.append(" 0 |");
                        }
                    } else if (left != null && right == null)
                        problems.add(new PreprocessingProblem("Expected right side of the boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    else {
                        if (lastNot) {
                            if (operator.test(left, right))
                                expression.append(" 1 ) |");
                            else
                                expression.append(" 0 ) |");
                            lastNot = false;
                        } else {
                            if (operator.test(left, right))
                                expression.append(" 1 |");
                            else
                                expression.append(" 0 |");
                        }
                    }
                    left = right = null;
                    operator = null;
                    sett[0] = false;
                    sett[1] = false;
                    sett[2] = false;
                    break;
                }
                case "~":
                case "!":
                case "not":
                {
                    if (left == null) {
                        if (lastNot)
                            expression.setLength(expression.length() - 8);
                        else
                            expression.append(" ( 1 & ~");
                        lastNot = !lastNot;
                    } else
                        problems.add(new PreprocessingProblem("Cannot use NOT inside a boolean statement!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    break;
                }

                default:
                {
                    if (left == null)
                        left = arg;
                    else if (operator == null)
                        problems.add(new PreprocessingProblem("Expected an operator!",
                                Problem.Type.ERROR, currentFile, line, arg));
                    else if (right == null)
                        right = arg;
                    else
                        problems.add(new PreprocessingProblem("Expected NOT, a CONJUNCTION or BRACKET!",
                                Problem.Type.ERROR, currentFile, line, arg));
                }
            }
        }
        // Clean up
        if (left == null)
            if (sett[0])
                problems.add(new PreprocessingProblem("Expected key of a setting!",
                        Problem.Type.ERROR, currentFile, line, "<EOL>"));
        if (left != null && operator == null) {
            if (lastNot) {
                if (simple.test(left))
                    expression.append(" 1 )");
                else
                    expression.append(" 0 )");
            } else  {
                if (simple.test(left))
                    expression.append(" 1");
                else
                    expression.append(" 0");
            }
        } else if (left != null && right == null)
            problems.add(new PreprocessingProblem("Expected right side of the boolean statement!",
                    Problem.Type.ERROR, currentFile, line, "<EOL>"));
        else {
            if (lastNot) {
                if (operator.test(left, right))
                    expression.append(" 1 )");
                else
                    expression.append(" 0 )");
            } else {
                if (operator.test(left, right))
                    expression.append(" 1");
                else
                    expression.append(" 0");
            }
        }

        try {
            return SimpleMath.evaluate(expression.toString()) != 0;
        } catch (RuntimeException e) {
            problems.add(new PreprocessingProblem("Cannot evaluate condition!",
                    Problem.Type.ERROR, currentFile, line, e.getMessage()));
            return null;
        }
    }

}
