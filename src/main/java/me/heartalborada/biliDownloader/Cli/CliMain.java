package me.heartalborada.biliDownloader.Cli;

import me.heartalborada.biliDownloader.Cli.Commands.Bilibili;
import me.heartalborada.biliDownloader.Main;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.completer.*;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class CliMain {
    private static final String welcome = "\n" +
            "██████╗ ██████╗ ██████╗      ██████╗██╗     ██╗\n" +
            "██╔══██╗██╔══██╗██╔══██╗    ██╔════╝██║     ██║\n" +
            "██████╔╝██║  ██║██████╔╝    ██║     ██║     ██║\n" +
            "██╔══██╗██║  ██║██╔══██╗    ██║     ██║     ██║\n" +
            "██████╔╝██████╔╝██║  ██║    ╚██████╗███████╗██║\n" +
            "╚═════╝ ╚═════╝ ╚═╝  ╚═╝     ╚═════╝╚══════╝╚═╝\n" +
            "Type Ctrl+D or type \"exit\" to quit.          \n";

    public static void main(String[] args) throws Exception {
        DefaultParser parser = new DefaultParser();
        parser.setEofOnUnclosedQuote(true);
        parser.setEscapeChars(null);
        parser.setRegexVariable(null);

        Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .signalHandler(Terminal.SignalHandler.SIG_IGN)
                .jansi(true)
                .jna(true)
                .build();
        Thread executeThread = Thread.currentThread();
        terminal.handle(Terminal.Signal.INT, signal -> executeThread.interrupt());
        Supplier<Path> workDir = () -> Paths.get(Main.getDataPath().getPath());
        CommandRegistry builtins = new Bilibili();
        SystemRegistryImpl systemRegistry = new SystemRegistryImpl(parser,terminal, workDir,null);
        systemRegistry.setCommandRegistries(builtins);
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
                        System.err.println(e.getMessage());
                    }
                } else {
                    System.out.printf("%n");
                }
            } catch (EndOfFileException e) {
                // user cancelled application with Ctrl+D or kill
                break;
            } catch (Throwable t) {
                throw new Exception("Could not read from command line.", t);
            }
        }
    }
}
