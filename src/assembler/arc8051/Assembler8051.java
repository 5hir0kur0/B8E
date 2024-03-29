package assembler.arc8051;

import assembler.Assembler;
import assembler.tokens.LabelToken;
import assembler.tokens.Token;
import assembler.util.AssemblerSettings;
import assembler.util.HexWriter;
import assembler.util.Listing;
import assembler.util.assembling.Assembled;
import assembler.util.problems.*;
import misc.Logger;
import misc.Settings;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Noxgrim
 */
public class Assembler8051 implements Assembler {

    /** The associated preprocessor. */
    private Preprocessor8051 preprocessor;
    /** The associated preprocessor. */
    private Tokenizer8051  tokenizer;

    /** Holds the result of the last assembling. */
    private byte[] result;
    /** Whether the last assembling was successful. */
    private boolean successful;
    /** The {@link assembler.util.Listing} of the last assembling. */
    private Listing listing;

    static {
        AssemblerSettings.init(); // Load settings
    }

    /** Constructs a new Assembler for the 8051 architecture. */
    public Assembler8051() {
        this.preprocessor  = new Preprocessor8051();
        this.tokenizer = new Tokenizer8051();
    }

    @Override
    public byte[] getResult() {
        return result;
    }

    @Override
    public boolean wasSuccessful() {
        return successful;
    }

    @Override
    public Listing getListing() {
        return listing;
    }

    @Override
    public byte[] assemble(Path source, Path directory, List<Problem<?>> problems) {
        Logger.log("Start assembling process of '" + source + "' in reference directory '" + directory + "'.",
                Assembler.class, Logger.LogLevel.INFO);
        result = new byte[0xFFFF+1];
        successful = true;

        List<LabelToken> labels = new LinkedList<>();

        List<Token> tokens = getTokens(source, directory, problems);
        if (tokens == null)
            return result;

        List<Assembled8051> assembled = toAssembled(tokens, labels, problems, source);

        Logger.log("Resolving labels…", Assembler.class, Logger.LogLevel.INFO);
        resolve(assembled, labels, problems, Integer.MAX_VALUE);
        Logger.log("Resolved labels.", Assembler.class, Logger.LogLevel.INFO);

        int actualBytes = writeBinaryToArray(result, assembled, problems);

        Logger.log("Assembling finished. Result holds " + actualBytes + " byte" + (actualBytes == 1 ? "" : "s") + ".",
                Assembler.class, Logger.LogLevel.INFO);
        Logger.log("Writing files…", Assembler.class, Logger.LogLevel.INFO);
        writeFiles(directory, source, actualBytes, assembled, problems);
        Logger.log("Writing files finished.", Assembler.class, Logger.LogLevel.INFO);

        if (!checkErrors(AssemblerSettings.STOP_ASSEMBLER, problems, TokenProblem.class, "assembling") ||
            !checkErrors(AssemblerSettings.STOP_ASSEMBLER, problems, ExceptionProblem.class, "assembling"))
            return result = new byte[0xFFFF+1];

        Collections.sort(problems);

        Logger.log("Assembling process finished. Encountered " + problems.size() +
                " problem" + (problems.size() == 1 ? "." : "s."), Assembler.class, Logger.LogLevel.INFO);
        return result;
    }

    private List<Token> getTokens(Path source, Path directory, List<Problem<?>> problems) {
        List<String> inputOutput = new LinkedList<>();
        problems.addAll(preprocessor.preprocess(directory, source, inputOutput));
        if (!checkErrors(AssemblerSettings.STOP_PREPROCESSOR, problems, PreprocessingProblem.class, "preprocessing") ||
            !checkErrors(AssemblerSettings.STOP_PREPROCESSOR, problems, ExceptionProblem.class, "preprocessing"))
            return null;

        List<Token> output = tokenizer.tokenize(inputOutput, problems);
        if (!checkErrors(AssemblerSettings.STOP_TOKENIZER, problems, TokenizingProblem.class, "tokenizing") ||
            !checkErrors(AssemblerSettings.STOP_TOKENIZER, problems, ExceptionProblem.class, "tokenizing"))
            return null;
        return output;
    }

