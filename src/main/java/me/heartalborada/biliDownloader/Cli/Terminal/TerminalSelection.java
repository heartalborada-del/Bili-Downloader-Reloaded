package me.heartalborada.biliDownloader.Cli.Terminal;

import me.heartalborada.biliDownloader.Cli.Enums.KeyOperation;
import me.heartalborada.biliDownloader.Interfaces.Selection;
import me.heartalborada.biliDownloader.Interfaces.SelectionCallback;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.*;

import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.key;

public class TerminalSelection<T> implements Selection<T> {
    private final LinkedHashMap<T,SelectionCallback<T>> map = new LinkedHashMap<>();
    private final boolean isDumb;
    private final Display display;
    private final int width;
    private final ExecutorService service = Executors.newFixedThreadPool(1);
    public TerminalSelection(Terminal terminal) {
        width = terminal.getWidth() == 0 ? 20 : terminal.getWidth();
        isDumb = terminal instanceof DumbTerminal;
        this.display = new Display(terminal,false);
        if(!terminal.paused()) terminal.pause();
        if(isDumb) {
            service.submit(() -> {
                int selectedNum = -1;
                KeyMap<KeyOperation> keys = new KeyMap<>();
                BindingReader bindingReader = new BindingReader(terminal.reader());
                keys.bind(KeyOperation.UP,key(terminal, InfoCmp.Capability.key_up));
                keys.bind(KeyOperation.DOWN,key(terminal, InfoCmp.Capability.key_down));
                keys.bind(KeyOperation.ENTER,key(terminal,InfoCmp.Capability.key_enter));
                keys.bind(KeyOperation.CTRL_C,ctrl('C'));
                while(true) {
                    KeyOperation key = bindingReader.readBinding(keys);
                    switch (key){
                        case UP:
                            if(selectedNum+1>=map.size()) selectedNum = 0;
                            else selectedNum++;
                            break;
                        case DOWN:
                            if(selectedNum-1<0) selectedNum = map.size()-1;
                            else selectedNum--;
                            break;
                        case CTRL_C:
                            //TODO QUIT
                            break;
                        case ENTER:
                            //TODO QUIT
                            break;
                    }
                }
            });
        } else {
            service.submit(() -> {
                NonBlockingReader nonBlockingReader = terminal.reader();
                LinkedList<Character> characters = new LinkedList<Character>();
                while (true) {
                    try {
                        int i = nonBlockingReader.read();
                        //Press Backspace
                        if(i==8) {
                            characters.removeLast();
                        }//Press Enter
                        else if (i==13) {
                            characters.clear();
                            //TODO QUIT
                        }//Number Key
                        else if(i>=48&&i<=57) {
                            characters.add((char)i);
                        }
                    } catch (InterruptedIOException ignore) {
                        //TODO QUIT
                    }
                }
            });
        }
    }

    @Override
    public void bind(T obj, SelectionCallback<T> cb) {
        map.put(obj,cb);
    }

    @Override
    public void rerender() {
        /*if(isDumb) {
            this.display.resize(map.size()+1,width);
            generateSequenceCollection(map.keySet(),null);
        } else {
            this.display.resize(map.size(),width);
            generateSequenceCollection(map.keySet(),getSetElement(map.keySet(),selectedNum));
        }*/
    }

    @Override
    public void close() {
        service.shutdownNow();
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

    private T getSetElement(Set<T> set, int selected) {
        int i=0;
        for (T obj : set) {
            if(i==selected)
                return obj;
            i++;
        }
        return null;
    }
}
