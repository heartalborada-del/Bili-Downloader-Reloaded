package me.heartalborada.biliDownloader.Cli.Terminal;

import me.heartalborada.biliDownloader.Interfaces.ProcessProgress;
import me.heartalborada.biliDownloader.Utils.Utils;
import org.jline.reader.LineReader;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.PrintWriter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TerminalProcessProgress implements ProcessProgress {
    private boolean isFailed,isClosed;
    private final LineReader reader;
    private final PrintWriter writer;
    private long total, processed;
    private String extraString = "";
    private final Lock lock = new ReentrantLock();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final PropertyChangeListener listener = evt -> {
        if(lock.tryLock()) {
            try {
                //TODO 这是一个临时解决方案
                if (evt.getPropertyName().equals("NeedUpdate") && evt.getNewValue() instanceof Boolean && (Boolean) evt.getNewValue()) {
                    updateProgresses();
                }
            } finally {
                lock.unlock();
            }
        } else {
            //TODO 未获取到锁处理
            return;
        }
    };
    private final String ProgressStyle;

    /**
     *
     * @param reader JLine Reader
     * @param ProgressStyle These are allowed placeholder<br>
     *                      <code>{bar}</code> Progress bar<br>
     *                      <code>{percentage}</code> Percentage<br>
     *                      <code>{processed}</code> Processed Block<br>
     *                      <code>{total}</code> Total Block<br>
     *                      <code>{extra}</code> Extra Text
     */
    public TerminalProcessProgress(LineReader reader, String ProgressStyle){
        this.reader = reader;
        this.writer = reader.getTerminal().writer();
        this.ProgressStyle = ProgressStyle;
        pcs.addPropertyChangeListener(listener);
    }
    public  TerminalProcessProgress(LineReader reader) {
        this.reader = reader;
        this.writer = reader.getTerminal().writer();
        this.ProgressStyle = "{bar} | {percentage}% | {processed}/{total}";
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void updateText(String string) {
        extraString = string;
        pcs.firePropertyChange(new PropertyChangeEvent(this,"NeedUpdate",false,true));
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
        pcs.firePropertyChange(new PropertyChangeEvent(this,"NeedUpdate",false,true));
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
        pcs.removePropertyChangeListener(listener);
        this.isClosed = true;
    }

    private void updateProgresses() {
        if(isClosed || isFailed)
            return;
        String output = this.ProgressStyle
                .replace("{bar}", Utils.generateProgressBar(
                        '█','░',10,this.total,this.processed
                ))
                .replace("{percentage}",String.valueOf(this.processed/this.total))
                .replace("{processed}",String.valueOf(this.processed))
                .replace("{total}",String.valueOf(this.total))
                .replace("{extra}",extraString);
        writer.print("\r"+output);
    }

}
