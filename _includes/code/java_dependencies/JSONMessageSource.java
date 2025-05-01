package enterprisey;

import java.io.FileInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.json.JSONArray;
import org.json.JSONTokener;

public class JSONMessageSource implements MessageSource {
    private final JSONArray arr;

    public JSONMessageSource(JSONArray arr) {
        this.arr = arr;
    }

    public static JSONMessageSource fromFile(String fileName) throws IOException {
        try (FileInputStream is = new FileInputStream(fileName)) {
            JSONTokener tokeniser = new JSONTokener(is);
            JSONArray arr = new JSONArray(tokeniser);
            return new JSONMessageSource(arr);
        }
    }

    public Iterable<Reader> messages() throws IOException {
        return new Iterable<Reader>() {
            public Iterator<Reader> iterator() {
                return new Iter();
            }
        };
    }

    private class Iter implements Iterator<Reader> {
        private int index;
        public Iter() {
            this.index = 0;
        }

        public boolean hasNext() {
            return this.index < arr.length();
        }

        public Reader next() {
            if (this.index < arr.length()) {
                String s = arr.getString(this.index);
                this.index++;
                return new StringReader(s);
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
