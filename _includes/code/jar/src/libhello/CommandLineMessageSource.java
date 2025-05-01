package enterprisey;

import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CommandLineMessageSource implements MessageSource {
    private final String[] args;

    public CommandLineMessageSource(String[] args) {
        this.args = args;
    }

    public Iterable<Reader> messages() throws IOException {
        return new Iterable<Reader>() {
            public Iterator<Reader> iterator() {
                return new Iter();
            }
        };
    }

    private class Iter implements Iterator<Reader> {
        private int index = 0;

        public boolean hasNext() {
            return this.index < args.length;
        }

        public Reader next() {
            if (this.hasNext()) {
                String next = args[this.index];
                this.index++;
                return new StringReader(next);
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
