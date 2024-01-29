package me.heartalborada.biliDownloader.Cli.Terminal;

import lombok.Getter;
import lombok.SneakyThrows;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final LinkedList<Character> characters = new LinkedList<>();
    private final Terminal terminal;
    private boolean isBegin = false;
    private volatile AttributedString dumbSuggestionString;
    public TerminalSelection(Terminal terminal, Map<T,SelectionCallback<T>> binds) {
        this.binds.putAll(binds);
        this.width = terminal.getWidth() == 0 ? 20 : terminal.getWidth();
        this.terminal = terminal;
        this.display = new Display(terminal,false);
        //if(!terminal.paused()) terminal.pause();
    }

    @Override
    public void bind(T obj, SelectionCallback<T> cb) {
        this.binds.put(obj,cb);
    }

    @Override
    public void rerender() {
        if(this.terminal instanceof DumbTerminal) {
            List<AttributedString> c = generateSequenceCollection(true,this.binds.keySet(),null);
            StringBuilder stringBuilder = new StringBuilder(this.characters.size());
            for (Character character : this.characters) stringBuilder.append(character);
            AttributedStringBuilder builder = new AttributedStringBuilder()
                    .style(new AttributedStyle().background(0xFFC0CB))
                    .append("Input Number: ")
                    .style(new AttributedStyle().background(0x708090).foreground(0x0))
                    .append(stringBuilder.toString());
            c.add(builder.toAttributedString());
            if(this.dumbSuggestionString != null) {
                c.add(this.dumbSuggestionString);
                this.display.resize(binds.size()+2,width);
                this.display.update(c, terminal.getSize().cursorPos(binds.size() + 2, 0));
            } else {
                this.display.resize(binds.size()+1,width);
                this.display.update(c, terminal.getSize().cursorPos(binds.size() + 1, 0));
            }
        } else {
            this.display.resize(binds.size(),width);
            List<AttributedString> c = generateSequenceCollection(false, binds.keySet(),getSetElement(binds.keySet(),selectedNum.get()));
            this.display.update(c,terminal.getSize().cursorPos(binds.size(),0));
        }
    }

    @Override
    public void close() {
        isClosed = true;
        service.shutdownNow();
    }

    @SneakyThrows
    @Override
    public void begin() {
        if(isBegin) return;
        this.isBegin = true;
        if(!(this.terminal instanceof DumbTerminal)) {
            this.display.resize(this.binds.size(),width);
            rerender();
            this.service.submit(() -> {
                KeyMap<KeyOperation> keys = new KeyMap<>();
                BindingReader bindingReader = new BindingReaderM(terminal.reader());
                keys.bind(KeyOperation.UP,key(terminal, InfoCmp.Capability.key_up));
                keys.bind(KeyOperation.DOWN,key(terminal, InfoCmp.Capability.key_down));
                keys.bind(KeyOperation.ENTER,key(terminal,InfoCmp.Capability.key_enter));
                keys.bind(KeyOperation.ENTER, String.valueOf((char) 13));
                keys.bind(KeyOperation.CTRL_C,ctrl('c'));
                while(!this.isClosed) {
                    KeyOperation key = bindingReader.readBinding(keys);
                    switch (key) {
                        case UP:
                            if (this.selectedNum.get() + 1 >= this.binds.size()) this.selectedNum.set(this.binds.size() - 1);
                            else this.selectedNum.incrementAndGet();
                            rerender();
                            break;
                        case DOWN:
                            if (this.selectedNum.get() - 1 < 0) this.selectedNum.set(0);
                            else this.selectedNum.decrementAndGet();
                            rerender();
                            break;
                        case CTRL_C:
                            this.close();
                            rerender();
                            break;
                        case ENTER:
                            T Tobj = null;
                            int i = 0;
                            for (T obj : this.binds.keySet()) {
                                if (i == this.selectedNum.get())
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
            rerender();
            this.service.submit(() -> {
                NonBlockingReader nonBlockingReader = this.terminal.reader();
                while (!this.isClosed) {
                    try {
                        int i = nonBlockingReader.read();
                        //Press Backspace
                        if(i==8) {
                            this.characters.removeLast();
                            rerender();
                        }//Press Enter
                        else if (i==13) {
                            StringBuilder stringBuilder = new StringBuilder(characters.size());
                            for (Character character : characters) stringBuilder.append(character);
                            int parsed = Integer.parseInt(stringBuilder.toString());
                            if (checkValid(parsed)) {
                                this.dumbSuggestionString = null;
                                T Tobj = null;
                                int i2 = 0;
                                for (T obj : this.binds.keySet()) {
                                    if (i2 == this.selectedNum.get())
                                        Tobj = obj;
                                    i2++;
                                }
                                if (Tobj == null) break;
                                this.binds.get(Tobj).onSelected(Tobj);
                                this.close();
                            } else {
                                this.dumbSuggestionString = new AttributedStringBuilder()
                                        .style(new AttributedStyle().background(0XDC143C).foreground(0x0))
                                        .append("Invalid input")
                                        .toAttributedString();
                                this.characters.clear();
                            }
                            rerender();
                        }//Number Key
                        else if(i>=48&&i<=57) {
                            this.characters.add((char)i);
                            rerender();
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
    public void binds(Map<T,SelectionCallback<T>> map) {
        this.binds.putAll(map);
        rerender();
    }

    private List<AttributedString> generateSequenceCollection(boolean isDumb,Set<T> set, T highLightRow) {
        LinkedList<AttributedString> charSequences = new LinkedList<>();
        int i=1;
        for (T o:set) {
            AttributedStringBuilder builder = new AttributedStringBuilder();
            if(o == highLightRow)
                builder.style(new AttributedStyle().background(0x94c765));
            if(isDumb)
                builder.append(String.valueOf(i)).append("-");
            else
                builder.append("•").append(" ");
            builder.append(o.toString());
            charSequences.add(builder.toAttributedString());
            i++;
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
        if(n>this.binds.size()-1) return false;
        else return n > 0;
    }
}
