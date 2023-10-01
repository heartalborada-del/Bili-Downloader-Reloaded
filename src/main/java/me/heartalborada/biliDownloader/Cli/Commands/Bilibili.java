package me.heartalborada.biliDownloader.Cli.Commands;

import me.heartalborada.biliDownloader.Cli.Commands.SubCommands.BilibiliSubCommands;
import org.jline.builtins.Commands;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.*;

public class Bilibili extends JlineCommandRegistry implements CommandRegistry {
    private enum Command {
        LOGIN;
    }
    private LineReader reader;

    public Bilibili() {
        super();
        Set<Command> cmds = new HashSet<>(EnumSet.allOf(Command.class));
        Map<Command, String> commandName = new HashMap<>();
        Map<Command, CommandMethods> commandExecute = new HashMap<>();
        for (Command c : cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        commandExecute.put(Command.LOGIN, new CommandMethods(
                this::loginMethod,
                (ignore) -> List.of(
                        new Completer[]{
                                new ArgumentCompleter(
                                        NullCompleter.INSTANCE,new StringsCompleter("qr"), NullCompleter.INSTANCE
                                )
                        })
        ));
        registerCommands(commandName, commandExecute);
    }

    private void loginMethod(CommandInput input) {
        try {
            BilibiliSubCommands.login(input.terminal(), input.out(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    public void setLineReader(LineReader reader) {
        this.reader = reader;
    }

    private List<String> unsetOptions(boolean set) {
        List<String> out = new ArrayList<>();
        for (Option option : Option.values()) {
            if (set == (reader.isSet(option) == option.isDef())) {
                out.add((option.isDef() ? "no-" : "")
                        + option.toString().toLowerCase().replace('_', '-'));
            }
        }
        return out;
    }

    private Set<String> allWidgets() {
        Set<String> out = new HashSet<>();
        for (String s : reader.getWidgets().keySet()) {
            out.add(s);
            out.add(reader.getWidgets().get(s).toString());
        }
        return out;
    }
}