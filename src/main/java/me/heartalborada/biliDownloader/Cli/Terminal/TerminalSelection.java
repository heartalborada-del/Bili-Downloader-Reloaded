package me.heartalborada.biliDownloader.Cli.Terminal;

import lombok.Getter;
import me.heartalborada.biliDownloader.Cli.Enums.KeyOperation;
import me.heartalborada.biliDownloader.Interfaces.Selection;
import me.heartalborada.biliDownloader.Interfaces.SelectionCallback;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jline.keymap.KeyMap.ctrl;
import static org.jline.keymap.KeyMap.key;

public class TerminalSelection<T> implements Selection<T> {
    private final ConcurrentHashMap<T,SelectionCallback<T>> binds = new ConcurrentHashMap<>();
    @Getter
    volatile private boolean isClosed = false;
    private final Display display;
    private final int width;
    private final ExecutorService service = Executors.newFixedThreadPool(1);
    private final AtomicInteger selectedNum = new AtomicInteger(-1);
    private final Terminal terminal;
    public TerminalSelection(Terminal terminal, Map<T,SelectionCallback<T>> binds) {
        this.binds.putAll(binds);
        width = terminal.getWidth() == 0 ? 20 : terminal.getWidth();
        this.terminal = terminal;
        this.display = new Display(terminal,false);
        //if(!terminal.paused()) terminal.pause();
        if(!(terminal instanceof DumbTerminal)) {
            this.display.resize(this.binds.size(),width);
            service.submit(() -> {
                KeyMap<KeyOperation> keys = new KeyMap<>();
                BindingReader bindingReader = new BindingReaderM(terminal.reader());
                keys.bind(KeyOperation.UP,key(terminal, InfoCmp.Capability.key_up));
                keys.bind(KeyOperation.DOWN,key(terminal, InfoCmp.Capability.key_down));
                keys.bind(KeyOperation.ENTER,key(terminal,InfoCmp.Capability.key_enter));
                keys.bind(KeyOperation.ENTER, String.valueOf((char) 13));
                keys.bind(KeyOperation.CTRL_C,ctrl('c'));
                while(!isClosed) {
                    KeyOperation key = bindingReader.readBinding(keys);
                    switch (key) {
                        case UP:
                            if (selectedNum.get() + 1 >= this.binds.size()) selectedNum.set(0);
                            else selectedNum.incrementAndGet();
                            break;
                        case DOWN:
                            if (selectedNum.get() - 1 < 0) selectedNum.set(this.binds.size() - 1);
                            else selectedNum.decrementAndGet();
                            break;
                        case CTRL_C:
                            this.close();
                            break;
                        case ENTER:
                            T Tobj = null;
                            int i = 0;
                            for (T obj : this.binds.keySet()) {
                                if (i == selectedNum.get())
                                    Tobj = obj;
                                i++;
                            }
                            if (Tobj == null) break;
                            this.binds.get(Tobj).onSelected(Tobj);
                            this.close();
                            break;
                    }
                }
            });
        } else {
            this.display.resize(this.binds.size()+1,width);
            service.submit(() -> {
                NonBlockingReader nonBlockingReader = terminal.reader();
                LinkedList<Character> characters = new LinkedList<>();
                while (!isClosed) {
                    try {
                        int i = nonBlockingReader.read();
                        //Press Backspace
                        if(i==8) {
                            characters.removeLast();
                        }//Press Enter
                        else if (i==13) {
                            StringBuilder stringBuilder = new StringBuilder(characters.size());
                            for (Character character : characters) stringBuilder.append(character);
                            int parsed = Integer.parseInt(stringBuilder.toString());
                            if (checkValid(parsed)) {
                                this.close();
                            }
                            characters.clear();
                        }//Number Key
                        else if(i>=48&&i<=57) {
                            characters.add((char)i);
                        }
                    } catch (InterruptedIOException ignore) {
                        this.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void bind(T obj, SelectionCallback<T> cb) {
        binds.put(obj,cb);
    }

    @Override
    public void rerender() {
        if(terminal instanceof DumbTerminal) {
            this.display.resize(binds.size()+1,width);
            List<AttributedString> c = generateSequenceCollection(binds.keySet(),null);
            this.display.update(c,terminal.getSize().cursorPos(1,0));
        } else {
            this.display.resize(binds.size(),width);
            List<AttributedString> c = generateSequenceCollection(binds.keySet(),getSetElement(binds.keySet(),selectedNum.get()));
            this.display.update(c,terminal.getSize().cursorPos(1,0));
        }
    }

    @Override
    public void close() {
        isClosed = true;
        service.shutdownNow();
    }

    @Override
    public void binds(Map<T,SelectionCallback<T>> map) {
        this.binds.putAll(map);
        rerender();
    }

    private List<AttributedString> generateSequenceCollection(Set<T> set, T highLightRow) {
        LinkedList<AttributedString> charSequences = new LinkedList<>();
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

    private boolean checkValid(int n) {
        if(n>binds.size()-1) return false;
        else return n >= 0;
    }

}
