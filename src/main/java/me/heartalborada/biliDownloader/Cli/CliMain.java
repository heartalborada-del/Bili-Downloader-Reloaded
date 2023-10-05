package me.heartalborada.biliDownloader.Cli;

import lombok.Getter;
import me.heartalborada.biliDownloader.Main;
import me.heartalborada.biliDownloader.Utils.LoggerFormatter;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class CliMain {
    private static final String welcome = "\n" +
            "██████╗ ██████╗ ██████╗      ██████╗██╗     ██╗\n" +
            "██╔══██╗██╔══██╗██╔══██╗    ██╔════╝██║     ██║\n" +
            "██████╔╝██║  ██║██████╔╝    ██║     ██║     ██║\n" +
            "██╔══██╗██║  ██║██╔══██╗    ██║     ██║     ██║\n" +
            "██████╔╝██████╔╝██║  ██║    ╚██████╗███████╗██║\n" +
            "╚═════╝ ╚═════╝ ╚═╝  ╚═╝     ╚═════╝╚══════╝╚═╝\n" +
            "Type Ctrl+D or type \"exit\" to quit.          \n";
    @Getter
    private static final Terminal terminal;

    static {
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                        .signalHandler(Terminal.SignalHandler.SIG_IGN)
                        .jansi(true)
                        .jna(true)
                        .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        Field logField = org.jline.utils.Log.class.getDeclaredField("logger");
        logField.setAccessible(true);
        Field mod = logField.getClass().getDeclaredField("modifiers");
        mod.setAccessible(true);
        mod.setInt(logField,logField.getModifiers() &~ Modifier.FINAL);
        Logger tmp = Logger.getLogger("JLine");
        LoggerFormatter.installFormatter(tmp);
        logField.set(org.jline.utils.Log.class, tmp);

        DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedQuote(true);
        parser.setEscapeChars(null);
        parser.setRegexVariable(null);

        Thread executeThread = Thread.currentThread();
        terminal.handle(Terminal.Signal.INT, signal -> executeThread.interrupt());
        Supplier<Path> workDir = () -> Paths.get(Main.getDataPath().getPath());
        SystemRegistryImpl systemRegistry = new SystemRegistryImpl(parser,terminal, workDir,null);

        Commands commands = new Commands(terminal);
        CommandLine.IFactory factory = new InnerClassFactory(commands);

        CommandLine commandLine = new CommandLine(commands,factory);
        PicocliCommands picocliCommands = new PicocliCommands(commandLine);

        systemRegistry.setCommandRegistries(picocliCommands);

        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .appName("BDR CLI")
                .parser(parser)
                .completer(systemRegistry.completer())
                .build();
        lineReader.setVariable(LineReader.BLINK_MATCHING_PAREN, 0);

        terminal.writer().append(welcome);

        lineReader.setAutosuggestion(LineReader.SuggestionType.COMPLETER);
        String prompt = "BDR CLI> ";
        while (true) {
            terminal.writer().append("\n");
            terminal.flush();

            final String line;
            try {
                systemRegistry.cleanUp();
                line = lineReader.readLine(prompt).trim();
                if(!line.equals("")) {
                    try {
                        Object result = systemRegistry.execute(line);
                        if (result != null) {
                            System.out.println(result);
                        }
                    } catch (IllegalArgumentException | SystemRegistryImpl.UnknownCommandException e) {
                        System.err.printf("\33[1;31m%s\33[0m%n",e.getMessage());
                    }
                } else {
                    System.out.printf("%n");
                }
            } catch (UserInterruptException ignore) {}
            catch (EndOfFileException e) {
                // user cancelled application with Ctrl+D or kill
                return;
            } catch (Exception e) {
                if(terminal.paused())
                    terminal.resume();
                systemRegistry.trace(e);
            }
        }
    }
}
