package me.heartalborada.biliDownloader.Cli.Terminal;

import lombok.SneakyThrows;
import me.heartalborada.biliDownloader.Cli.Enums.KeyOperation;
import me.heartalborada.biliDownloader.Interfaces.Selection;
import me.heartalborada.biliDownloader.Interfaces.SelectionCallback;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.*;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jline.keymap.KeyMap.key;

public class TerminalSelection<T> implements Selection<T> {
    private final LinkedHashMap<T, SelectionCallback<T>> binds = new LinkedHashMap<>();
    private final Display display;
    private final int width;
    private final ExecutorService service = Executors.newSingleThreadExecutor();
    private final AtomicInteger selectedNum = new AtomicInteger(-1);
    private final LinkedList<Character> characters = new LinkedList<>();
    private final Terminal terminal;
    private final AttributedString headSuggestionString;
    private final Attributes attr;
    volatile private boolean isClosed = false, isBegin = false, isDone = false;
    private volatile AttributedString footSuggestionString;
    volatile private Thread runningThread;

    public TerminalSelection(Terminal terminal, Map<T, SelectionCallback<T>> binds, AttributedString headSuggestionString) {
        this.binds.putAll(binds);
        this.width = terminal.getWidth() == 0 ? 20 : terminal.getWidth();
        this.terminal = terminal;
        this.display = new Display(terminal, false);
        this.attr = terminal.enterRawMode();
        if (Objects.equals(headSuggestionString, null)) {
            AttributedStringBuilder builder = new AttributedStringBuilder()
                    .style(new AttributedStyle().background(AttributedStyle.YELLOW).foreground(AttributedStyle.BLACK))
                    .append("You can press Ctrl+C to cancel.");
            this.headSuggestionString = builder.toAttributedString();
        } else {
            this.headSuggestionString = headSuggestionString;
        }
    }

    @Override
    public void bind(T obj, SelectionCallback<T> cb) {
        this.binds.put(obj, cb);
    }

    @Override
    public void rerender() {
        List<AttributedString> c = new LinkedList<>();
        if (headSuggestionString != null) c.add(headSuggestionString);
        if (this.terminal instanceof DumbTerminal) {
            c.addAll(generateSequenceCollection(true, this.binds.keySet(), null));
            StringBuilder stringBuilder = new StringBuilder(this.characters.size());
            for (Character character : this.characters) stringBuilder.append(character);
            AttributedStringBuilder builder = new AttributedStringBuilder()
                    .style(new AttributedStyle().background(0xFFC0CB))
                    .append("Input Number: ")
                    .style(new AttributedStyle().background(0x708090).foreground(0x0))
                    .append(stringBuilder.toString());
            c.add(builder.toAttributedString());
        } else {
            c.addAll(generateSequenceCollection(false, binds.keySet(), getSetElement(binds.keySet(), selectedNum.get())));
        }
        if (this.footSuggestionString != null) c.add(this.footSuggestionString);
        this.display.resize(c.size(), width);
        this.display.update(c, terminal.getSize().cursorPos(c.size(), 0));
    }