    private List<Assembled8051> toAssembled(List<Token> tokens, List<LabelToken> labels,
                                            List<Problem<?>> problems, Path source) {
        Logger.log("Generating Assembled objects…", Assembler.class, Logger.LogLevel.DEBUG);
        int origin = 0;
        Path currentFile = source;

        List<Assembled8051> result = new ArrayList<>(tokens.size()/3);

        List<Token> localList = new LinkedList<>();
        List<LabelToken> localLabels = new LinkedList<>();

        for (Token t : tokens) {
            switch (t.getType()) {
                case MNEMONIC_NAME:
                {
                    if (localList.size() > 0) {
                        result.add(new Assembled8051(origin, 0, localList, localLabels, currentFile));
                        localList.clear();
                        localLabels.clear();
                    }
                    localList.add(t);
                    break;
                }
                case OPERAND:
                {
                    localList.add(t); // Assume that the tokenizer worked
                    break;
                }
                case LABEL:
                {
                    if (localList.size() > 0) { // Assume that a new Mnemonic has started
                        result.add(new Assembled8051(origin, 0, localList, localLabels, currentFile));
                        localList.clear();
                        localLabels.clear();
                    }
                    LabelToken lt = (LabelToken) t;
                    lt.setOrigin(origin);
                    labels.add(lt);
                    localLabels.add(lt);
                    break;
                }
                case DIRECTIVE:
                {
                    if (t instanceof DirectiveTokens.FileChangeToken)
                        currentFile = ((DirectiveTokens.FileChangeToken) t).getFile();
                    else if (t instanceof DirectiveTokens.DataToken) {
                        if (localList.size() > 0) {
                            result.add(new Assembled8051(origin, 0, localList, localLabels, currentFile));
                            localList.clear();
                            localLabels.clear();
                        }

                        localList.add(t);   // A data token is always a single Assembled.
                        result.add(new Assembled8051(origin, 0, localList, localLabels, currentFile));
                        localList.clear();
                        localLabels.clear();

                    } else if (t instanceof DirectiveTokens.OriginChangeToken) {
                        origin = (int) ((DirectiveTokens.OriginChangeToken) t).getAddress();
                        for (LabelToken lt : localLabels) {
                            problems.add(new TokenProblem("Label has no associated instruction!", Problem.Type.ERROR,
                                    currentFile, lt)); // Origin and all addresses would change afterwards, making the
                            labels.remove(lt);         // label referring to nothing,
                        }
                        localLabels.clear();
                    } else
                        throw new IllegalStateException("Unknown directive token:" + t);
                    break;
                }
            }
        }

        if (localList.isEmpty())
            for (LabelToken lt : localLabels) {
                problems.add(new TokenProblem("Label has no associated instruction!", Problem.Type.ERROR,
                        currentFile, lt)); // Label at EOF
                labels.remove(lt);
            }
        else
            result.add(new Assembled8051(origin, 0, localList, localLabels, currentFile));

        Logger.log("Generating of " + result.size() + " Assembled objects finished.",
                Assembler.class, Logger.LogLevel.DEBUG);
        return result;
    }

    private int resolve(List<Assembled8051> assembled, List<LabelToken> labels, List<Problem<?>> problems,
                         final int upTo) {
        int origin = 0;
        int change = 0;

        for (int i = 0; i < upTo && i < assembled.size(); ++i) {

            Assembled8051 a = assembled.get(i);

            if (a.getOrigin() != origin) {
                origin = (int) a.getOrigin();
                change = 0;
            }

            a.moveAddress(change);

            if (change != 0 && a.hasLabels())
               change += resolve(assembled, labels, problems, i+1);
            else
                change += a.compile(problems, labels);

        }

        return change;
    }

    private int writeBinaryToArray(byte[] out, List<Assembled8051> assembled, List<Problem<?>> problems) {

        final int maxAddr = out.length-1;
        int maxAddrWritten = -1;
        for (Assembled8051 a : assembled) {
            int address = (int) a.getAddress();
            if (address > maxAddr) {
                problems.add(new TokenProblem("Address of instruction outside of the valid code memory area!:" + address,
                        Problem.Type.ERROR, a.getFile(), a.getTokens()[0]));
                continue;
            }
            byte[] codes = a.getCodes();
            for (int i = 0; i < codes.length; i++) {
                if (address + i > maxAddr) {
                    problems.add(new TokenProblem("Only " + i + " byte" + (i == 1 ? "" : "s") + "of instruction are in " +
                            "the valid code memory area!:" + address, Problem.Type.ERROR, a.getFile(), a.getTokens()[0]));
                    continue;
                } else if (address + i > maxAddrWritten)
                    maxAddrWritten = address + i;

                out[address + i] = codes[i];
            }
        }
        return maxAddrWritten + 1;
    }

