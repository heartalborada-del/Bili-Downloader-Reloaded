package me.heartalborada.biliDownloader.Cli.Terminal;

import me.heartalborada.biliDownloader.Interfaces.ProcessProgress;
import me.heartalborada.biliDownloader.Utils.Utils;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.Display;

import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TerminalProcessProgress implements ProcessProgress {
    private boolean isFailed,isClosed;
    private final Terminal terminal;
    private final Display display;
    private long total, processed;
    private String extraString = "";
    private final Lock lock = new ReentrantLock();
    private final String ProgressStyle;

    /**
     *
     * @param terminal Jline Terminal
     * @param ProgressStyle These are allowed placeholder<br>
     *                      <code>{bar}</code> Progress bar<br>
     *                      <code>{percentage}</code> Percentage<br>
     *                      <code>{processed}</code> Processed Block<br>
     *                      <code>{total}</code> Total Block<br>
     *                      <code>{extra}</code> Extra Text<br>
     *                      <code>{stat}</code> Progress Stat
     */
    public TerminalProcessProgress(Terminal terminal, String ProgressStyle){
        this.terminal = terminal;
        this.ProgressStyle = ProgressStyle;
        this.display = new Display(terminal,false);
        display.resize(1,terminal.getWidth() == 0 ? 20 : terminal.getWidth());
    }
    public  TerminalProcessProgress(Terminal terminal) {
        this.terminal = terminal;
        this.ProgressStyle = "{stat} | {bar} | {percentage}% | {processed}/{total}";
        this.display = new Display(terminal,false);
        display.resize(1,terminal.getWidth() == 0 ? 20 : terminal.getWidth());
    }

    @Override
    public void updateText(String string) {
        extraString = string;
        updateProgresses();
    }

    @Override
    public void updateText(CharSequence charSequence) {
        updateText(charSequence.toString());
    }

    @Override
    public void setTotalSize(long size) {
        this.total =size;
    }

    @Override
    public void update(long processed) {
        this.processed = processed;
        updateProgresses();
    }

    @Override
    public void update(long processed, long size) {
        this.total =size;
        update(processed);
    }

    @Override
    public void setFailed() {
        this.isFailed = true;
        this.close();
    }


    @Override
    public void rerender() {
        updateProgresses();
    }

    @Override
    public void close() {
        updateProgresses();
        this.isClosed = true;
    }

    private void updateProgresses() {
        if(lock.tryLock()){
            if(isClosed || isFailed)
                return;
            String output = this.ProgressStyle
                    .replace("{bar}", Utils.generateProgressBar(
                            '█','░',10,this.total,this.processed
                    ))
                    .replace("{stat}",getStat())
                    .replace("{percentage}",String.valueOf((this.processed*100/this.total)))
                    .replace("{processed}",String.valueOf(this.processed))
                    .replace("{total}",String.valueOf(this.total))
                    .replace("{extra}",extraString);
            display.update(
                    Collections.singletonList(new AttributedStringBuilder().append(output).toAttributedString())
                    , terminal.getSize().cursorPos(1,0));
            lock.unlock();
        }
    }

    private String getStat() {
        if(this.isFailed && this.isClosed) {
            return "[Failed]";
        } else if (this.isClosed && this.processed >= this.total) {
            return "[Done]";
        } else {
            return "[Processing]";
        }
    }
}