    @SneakyThrows
    @Override
    public void start() {
        terminal.puts(InfoCmp.Capability.keypad_xmit, new Object());
        terminal.flush();
        terminal.handle(Terminal.Signal.INT, signal -> {
            if (runningThread != null) {
                runningThread.interrupt();
                runningThread = null;
            }
        });
        if (isBegin) return;
        this.isBegin = true;
        rerender();
        if (!(this.terminal instanceof DumbTerminal)) {
            this.service.submit(() -> {
                this.runningThread = Thread.currentThread();
                KeyMap<KeyOperation> keys = new KeyMap<>();
                BindingReaderM bindingReader = new BindingReaderM(terminal.reader());
                keys.bind(KeyOperation.UP, key(terminal, InfoCmp.Capability.key_up));
                keys.bind(KeyOperation.DOWN, key(terminal, InfoCmp.Capability.key_down));
                keys.bind(KeyOperation.ENTER, key(terminal, InfoCmp.Capability.key_enter));
                keys.bind(KeyOperation.ENTER, String.valueOf((char) 13));
                keys.bind(KeyOperation.CTRL_C, String.valueOf((char) 3));
                keys.setNomatch(KeyOperation.UNKNOWN);
                while (!this.isClosed) {
                    try {
                        KeyOperation key = bindingReader.readBinding(keys);
                        switch (key) {
                            case UP:
                                if (this.selectedNum.get() - 1 < 0) this.selectedNum.set(this.binds.size() - 1);
                                else this.selectedNum.decrementAndGet();
                                rerender();
                                break;
                            case DOWN:
                                if (this.selectedNum.get() + 1 >= this.binds.size()) this.selectedNum.set(0);
                                else this.selectedNum.incrementAndGet();
                                rerender();
                                break;
                            case CTRL_C:
                                this.footSuggestionString = new AttributedStringBuilder()
                                        .style(new AttributedStyle().background(AttributedStyle.RED).foreground(0x0))
                                        .append("Canceled")
                                        .toAttributedString();
                                rerender();
                                close();
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
                                rerender();
                                this.binds.get(Tobj).onSelected(Tobj);
                                this.isDone = true;
                                close();
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            this.service.submit(() -> {
                this.runningThread = Thread.currentThread();
                NonBlockingReader nonBlockingReader = this.terminal.reader();
                while (!this.isClosed) {
                    try {
                        int i = nonBlockingReader.read();
                        //Press Backspace
                        if (i == 8) {
                            this.characters.removeLast();
                            rerender();
                        }//Press Enter
                        else if (i == 13) {
                            StringBuilder stringBuilder = new StringBuilder(characters.size());
                            for (Character character : characters) stringBuilder.append(character);
                            int parsed = Integer.parseInt(stringBuilder.toString());
                            if (checkValid(parsed)) {
                                this.footSuggestionString = null;
                                T Tobj = null;
                                int i2 = 0;
                                for (T obj : this.binds.keySet()) {
                                    if (i2 == parsed - 1)
                                        Tobj = obj;
                                    i2++;
                                }
                                if (Tobj == null) break;
                                this.binds.get(Tobj).onSelected(Tobj);
                                rerender();
                                this.isDone = true;
                                close();
                            } else {
                                this.footSuggestionString = new AttributedStringBuilder()
                                        .style(new AttributedStyle().background(0XDC143C).foreground(0x0))
                                        .append("Invalid input")
                                        .toAttributedString();
                                this.characters.clear();
                                rerender();
                            }
                        }//Number Key
                        else if (i >= 48 && i <= 57) {
                            this.characters.add((char) i);
                            rerender();
                        }
                    } catch (InterruptedIOException ignore) {
                        this.footSuggestionString = new AttributedStringBuilder()
                                .style(new AttributedStyle().background(AttributedStyle.RED).foreground(0x0))
                                .append("Canceled")
                                .toAttributedString();
                        rerender();
                        close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Override
    public void binds(Map<T, SelectionCallback<T>> map) {
        this.binds.putAll(map);
        rerender();
    }

    private List<AttributedString> generateSequenceCollection(boolean isDumb, Set<T> set, T highLightRow) {
        LinkedList<AttributedString> charSequences = new LinkedList<>();
        int i = 1;
        for (T o : set) {
            AttributedStringBuilder builder = new AttributedStringBuilder();
            if (o == highLightRow)
                builder.style(new AttributedStyle().background(0x94c765));
            if (isDumb)
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
        int i = 0;
        for (T obj : set) {
            if (i == selected)
                return obj;
            i++;
        }
        return null;
    }

    private boolean checkValid(int n) {
        if (n > this.binds.size() - 1) return false;
        else return n > 0;
    }

    @Override
    public boolean cancel() {
        close();
        return service.isShutdown() && service.isTerminated();
    }

    @Override
    public boolean isCancelled() {
        return isClosed && !isDone;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public void close() {
        this.service.shutdownNow();
        this.isClosed = true;
        terminal.puts(InfoCmp.Capability.keypad_local, new Object());
        if(attr != null) terminal.setAttributes(attr);
        terminal.flush();
    }
}