    private void writeFiles(Path directory, Path file, final int actualBytes,
                            List<? extends Assembled> assembled, List<Problem<?>> problems) {
        Settings s = Settings.INSTANCE;

        // Write Intel HEX
        if (s.getBoolProperty(AssemblerSettings.OUTPUT_HEX))
            try (HexWriter hw = new HexWriter(Files.newBufferedWriter(getFile(directory, file,
                    s.getProperty(AssemblerSettings.OUTPUT_HEX_EXTENSION, AssemblerSettings.VALID_FILE_EXTENSION))),
                    s.getIntProperty(AssemblerSettings.OUTPUT_HEX_BUFFER_LENGTH, i -> i > 0))) {

                Logger.log("Writing Intel HEX file…", Assembler.class, Logger.LogLevel.INFO);
                hw.writeAll(assembled, s.getBoolProperty(AssemblerSettings.OUTPUT_HEX_WRAP));
                Logger.log("Writing Intel HEX file finished.", Assembler.class, Logger.LogLevel.INFO);

            } catch (Exception e) {
                problems.add(new ExceptionProblem("Could not write HEX file!", Problem.Type.ERROR, e));
                Logger.log("Could not write HEX file!", Assembler.class, Logger.LogLevel.DEBUG);
                Logger.logThrowable(e, Assembler.class, Logger.LogLevel.DEBUG);
            }
        else
            Logger.log("Writing Intel HEX file omitted.", Assembler.class, Logger.LogLevel.INFO);


        this.listing = new Listing(assembled, 16);
        Logger.log("Created Listing.", Assembler.class, Logger.LogLevel.DEBUG);

        // Write Listing
        if (s.getBoolProperty(AssemblerSettings.OUTPUT_LST)) {
            try (BufferedWriter bf = Files.newBufferedWriter(getFile(directory, file,
                    s.getProperty(AssemblerSettings.OUTPUT_LST_EXTENSION)))) {

                Logger.log("Writing listing file…", Assembler.class, Logger.LogLevel.INFO);
                this.listing.writeAll(bf);
                Logger.log("Writing listing file finished.", Assembler.class, Logger.LogLevel.INFO);

            } catch (IOException e) {
                problems.add(new ExceptionProblem("Could not write listing file!", Problem.Type.ERROR, e));
                Logger.log("Could not write listing file!", Assembler.class, Logger.LogLevel.DEBUG);
                Logger.logThrowable(e, Assembler.class, Logger.LogLevel.DEBUG);
            }
        } else
            Logger.log("Writing listing file omitted.", Assembler.class, Logger.LogLevel.INFO);

        // Write binary
        if (s.getBoolProperty(AssemblerSettings.OUTPUT_BIN)) {
           try (OutputStream os = Files.newOutputStream(getFile(directory, file,
                   s.getProperty(AssemblerSettings.OUTPUT_BIN_EXTENSION, AssemblerSettings.VALID_FILE_EXTENSION)))) {

               Logger.log("Writing binary file…", Assembler.class, Logger.LogLevel.INFO);
               if (s.getBoolProperty(AssemblerSettings.OUTPUT_BIN_NECESSARY))
                   os.write(result, 0 , actualBytes);
               else
                   os.write(result);
               Logger.log("Writing binary file finished.", Assembler.class, Logger.LogLevel.INFO);

           } catch (Exception e) {
                problems.add(new ExceptionProblem("Could not write binary file!", Problem.Type.ERROR, e));
               Logger.log("Could not write binary file!", Assembler.class, Logger.LogLevel.DEBUG);
               Logger.logThrowable(e, Assembler.class, Logger.LogLevel.DEBUG);
            }
        } else
            Logger.log("Writing binary file omitted.", Assembler.class, Logger.LogLevel.INFO);
    }

    private Path getFile(Path directory, Path file, String extension) {
        String fileName = file.getFileName().toString();
        final String outDir = Settings.INSTANCE.getProperty(AssemblerSettings.OUTPUT_DIR);
        if (outDir.isEmpty())
            return Paths.get(file.getParent().toString(), fileName.substring(0, fileName.lastIndexOf('.')) + extension);
        else
            return Paths.get(directory.toString(), outDir, fileName.substring(0, fileName.lastIndexOf('.')) + extension);

    }
    private boolean checkErrors(String setting, List<Problem<?>> problems, Class<? extends Problem> searchedType,
                                String workName) {
        Problem.Type[] type = {AssemblerSettings.getStopPoint(
                Settings.INSTANCE.getProperty(setting, AssemblerSettings.VALID_STOP_POINT))};
        Optional<Problem<?>> found = problems.stream().filter(
                p -> searchedType.isInstance(p) && p.getType() == type[0]).findFirst();
        if (found.isPresent()) {
            Problem cause = found.get();
            final String msg = "Stopped assembling due to a" +
                    ("AEIOU".contains(Character.toString(cause.getType().name().charAt(0))) ? "n " : " ") + cause.getType() +
                    " while " +  workName + " the file.";
            problems.add(new Problem<Problem<?>>(msg, Problem.Type.INFORMATION,
                    cause.getPath(), cause.getLine(), cause));
            Logger.log(msg, Assembler.class, Logger.LogLevel.INFO);
            return this.successful = false;
        }
        return true;
    }
}
