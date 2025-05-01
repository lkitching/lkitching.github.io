package enterprisey;

import java.io.Reader;
import java.io.IOException;

public interface MessageSource {
    Iterable<Reader> messages() throws IOException;
}
