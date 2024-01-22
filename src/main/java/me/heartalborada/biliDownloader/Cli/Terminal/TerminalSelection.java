package me.heartalborada.biliDownloader.Cli.Terminal;

import me.heartalborada.biliDownloader.Cli.Enums.KeyOperation;
import me.heartalborada.biliDownloader.Interfaces.Selection;
import me.heartalborada.biliDownloader.Interfaces.SelectionCallback;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.*;

import java.util.*;

import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.key;

public class TerminalSelection<T> implements Selection<T> {
    private final LinkedHashMap<T,SelectionCallback<T>> map = new LinkedHashMap<>();
    private final BindingReader bindingReader;
    private final KeyMap<KeyOperation> keys = new KeyMap<>();
    private boolean isDumb;
    private final Display display;
    private final int width;
    public TerminalSelection(Terminal terminal) {
        width = terminal.getWidth() == 0 ? 20 : terminal.getWidth();
        isDumb = terminal instanceof DumbTerminal;
        this.bindingReader = new BindingReader(terminal.reader());
        keys.bind(KeyOperation.UP,key(terminal, InfoCmp.Capability.key_up));
        keys.bind(KeyOperation.DOWN,key(terminal, InfoCmp.Capability.key_down));
        keys.bind(KeyOperation.CTRL_C,ctrl('C'));
        this.display = new Display(terminal,false);
    }

    @Override
    public void bind(T obj, SelectionCallback<T> cb) {
        map.put(obj,cb);
    }

    @Override
    public void rerender() {
        if(isDumb) {
            this.display.resize(map.size()+1,width);
        } else {
            this.display.resize(map.size(),width);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void binds(Map<T,SelectionCallback<T>> map) {
        this.map.putAll(map);
    }

    private Collection<AttributedCharSequence> generateSequenceCollection(Set<T> set, T highLightRow) {
        Collection<AttributedCharSequence> charSequences = new LinkedList<>();
        for (T o:set) {
            AttributedStringBuilder builder = new AttributedStringBuilder().append("•").append(o.toString());
            if(o == highLightRow)
                builder.style(new AttributedStyle().foregroundRgb(0x00FF7F));
            charSequences.add(builder.toAttributedString());
        }
        return charSequences;
    }
}
