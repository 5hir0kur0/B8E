package emulator;

import java.util.List;

/**
 * This interface represents a general CPU emulator.
 *
 * @author 5hir0kur0
 */
public interface Emulator {
    /**
     * @return all the {@code Register}s of the CPU
     */
    List<Register> getRegisters();

    /**
     * @return the program status word {@code FlagRegister} of the CPU
     */
    FlagRegister getPSW();

    /**
     * Execute the next instruction. After the last instruction it finds, the {@code Emulator} will start at the first
     * instruction again.
     * @return
     *     the number of machine cycles the instruction takes to execute on real hardware
     * @throws EmulatorException
     *     when any kind of {@code Exception} (or {@code RuntimeException}) is thrown during emulation
     *     (The user should be given the choice to continue running the program regardless.)
     */
    int next() throws EmulatorException;

    /**
     * @return the CPU's primary (usu. internal) {@code RAM} (This can potentially be the only {@code RAM} module the
     * CPU has access to.)
     */
    RAM getMainMemory();

    /**
     * If there is a secondary {@code RAM} module this method should be overwritten to return it.
     * @return
     *     the CPU's secondary {@code RAM} module, if it has one
     * @throws UnsupportedOperationException
     *     if there is no secondary {@code RAM} module
     */
    default RAM getSecondaryMemory() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Emulator::getSecondaryMemory");
    }

    /**
     * If the CPU has a secondary {@code RAM} module, this method should be overwritten to return {@code true}
     * @return
     *     {@code true} if there is a secondary {@code RAM} module; {@code false} if there isn't one
     */
    default boolean hasSecondaryMemory() {
        return false;
    }

    /**
     * Get the code memory.
     * Not all CPUs may support this.
     * @return
     *     {@code ROM} representing the code memory
     * @throws UnsupportedOperationException
     *     if the CPU does not have a code memory module
     */
    default ROM getCodeMemory() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Emulator::getCodeMemory");
    }

    /**
     * Return whether the CPU has a code memory module.
     * @return
     *     {@code true} if the CPU has code memory; {@code false} otherwise
     */
    default boolean hasCodeMemory() {
        return false;
    }
}
